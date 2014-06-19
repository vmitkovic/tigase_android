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

import org.tigase.messenger.phone.pro.Preferences;
import org.tigase.messenger.phone.pro.db.DatabaseHelper;
import org.tigase.messenger.phone.pro.pubsub.GeolocationModule;

import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.DefaultElement;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

public class GeolocationFeature {

	private static final String TAG = "GeolocationFeature";
	
	public static final String GEOLOCATION_QUEUED = "geolocation#location_queued";
	
	public static final String GEOLOCATION_LISTEN_ENABLED = "geolocation#listen_enabled";

	public static final String GEOLOCATION_PUBLISH_ENABLED = "geolocation#publish_enabled";

	public static final String GEOLOCATION_PUBLISH_PRECISION = "geolocation#publish_precision";

	public static void sendCurrentLocation(JaxmppCore jaxmpp, Context context) {
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
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
	public static boolean updateGeolocationSettings(final Account account, final JaxmppCore jaxmpp, final Context context, DatabaseHelper dbHelper) {
		boolean featuresChanged = false;
//		new Thread() {
//			@Override
//			public void run() {
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
//					if (jaxmpp.isConnected()) {
//						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//						PresenceModule presenceModule = jaxmpp.getModule(PresenceModule.class);
//						try {
//							if (JaxmppService.focused) {
//								int pr = prefs.getInt(Preferences.DEFAULT_PRIORITY_KEY, 5);
//								presenceModule.setPresence(JaxmppService.userStatusShow, JaxmppService.userStatusMessage, pr);
//							} else {
//								int pr = prefs.getInt(Preferences.AWAY_PRIORITY_KEY, 0);
//								presenceModule.setPresence(Show.away, "Auto away", pr);
//							}
//						} catch (JaxmppException ex) {
//							ex.printStackTrace();
//						}
//					}
				}

				// update of geolocation publish setting
				Boolean publishOld = jaxmpp.getSessionObject().getProperty(GEOLOCATION_PUBLISH_ENABLED);
				if (publishOld == null) {
					publishOld = false;
				}
				valueStr = accountManager.getUserData(account, GEOLOCATION_PUBLISH_ENABLED);
				boolean publishNew = (valueStr != null && Boolean.parseBoolean(valueStr));

				if (jaxmpp.isConnected() && publishNew != publishOld) {
					if (publishNew) {
						jaxmpp.getSessionObject().setUserProperty(GEOLOCATION_PUBLISH_ENABLED, publishNew);
						sendCurrentLocation(jaxmpp, context);
					} else {
						try {
								
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						jaxmpp.getSessionObject().setUserProperty(GEOLOCATION_PUBLISH_ENABLED, publishNew);
					}
				}
				jaxmpp.getSessionObject().setUserProperty(GEOLOCATION_PUBLISH_ENABLED, publishNew);

				// update of geolocation publish precision
				Integer precisionOld = jaxmpp.getSessionObject().getProperty(GEOLOCATION_PUBLISH_PRECISION);
				if (precisionOld == null)
					precisionOld = 0;
				valueStr = accountManager.getUserData(account, GEOLOCATION_PUBLISH_PRECISION);
				int precisionNew = (valueStr != null) ? Integer.parseInt(valueStr) : 0;

				jaxmpp.getSessionObject().setUserProperty(GEOLOCATION_PUBLISH_PRECISION, precisionNew);
				if (jaxmpp.isConnected() && precisionOld != precisionNew) {
					sendCurrentLocation(jaxmpp, context);
				}
//			}
//		}.start();
		return featuresChanged;
	}
	
	public static void updateLocation(final JaxmppCore jaxmpp, final Location location, final Context context) throws JaxmppException {
		Log.v(TAG, "update location1");
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

		new Thread() {
			@Override
			public void run() {
				try {
					Log.v(TAG, "publishing = " + iq.getAsString());
					jaxmpp.send(iq);
				} catch (XMLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JaxmppException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
	}

	private final JaxmppService jaxmppService;

	private Criteria locationCriteria = new Criteria();

	private long locationInterval = 5 * 60 * 1000;

	private LocationListener locationListener = null;

	private static Location lastLocation = null;
	
	public GeolocationFeature(JaxmppService service) {
		jaxmppService = service;
	}

	public void registerLocationListener() {
		if (locationListener != null) {
			return;
		}
		locationListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				Log.v(TAG, "received location");
				queueLocationForUpdate(location);
			}

			@Override
			public void onProviderDisabled(String provider) {
			}

			@Override
			public void onProviderEnabled(String provider) {
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

		};
		
		execInLooper(new Runnable() {
			public void run() {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
					registerLocationListenerGingerbread();
				} else {
					LocationManager locationManager = (LocationManager) jaxmppService.getSystemService(Context.LOCATION_SERVICE);
					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, locationInterval,
							100, locationListener);
				}
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

	@TargetApi(9)
	public void registerLocationListenerGingerbread() {
		LocationManager locationManager = (LocationManager) jaxmppService.getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(locationInterval, 100, locationCriteria, locationListener, null);
	}

	public void sendCurrentLocation() {
		LocationManager locationManager = (LocationManager) jaxmppService.getSystemService(Context.LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		for (JaxmppCore jaxmpp : jaxmppService.getMulti().get()) {
			try {
				updateLocation(jaxmpp, location, jaxmppService);
			} catch (JaxmppException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void unregisterLocationListener() {
		if (locationListener == null)
			return;
		final LocationListener tmp = locationListener;
		locationListener = null;
		execInLooper(new Runnable() {
			public void run() {		
				LocationManager locationManager = (LocationManager) jaxmppService.getSystemService(Context.LOCATION_SERVICE);
				locationManager.removeUpdates(tmp);		
			}
		});
	}

	public void queueLocationForUpdate(Location location) {
		for (JaxmppCore jaxmpp : jaxmppService.getMulti().get()) {
			try {
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
