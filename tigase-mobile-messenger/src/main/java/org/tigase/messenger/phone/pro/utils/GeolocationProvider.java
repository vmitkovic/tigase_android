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

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public abstract class GeolocationProvider {

	private static final String TAG = "GeolocationProvider";
	
	public static GeolocationProvider createInstance(Context context) {
		int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
		if (result == ConnectionResult.SUCCESS) {
			// success - Google Play Services SDK is available
			//return null;
			Log.v(TAG, "Using Google Play Services as location provider");
			return new GeolocationProviderGPlayService(context);
		}
		else {
			String cause = "unknown";
			switch (result) {
				case ConnectionResult.SERVICE_MISSING:
					cause = "service unavailable";
					break;
				case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
					cause = "service update required";
					break;
				case ConnectionResult.SERVICE_DISABLED:
					cause = "service disabled";
					break;
				default:
					cause = "unknown";
					break;
			}
			Log.v(TAG, "Using Android internal location manager as location provider, cause = " + cause);
			return new GeolocationProviderBasic(context);
		}
	}
	
	public abstract void onStart();
	public abstract void onStop();
	
	public abstract void registerLocationListener(LocationRequest request, LocationListener listener);

	public abstract void unregisterLocationListener(LocationListener listener);

	public abstract void getCurrentLocation(LocationListener listener);
}
