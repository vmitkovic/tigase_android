/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package org.tigase.messenger.phone.pro.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.ChatTableMetaData;
import org.tigase.messenger.phone.pro.db.providers.ChatHistoryProvider;
import org.tigase.messenger.phone.pro.service.GeolocationFeature;
import org.tigase.messenger.phone.pro.utils.CameraHelper;
import org.tigase.messenger.phone.pro.utils.GeolocationProvider;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import tigase.jaxmpp.android.xml.ParcelableElement;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.utils.MutableBoolean;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class ShareDialog {
	
	private static final String TAG = "ShareDialog";
	
	private static final int[] ACTION_NAMES = {
		R.string.share_takePhoto,
		R.string.share_pickPhoto,
		R.string.share_takeVideo,
		R.string.share_pickVideo,
		R.string.share_location
	};
	private static final int[] ACTION_ICONS = {
		android.R.drawable.ic_menu_camera,
		android.R.drawable.ic_menu_gallery,
		android.R.drawable.ic_menu_camera,
		android.R.drawable.ic_menu_slideshow,
		android.R.drawable.ic_menu_mylocation
	};
	
	public static int IMAGE = 1;
	public static int VIDEO = 2;
	public static int LOCALITY = 4;
	
	private final MainActivity context;
	private final Fragment fragment;
	private final int actionForResult;
	private final String account;
	private final JID recipient;
	private final String thread;
	private int which = -1;
	private Dialog dialog;
	private boolean finished = true;
	private Uri toShare = null;
	
	public static ShareDialog newInstance(MainActivity context, Fragment fragment, int actionForResult, String account, JID recipient, String thread) {
		final ShareDialog instance = new ShareDialog(context, fragment, actionForResult, account, recipient, thread);
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		List<HashMap<String,String>> fillMaps = new ArrayList<HashMap<String,String>>();
		for (int i=0; i<ACTION_NAMES.length; i++) {
			HashMap<String,String> vals = new HashMap<String,String>();
			vals.put("icon", Integer.toString(ACTION_ICONS[i]));
			vals.put("text", context.getResources().getString(ACTION_NAMES[i]));
			fillMaps.add(vals);
		}
		String[] from = { "icon", "text" };
		int[] to = { R.id.icon, R.id.text }; 
		SimpleAdapter adapter = new SimpleAdapter(context, fillMaps, R.layout.share_list_item, from, to);
		builder.setAdapter(adapter,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						instance.which = which;
						switch (which) {
						case 0:
							instance.takePhoto();
							break;
						case 1:
							instance.pickPhoto();
							break;
						case 2:
							instance.takeVideo();
							break;
						case 3:
							instance.pickVideo();
							break;
						case 4:
							instance.location();
							break;
						}
					}
				});
		 instance.dialog = builder.create();
		 return instance;
	}
	
	private ShareDialog(MainActivity context, Fragment fragment, int actionForResult, String account, JID recipient, String thread) {
		this.context = context;
		this.fragment = fragment;
		this.actionForResult = actionForResult;
		this.account = account;
		this.recipient = recipient;
		this.thread = thread;
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	public void onActivityResult(Intent data) {
		Uri selected = which > 0 ? data.getData() : toShare;
		String mimetype = data == null ? null : data.getType();
		try {
			IJaxmppService jaxmppService = context.getJaxmppService();
			jaxmppService.sendFile(account, recipient.toString(), selected.toString(), mimetype);
		} catch (RemoteException ex) {
			ex.printStackTrace();
		}
	}
	
	public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
		dialog.setOnDismissListener(listener);
	}
	
	public void show() {
		dialog.show();
	}
	
	private void takePhoto() {
		File file = CameraHelper.takePhoto(context, fragment, actionForResult);
		toShare = Uri.fromFile(file);
		finished = false;
	}
	
	private void pickPhoto() {
		Intent pickerIntent = new Intent(Intent.ACTION_PICK);
		pickerIntent.setType("image/*");
		pickerIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		if (fragment == null) {
			context.startActivityForResult(pickerIntent, actionForResult);		
		} else {
			fragment.startActivityForResult(pickerIntent, actionForResult);
		}
		finished = false;
	}
	
	private void takeVideo() {
		CameraHelper.takeVideo(context, fragment, actionForResult);
		finished = false;
	}
	
	private void pickVideo() {
		Intent pickerIntent = new Intent(Intent.ACTION_PICK);
		pickerIntent.setType("video/*");
		pickerIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		if (fragment == null) {
			context.startActivityForResult(pickerIntent, actionForResult);		
		} else {
			fragment.startActivityForResult(pickerIntent, actionForResult);
		}
		finished = false;
	}
	
	private void location() {
		Toast.makeText(context, "Acquiring location..", 1).show();
		new AsyncTask<Object,String,String>() {		
			protected String doInBackground(Object... objects) {
				try {
					final GeolocationProvider geoProvider = GeolocationProvider.createInstance(context);
					final MutableBoolean called = new MutableBoolean();
					final LocationListener geoListener = new LocationListener() {
						@Override
						public void onLocationChanged(final Location location) {
							if (called.isValue())
								return;
							called.setValue(true);
							Log.v(TAG, "location 1 = " + location);
							geoProvider.unregisterLocationListener(this);
							geoProvider.onStop();
							if (location == null) {
								publishProgress("Unable to acquire location");
								return;
							}
							publishProgress("Location acquired");
							new Thread() {
								public void run() {
									sendGeolocation(location);
								}
							}.start();
						}
					};
					// Log.v(TAG, "currLocation = " + currLocation);
					LocationRequest request = LocationRequest
							.create()
							.setNumUpdates(1)
							.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
							.setFastestInterval(1).setInterval(1)
							.setSmallestDisplacement(1)
							.setExpirationDuration(10 * 1000);
					Looper.prepare();
					Looper tmp = Looper.myLooper();
					geoProvider.registerLocationListener(request, geoListener);
					Looper.loop();
					tmp.quit();
					geoProvider.onStart();					
					Thread.sleep(10 * 1000);
					if (!called.isValue()) {
						geoProvider.getCurrentLocation(geoListener);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}
			
		    protected void onProgressUpdate(String... progress) {
		    	Toast.makeText(context, progress[0], 1).show();
		    }

		    protected void onPostExecute(String result) {
		        //showDialog("Downloaded " + result + " bytes");
		    }			
		}.execute(new Object());		
	}
	
	private void sendGeolocation(Location location) {
		Geocoder geocoder = new Geocoder(context);
		try {
			Log.v(TAG, "resolving location " + location + " to address..");
			List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
			if (addresses == null || addresses.isEmpty()) {
				Log.v(TAG, "no address found for location = " + location);
				return;
			}
			Address address = addresses.get(0);
			Log.v(TAG, "resolved to address = " + address);
			ParcelableElement geoEl = ParcelableElement.fromElement(
				GeolocationFeature.toElement(location, address, null, Integer.MAX_VALUE));
			Log.v(TAG, "prepared geolocation addon " + geoEl.getAsString());
			IJaxmppService jaxmppService = context.getJaxmppService();
			List<ParcelableElement> elems = new ArrayList<ParcelableElement>();
			if (geoEl != null) {
				elems.add(geoEl);
			}
			String message = "";
			if (address.getFeatureName() != null) {
				message += address.getFeatureName();
			}
			if (address.getMaxAddressLineIndex() > -1) {
				for (int i=0; i<address.getMaxAddressLineIndex(); i++) {
					if (message.length() > 0) {
						message += "\n";
					}
					message += address.getAddressLine(i);
				}
			}
			if (address.getUrl() != null) {
				if(message.length() > 0) {
					message += "\n";
				}
				message += address.getUrl();
			}
			else {
				if(message.length() > 0) {
					message += "\n";
				}
				message += "http://maps.google.com/maps?q="+location.getLatitude()+","+location.getLongitude()+"&z=14";
			}
			jaxmppService.sendMessageExt(account.toString(), recipient.toString(), thread, message, elems);

			Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" 
					+ recipient.getBareJid().toString());
			ContentValues values = new ContentValues();
			values.put(ChatTableMetaData.FIELD_AUTHOR_JID, account.toString());
			values.put(ChatTableMetaData.FIELD_JID, recipient.getBareJid().toString());
			values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
			values.put(ChatTableMetaData.FIELD_BODY, message);
			if (thread != null) {
				values.put(ChatTableMetaData.FIELD_THREAD_ID, thread);
			}
			values.put(ChatTableMetaData.FIELD_ACCOUNT, account.toString());
			values.put(ChatTableMetaData.FIELD_STATE, ChatTableMetaData.STATE_OUT_SENT);
			values.put(ChatTableMetaData.FIELD_ITEM_TYPE, ChatTableMetaData.ITEM_TYPE_LOCALITY);
			values.put(ChatTableMetaData.FIELD_DATA, geoEl.getAsString());
			context.getContentResolver().insert(uri, values);				
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
