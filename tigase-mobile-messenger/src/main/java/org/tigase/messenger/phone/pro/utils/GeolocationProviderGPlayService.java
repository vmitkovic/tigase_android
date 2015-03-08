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
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class GeolocationProviderGPlayService extends GeolocationProvider 
		implements GoogleApiClient.ConnectionCallbacks,
			GoogleApiClient.OnConnectionFailedListener {

	private static final String TAG = "GeolocationProviderGPlayService";
	
	private Context context;
	private Map<LocationListener,LocationRequest> listeners = new HashMap<LocationListener,LocationRequest>();
	private GoogleApiClient locationClient;
	private LocationListener currentLocationListener;
	
	protected GeolocationProviderGPlayService(Context context) {
		this.context = context;
	}
	
	@Override
	public void registerLocationListener(LocationRequest request,
			LocationListener listener) {
		Log.v(TAG, "adding location listener = " + listener);
		listeners.put(listener, request);
		if (locationClient != null && locationClient.isConnected()) {
			Log.v(TAG, "registering location listener = " + listener);
            LocationServices.FusedLocationApi.requestLocationUpdates(locationClient, request, listener);
		}
	}

	@Override
	public void unregisterLocationListener(LocationListener listener) {
		Log.v(TAG, "removing location listener = " + listener);
		listeners.remove(listener);
		if (locationClient.isConnected()) {
			Log.v(TAG, "unregistering location listener = " + listener);
            LocationServices.FusedLocationApi.removeLocationUpdates(locationClient, listener);
		}
	}

	@Override
	public void getCurrentLocation(LocationListener listener) {	
		if (locationClient.isConnected()) {
			Location location = LocationServices.FusedLocationApi.getLastLocation(locationClient);
			listener.onLocationChanged(location);
		} else {
			currentLocationListener = listener;
		}
	}



	@Override
	public void onStart() {
        locationClient = new GoogleApiClient.Builder(context).addApi(LocationServices.API).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
		locationClient.connect();
	}



	@Override
	public void onStop() {
		GoogleApiClient tmp = locationClient;
		locationClient = null;
		tmp.disconnect();
	}



	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.v(TAG, "Connection to geolocation provider failed, cause = " + result.getErrorCode());
	}



	@Override
	public void onConnected(Bundle connectionHint) {
		Log.v(TAG, "Connection to geolocation provider established, registering " + listeners.size() + " listeners");
		for (Map.Entry<LocationListener,LocationRequest> e : listeners.entrySet()) {
			Log.v(TAG, "registering location listener = " + e.getKey());
            LocationServices.FusedLocationApi.requestLocationUpdates(locationClient, e.getValue(), e.getKey());
		}
		if (currentLocationListener != null) {
			Location location = LocationServices.FusedLocationApi.getLastLocation(locationClient);
			currentLocationListener.onLocationChanged(location);
			currentLocationListener = null;
		}
	}

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "Connection to geolocation provider suspended = " + i);
    }


//	@Override
//	public void onDisconnected() {
//		Log.v(TAG, "Connection to geolocation provider broken");
//		if (locationClient != null) {
//			locationClient.connect();
//		}
//	}

}
