/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package org.tigase.messenger.phone.pro.service;

import java.io.IOException;
import java.util.List;

import org.tigase.messenger.phone.pro.db.DatabaseHelper;
import org.tigase.messenger.phone.pro.pubsub.GeolocationModule;
import org.tigase.messenger.phone.pro.utils.GeolocationProvider;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

public class GeolocationFeature {

	private static final String TAG = "GeolocationFeature";
	
	public static final String GEOLOCATION_QUEUED = "geolocation#location_queued";
	
	public static final String GEOLOCATION_LISTEN_ENABLED = "geolocation#listen_enabled";

	public static final String GEOLOCATION_PUBLISH_ENABLED = "geolocation#publish_enabled";

	public static final String GEOLOCATION_PUBLISH_PRECISION = "geolocation#publish_precision";

	private void sendCurrentLocation(JaxmppCore jaxmpp, Context context) {
		Location location = locationProvider.getCurrentLocation();
		try {
			GeolocationFeature.updateLocation(jaxmpp, location, context);
		} catch (JaxmppException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method used to update geolocation settings on connected or disconnected
	 * instance of JaxmppCore with proper handling of this situation
	 * 
	 * @param account
	 * @param jaxmpp
	 * @param context
	 */
	public boolean updateGeolocationSettings(final Account account, final Jaxmpp jaxmpp, final Context context, DatabaseHelper dbHelper) {
		boolean featuresChanged = false;

		AccountManager accountManager = AccountManager.get(context);

		// update of geolocation listen setting
		Boolean listenOld = jaxmpp.getSessionObject().getProperty(GEOLOCATION_LISTEN_ENABLED);
		if (listenOld == null)
			listenOld = false;
		String valueStr = accountManager.getUserData(account, GEOLOCATION_LISTEN_ENABLED);
		boolean listenNew = (valueStr != null && Boolean.parseBoolean(valueStr));

		jaxmpp.getSessionObject().setUserProperty(GEOLOCATION_LISTEN_ENABLED, listenNew);
		if (listenNew != listenOld) {
			featuresChanged = true;
			if (listenNew) {
				GeolocationModule geolocationModule = new GeolocationModule(dbHelper);
				jaxmpp.getModulesManager().register(geolocationModule);
				geolocationModule.init(jaxmpp);
			} else {
				GeolocationModule module = jaxmpp.getModule(GeolocationModule.class);
				module.deinit(jaxmpp);
				jaxmpp.getModulesManager().unregister(module);
			}
		}

		// update of geolocation publish setting
		Boolean publishOld = jaxmpp.getSessionObject().getProperty(GEOLOCATION_PUBLISH_ENABLED);
		if (publishOld == null) {
			publishOld = false;
		}
		valueStr = accountManager.getUserData(account, GEOLOCATION_PUBLISH_ENABLED);
		boolean publishNew = (valueStr != null && Boolean.parseBoolean(valueStr));

		// update of geolocation publish precision
		Integer precisionOld = jaxmpp.getSessionObject().getProperty(GEOLOCATION_PUBLISH_PRECISION);
		if (precisionOld == null)
			precisionOld = 0;
		valueStr = accountManager.getUserData(account, GEOLOCATION_PUBLISH_PRECISION);
		int precisionNew = (valueStr != null) ? Integer.parseInt(valueStr) : 0;

		jaxmpp.getSessionObject().setUserProperty(GEOLOCATION_PUBLISH_PRECISION, precisionNew);
				
				
		if (jaxmpp.isConnected() && publishNew != publishOld) {
			if (publishNew) {
				jaxmpp.getSessionObject().setUserProperty(GEOLOCATION_PUBLISH_ENABLED, publishNew);
				accountConnected(jaxmpp);
			} else {
				accountDisconnect(jaxmpp);
				accountDisconnected(jaxmpp);
			}
		}
		jaxmpp.getSessionObject().setUserProperty(GEOLOCATION_PUBLISH_ENABLED, publishNew);

		if (jaxmpp.isConnected() && (precisionOld != precisionNew && publishOld == publishNew)) {
			sendCurrentLocation(jaxmpp, context);
		}

		return featuresChanged;
	}
	
	private static void updateLocation(final JaxmppCore jaxmpp, final Location location, final Context context) throws JaxmppException {
		if (!jaxmpp.isConnected())
			return;
		Boolean enabled = (Boolean) jaxmpp.getSessionObject().getProperty(GEOLOCATION_PUBLISH_ENABLED);
		Log.v(TAG, "updating location for " + jaxmpp.getSessionObject().getUserBareJid().toString() + " with location " + location + " enabled=" + enabled);
		if (enabled == null || !enabled.booleanValue())
			return;		
		
		new Thread() {
			@Override
			public void run() {
				try {
					List<Address> addresses = null;
					if (location != null) {
						try {
							Geocoder geocoder = new Geocoder(context);
							addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
						} catch (IOException ex) {
							ex.printStackTrace();			
						}
					}
					updateLocation(jaxmpp, location, addresses);
				}
				catch (JaxmppException ex) {
					Log.e(TAG, "Exception publishing updated geolocation", ex);
				}
			}
		}.start();
	}
	
	private static void updateLocation(final JaxmppCore jaxmpp, Location location, List<Address> addresses) throws JaxmppException {

		int precision = (Integer) jaxmpp.getSessionObject().getProperty(GEOLOCATION_PUBLISH_PRECISION);

		final IQ iq = IQ.create();
		iq.setType(StanzaType.set);
		iq.setId("pubsub1");
		Element pubsub = ElementFactory.create("pubsub");
		pubsub.setXMLNS("http://jabber.org/protocol/pubsub");
		iq.addChild(pubsub);
		Element publish = ElementFactory.create("publish");
		publish.setAttribute("node", "http://jabber.org/protocol/geoloc");
		pubsub.addChild(publish);
		Element item = ElementFactory.create("item");
		publish.addChild(item);
		Element geoloc = ElementFactory.create("geoloc");
		geoloc.setXMLNS("http://jabber.org/protocol/geoloc");
		item.addChild(geoloc);
		if (location != null) {
			if (precision > 2) {
				Element lat = ElementFactory.create("lat");
				lat.setValue(String.valueOf(location.getLatitude()));
				geoloc.addChild(lat);
				Element lon = ElementFactory.create("lon");
				lon.setValue(String.valueOf(location.getLongitude()));
				geoloc.addChild(lon);
				Element alt = ElementFactory.create("alt");
				alt.setValue(String.valueOf(location.getAltitude()));
				geoloc.addChild(alt);
			}

			if (addresses != null && !addresses.isEmpty()) {
				Address address = addresses.get(0);
				// precision == 0
				if (address.getCountryName() != null) {
					Element country = ElementFactory.create("country");
					country.setValue(address.getCountryName());
					geoloc.addChild(country);
				}
				// precision == 1
				if (precision >= 1) {
					if (address.getLocality() != null) {
						Element locality = ElementFactory.create("locality");
						locality.setValue(address.getLocality());
						geoloc.addChild(locality);
					}
					// precision == 1
					if (address.getPostalCode() != null) {
						Element postalcode = ElementFactory.create("postalcode");
						postalcode.setValue(address.getPostalCode());
						geoloc.addChild(postalcode);
					}
				}
				// precision == 2
				if (precision >= 2) {
					if (address.getThoroughfare() != null) {
						Element street = ElementFactory.create("street");
						street.setValue(address.getThoroughfare());
						geoloc.addChild(street);
					}
				}
			} else if (precision < 3) {
				// nothing to send - exiting
				Log.v(TAG, "precision " + precision + "< 3 - exiting 1");
				return;
			}
		} else if (precision < 3) {
			// nothing to send - exiting
			Log.v(TAG, "precision " + precision + " < 3 - exiting 2");
			return;
		}

		Log.v(TAG, "publishing = " + iq.getAsString());
		jaxmpp.send(iq);
	}

	private final JaxmppService jaxmppService;

	private long locationInterval = 1 * 60 * 1000;

	private int count = 0;
	
	private LocationListener locationListener = null;

	private GeolocationProvider locationProvider = null;
	
	public GeolocationFeature(JaxmppService service) {
		jaxmppService = service;
	}
	
	public void onStart() {
		if (locationProvider == null) {
			locationProvider = GeolocationProvider.createInstance(jaxmppService);
			locationProvider.onStart();
		}
	}
	
	public void onStop() {
		if (locationProvider != null) {
			locationProvider.onStop();
			locationProvider = null;
		}
	}
	
	public void accountConnected(final Jaxmpp jaxmpp) {
		Boolean publish = jaxmpp.getSessionObject().getProperty(GEOLOCATION_PUBLISH_ENABLED);
		if (publish == null) {
			publish = false;
		}
		if (publish) {
			synchronized (this) {
				count++;
				if (count > 0) {
					registerLocationListener();
				}
			}
			sendCurrentLocation(jaxmpp, jaxmppService);
		}
	}
	
	public void accountDisconnect(Jaxmpp jaxmpp) {
		Boolean publish = jaxmpp.getSessionObject().getProperty(GEOLOCATION_PUBLISH_ENABLED);
		if (publish == null) {
			publish = false;
		}
		if (publish && jaxmpp.isConnected()) {
			try {
				updateLocation(jaxmpp, (Location) null, (List<Address>) null);
			}
			catch (JaxmppException ex) {
				Log.v(TAG, "Exception reseting geolocation before disconnection");
			}
		}
	}
	
	public void accountDisconnected(Jaxmpp jaxmpp) {
		Boolean publish = jaxmpp.getSessionObject().getProperty(GEOLOCATION_PUBLISH_ENABLED);
		if (publish == null) {
			publish = false;
		}
		if (publish) {
			synchronized (this) {
				count--;
				if (count <= 0) {
					count = 0;
					unregisterLocationListener();
				}
			}
		}
	}
	
	private void registerLocationListener() {
		if (locationListener != null) {
			return;
		}
		
		locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				queueLocationForUpdate(location);
			}		
		};
		
		execInLooper(new Runnable() {
			public void run() {
				LocationRequest request = LocationRequest.create();
				request.setInterval(locationInterval);
				request.setSmallestDisplacement(100);
				request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
				request.setFastestInterval(60*1000);
				locationProvider.registerLocationListener(request, locationListener);
			}
		});		
	}
	
	public static void execInLooper(final Runnable runnable) {
		new Thread() {
			@Override
			public void run() {
				Looper.prepare();
				Looper tmp = Looper.myLooper();
				runnable.run();
				Looper.loop();
				tmp.quit();
			}
		}.start();
	}
	
	public void sendCurrentLocation() {
		Location location = locationProvider.getCurrentLocation();
		for (JaxmppCore jaxmpp : jaxmppService.getMulti().get()) {
			try {
				updateLocation(jaxmpp, location, jaxmppService);
			} catch (JaxmppException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void unregisterLocationListener() {
		if (locationListener == null)
			return;
		final LocationListener tmp = locationListener;
		locationListener = null;
		execInLooper(new Runnable() {
			public void run() {		
				locationProvider.unregisterLocationListener(tmp);
			}
		});
	}

	public void queueLocationForUpdate(Location location) {
		for (JaxmppCore jaxmpp : jaxmppService.getMulti().get()) {
			try {
				Boolean publish = jaxmpp.getSessionObject().getProperty(GEOLOCATION_PUBLISH_ENABLED);
				if (publish == null || !publish)
					continue;
				Log.v(TAG, "queuing location for update for account " + jaxmpp.getSessionObject().getUserBareJid().toString());
				jaxmpp.getSessionObject().setProperty(GEOLOCATION_QUEUED, location);
				updateLocation(jaxmpp, location, jaxmppService);
			} catch (JaxmppException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void sendQueuedGeolocation(JaxmppCore jaxmpp, JaxmppService jaxmppService) {
		Location location = jaxmpp.getSessionObject().getProperty(GEOLOCATION_QUEUED);
		if (location != null) {
			jaxmpp.getSessionObject().setProperty(GEOLOCATION_QUEUED, null);
			try {
				updateLocation(jaxmpp, location, jaxmppService);
			} catch (JaxmppException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}
