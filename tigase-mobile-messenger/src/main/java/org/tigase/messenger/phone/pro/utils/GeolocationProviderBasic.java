/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2014 "Tigase, Inc." <office@tigase.com>
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
package org.tigase.messenger.phone.pro.utils;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class GeolocationProviderBasic extends GeolocationProvider {

	private Context context;
	private Map<LocationListener,android.location.LocationListener> listenersMap = new HashMap<LocationListener,android.location.LocationListener>();
	
	protected GeolocationProviderBasic(Context context) {
		this.context = context;
	}
	
	@Override
	public void registerLocationListener(final LocationRequest request,
			final LocationListener listener) {
		if (listenersMap.containsKey(listener))
			return;
		final android.location.LocationListener locationListener = new android.location.LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				// TODO Auto-generated method stub
				listener.onLocationChanged(location);
			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			@Override
			public void onProviderEnabled(String provider) {
			}

			@Override
			public void onProviderDisabled(String provider) {
			}
			
		};
		listenersMap.put(listener, locationListener);
		
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(request.getInterval(), request.getSmallestDisplacement(), new Criteria(), locationListener, null);
	}

	@Override
	public void unregisterLocationListener(LocationListener listener) {
		final android.location.LocationListener locationListener = listenersMap.remove(listener);
		if (locationListener == null)
			return;

		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		locationManager.removeUpdates(locationListener);
	}

	@Override
	public Location getCurrentLocation() {
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	}

	@Override
	public void onStart() {
	}

	@Override
	public void onStop() {
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

}
