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
package org.tigase.messenger.phone.pro.share;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.ChatTableMetaData;
import org.tigase.messenger.phone.pro.db.providers.ChatHistoryProvider;
import org.tigase.messenger.phone.pro.db.providers.RosterProvider;
import org.tigase.messenger.phone.pro.db.providers.RosterProviderExt;
import org.tigase.messenger.phone.pro.roster.FlatRosterAdapter;
import org.tigase.messenger.phone.pro.service.GeolocationFeature;
import org.tigase.messenger.phone.pro.service.JaxmppService;

import tigase.jaxmpp.android.roster.RosterItemsCacheTableMetaData;
import tigase.jaxmpp.android.xml.ParcelableElement;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xmpp.modules.filetransfer.FileTransferModule;
import tigase.jaxmpp.core.client.xmpp.modules.socks5.Socks5BytestreamsModule;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.PopupMenu;
import android.widget.TextView;

public class ShareActivity extends Activity {

	private static final String TAG = "ShareActivity";

	private EditText message;
	private ParcelableElement locality;
	
	private ServiceConnection jaxmppServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			ShareActivity.this.jaxmppService = IJaxmppService.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			jaxmppService = null;
		}
		
	};	
	
	private class BuddyItem {
		//private RosterItem rosterItem;
		private BareJID account;
		private String name;
		private JID jid;
	}
	
	private ArrayAdapter<BuddyItem> recipientsAdapter;
	
//	private RosterItem getRosterItem(long itemId) {
//		final Cursor cursor = getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI + "/" + itemId), null, null,
//				null, null);
//
//		try {
//			if (!cursor.moveToNext())
//				return null;
//			BareJID jid = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterItemsCacheTableMetaData.FIELD_JID)));
//			BareJID account = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterItemsCacheTableMetaData.FIELD_ACCOUNT)));
//
//			if (account != null && jid != null) {
//				return RosterModule.getRosterStore(getJaxmpp(account).getSessionObject()).get(jid);
//			}
//
//		} finally {
//			cursor.close();
//		}
//		return null;
//	}

	@Override
	public void onCreate(Bundle bundle) {

		super.onCreate(bundle);

		Intent serviceintent = new Intent(this, JaxmppService.class);
		serviceintent.putExtra("ID", "AIDL");
		bindService(serviceintent, jaxmppServiceConnection, Context.BIND_AUTO_CREATE);
		
		setContentView(R.layout.send_file);

		final Button share = (Button) findViewById(R.id.share);
		final Button cancel = (Button) findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				finish();
			}
			
		});
		
		final OnClickListener deleteRecipient = new OnClickListener() {

			@Override
			public void onClick(View v) {
				BuddyItem bi = (BuddyItem) v.getTag();
				if (bi != null) 
					recipientsAdapter.remove(bi);
				share.setEnabled(!recipientsAdapter.isEmpty());
			}
			
		};
		
		final OnClickListener selectResourceListener = null;
//		final OnClickListener selectResourceListener = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? new OnClickListener() {
//
//			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
//			@Override
//			public void onClick(View v) {
//				final BuddyItem bi = (BuddyItem) v.getTag();
//				PopupMenu popupMenu = new PopupMenu(ShareActivity.this, v);
//				final RosterItem r = bi.rosterItem;
//				try {
//					Presence p = r.getSessionObject().getPresence().getBestPresence(r.getJid());
//					if (p != null && p.getType() == null) {
//						prepareResources(popupMenu.getMenu(), r);
//					}
//				} catch (Exception e) {
//				}
//
//				IconContextMenu imenu = new IconContextMenu(ShareActivity.this, popupMenu.getMenu(), r.getJid().toString(),
//						IconContextMenu.ICON_RIGHT);
//				imenu.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() {
//
//					@Override
//					public void onIconContextItemSelected(MenuItem item, Object info) {
//						String resource = item.getTitle().toString();
//						bi.jid = JID.jidInstance(r.getJid(), resource);
//						recipientsAdapter.notifyDataSetChanged();
//						//finish();
//					}
//
//				});
//				imenu.show();				
//			}
//			
//		} : null;
		recipientsAdapter = new ArrayAdapter<BuddyItem>(this, R.layout.recipient_item) {
			
			@Override
			public long getItemId(int position) {
				BuddyItem bi = getItem(position);
				return RosterProviderExt.createId(bi.account, bi.jid.getBareJid());
			}
			
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = convertView;//convertView;//super.getView(position, convertView, parent);
				
				if (v == null) {
					LayoutInflater inflater = (LayoutInflater) ShareActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					v = inflater.inflate(R.layout.recipient_item, parent, false);
				}
				
				TextView name = (TextView) v.findViewById(R.id.roster_item_jid);
				TextView resource = (TextView) v.findViewById(R.id.roster_item_resource);
				ImageView avatar = (ImageView) v.findViewById(R.id.imageView1);
				ImageButton delete = (ImageButton) v.findViewById(R.id.delete);
				ImageButton selectResource = (ImageButton) v.findViewById(R.id.selectResource);
				
				BuddyItem bi = getItem(position);
				if (bi != null) {
					name.setText(bi.name);
					resource.setText(bi.jid == null || bi.jid.getResource() == null ? "" : bi.jid.getResource());
					org.tigase.messenger.phone.pro.utils.AvatarHelper.setAvatarToImageView(bi.jid.getBareJid(), avatar);
					delete.setTag(bi);
					delete.setOnClickListener(deleteRecipient);
					if (selectResourceListener != null) {
						selectResource.setTag(bi);
						selectResource.setOnClickListener(selectResourceListener);
					}
					else {
						selectResource.setVisibility(View.GONE);
					}
				}
				
				return v;
			}
			
		};
		share.setEnabled(!recipientsAdapter.isEmpty());
		
		// Get intent, action and MIME type
		final Intent intent = getIntent();
		String action = intent.getAction();
		final String mimetype = intent.getType();
		
		final ListView recipientsList = (ListView) findViewById(R.id.recipientsList);
		recipientsList.setAdapter(recipientsAdapter);		
		final AutoCompleteTextView recipientsTextView2 = (AutoCompleteTextView) findViewById(R.id.recipients2);
		Cursor c = getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI), null, null, null, null);
		recipientsTextView2.setAdapter(new FlatRosterAdapter(this, c, R.layout.roster_item) {
	        @Override
	        public String convertToString(Cursor cursor) {
	            return cursor.getString(2) + " <" + cursor.getString(1) +">";
	        }		
	        

	        @Override
	        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
	            if (getFilterQueryProvider() != null) {
	                return getFilterQueryProvider().runQuery(constraint);
	            }
	            if ("text/plain".equals(mimetype)) {
	            	return getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI), null, "search,offline", 
	            			new String[] { constraint.toString() }, null);	            	
	            }
	            else {
	            	return getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI), null, "search,feature", 
	            			new String[] { constraint.toString(),  Socks5BytestreamsModule.XMLNS_BS, FileTransferModule.XMLNS_SI_FILE }, null);	        
	            }
	        }
		});	
		recipientsTextView2.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> tv, View view, int pos, long id) {
				// TODO Auto-generated method stub
				Uri uri = Uri.parse(RosterProvider.CONTENT_URI + "/" + id);
				Cursor c = getContentResolver().query(uri, null, null, null, null);
				if (c.moveToNext()) {
					BuddyItem bi = new BuddyItem();
					bi.account = BareJID.bareJIDInstance(c.getString(c.getColumnIndex(RosterItemsCacheTableMetaData.FIELD_ACCOUNT)));
					String jidStr = c.getString(c.getColumnIndex(RosterItemsCacheTableMetaData.FIELD_JID));
					bi.jid = JID.jidInstance(jidStr);
					bi.name = c.getString(c.getColumnIndex(RosterItemsCacheTableMetaData.FIELD_NAME));

					Log.v("ShareActivity", "adding recipient = " + bi.name + " -- " + bi.jid.toString());		
					int idx = 0;
					for (; idx<recipientsAdapter.getCount(); idx++) {
						if (bi.name.compareTo(recipientsAdapter.getItem(idx).name) < 0)
							break;
					}
					
					recipientsAdapter.insert(bi, idx);
					recipientsTextView2.setText(null);
					share.setEnabled(!recipientsAdapter.isEmpty());
				}
			}
			
		});

		View emptyView = findViewById(R.id.noRecipients);
		recipientsList.setEmptyView(emptyView);

		String textOrUri = intent.getStringExtra("android.intent.extra.TEXT");
		String subject = intent.getStringExtra("android.intent.extra.SUBJECT");		
		
		if (Intent.ACTION_SEND.equals(action) && mimetype != null) {
			final Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
			if (uri != null) {
				Log.v(TAG, "received input uri = " + uri.toString() + " for path = " + uri.getLastPathSegment());
			}
			
			message = (EditText) findViewById(R.id.message);
			
			if ("text/plain".equals(mimetype)) {
				findViewById(R.id.filename_layout).setVisibility(View.GONE);
				
				String txt = "";
				if (subject != null) {
					txt += subject + "\n";
				}
				if (textOrUri != null && textOrUri.length() > 0) {
					if (textOrUri.contains(subject)) {
						txt = textOrUri;
					} else {
						txt += textOrUri;
					}
				}
				if (txt != null) {
					int start = txt.indexOf("http://goo.gl/maps");
					if (start > -1) {
						int end = txt.indexOf(' ' , start);
						if (end == -1) {
							end = txt.length();
						}
						final String urlStr = txt.substring(start, end);
						final String textStr = txt.substring(0, start-1);
						new Thread() {
							public void run() {
								
						try {
							URL url = new URL(urlStr);
							HttpURLConnection conn = (HttpURLConnection) url.openConnection();
							conn.setFollowRedirects(false);
							Uri gmapsUri = Uri.parse(conn.getHeaderField("Location"));
							String location = gmapsUri.getQueryParameter("q");
							String cid = gmapsUri.getQueryParameter("cid");
							Log.v(TAG, "got intent with " + urlStr + " - redirect to " + location);
							if (location != null || cid != null) {
								List<Address> addresses = null;
								Location tmp_location = null;
								if (location != null) {
									String[] lparts = location.split(",");
								try {
									tmp_location = new Location("internal");
									tmp_location.setLatitude(Double.parseDouble(lparts[0]));
									tmp_location.setLongitude(Double.parseDouble(lparts[1]));
									//------ find a way to pass this to jaxmpp to send as a part of a message in chat
									Geocoder geocoder = new Geocoder(ShareActivity.this);
									addresses = geocoder.getFromLocation(tmp_location.getLatitude(), tmp_location.getLongitude(), 1);
								} catch (NumberFormatException ex) {
									ex.printStackTrace();
									tmp_location = null;
								}
								}
								if (addresses == null) {
									Geocoder geocoder = new Geocoder(ShareActivity.this);
									Log.v(TAG, "geocoding " + location + " textStr = " + textStr);
									addresses = geocoder.getFromLocationName(location == null ? textStr : location, 1);
									if (addresses != null && !addresses.isEmpty()) {
										Address address = addresses.get(0);
										if (address.hasLatitude() || address.hasLongitude())
											tmp_location = new Location("internal");
										if (address.hasLatitude())
											tmp_location.setLatitude(address.getLatitude());
										if (address.hasLongitude())
											tmp_location.setLongitude(address.getLongitude());
									}									
								}
								Element geoEl = GeolocationFeature.toElement(tmp_location, addresses != null && !addresses.isEmpty() ? addresses.get(0) : null, urlStr, Integer.MAX_VALUE);
								locality = ParcelableElement.fromElement(geoEl);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
							}
						}.start();	
					}
				}
				
				message.setText(txt);
			}
			else {
				message.setVisibility(View.GONE);
				((TextView) findViewById(R.id.filename)).setText(FileTransferUtility.resolveFilename(this, uri, mimetype));
				//findViewById(R.id.filename_layout).setVisibility(View.GONE);
			}
			
			share.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if ("text/plain".equals(mimetype)) {
						sendMessage();
					}
					else {
						sendFile(uri, mimetype);
					}
					finish();
				}
				
			});
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService(jaxmppServiceConnection);		
	}
	
	private IJaxmppService jaxmppService = null;
	
	private void sendMessage() {
		final List<Runnable> runnables = new ArrayList<Runnable>();
		final String body = message.getText().toString();
		for (int i=0; i<recipientsAdapter.getCount(); i++) {
			BuddyItem bi = recipientsAdapter.getItem(i);
			final JID jid = bi.jid;
			final BareJID account = bi.account;
			final ContentResolver contentResolver = getContentResolver();
			runnables.add(new Runnable() {
				public void run() {
					try {
						// let's ensure that we have open chat
						jaxmppService.openChat(account.toString(), jid.toString());
						List<ParcelableElement> elems = new ArrayList<ParcelableElement>();
						if (locality != null) {
							elems.add(locality);
						}
						if (!jaxmppService.sendMessageExt(account.toString(), jid.toString(), null, body, elems)) {
							// we were not able to send message - should not happen
							return;
						}
						
						// append sent message to chat history
						Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + jid.getBareJid().toString());

						ContentValues values = new ContentValues();
						values.put(ChatTableMetaData.FIELD_AUTHOR_JID, account.toString());
						values.put(ChatTableMetaData.FIELD_JID, jid.getBareJid().toString());
						values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
						values.put(ChatTableMetaData.FIELD_BODY, body);
						//values.put(ChatTableMetaData.FIELD_THREAD_ID, null);
						values.put(ChatTableMetaData.FIELD_ACCOUNT, account.toString());
						values.put(ChatTableMetaData.FIELD_STATE, ChatTableMetaData.STATE_OUT_SENT);
						if (locality != null) {
							values.put(ChatTableMetaData.FIELD_ITEM_TYPE, ChatTableMetaData.ITEM_TYPE_LOCALITY);
							values.put(ChatTableMetaData.FIELD_DATA, locality.getAsString());
						}
						contentResolver.insert(uri, values);						
					}
					catch(Exception ex) {
						Log.v(TAG, "error sending message", ex);
					}
				}
			});			
		}
		new Thread() {
			public void run() {
				for (Runnable r : runnables) {
					r.run();
				}
			}
		}.start();
	}
	
	private void sendFile(final Uri uri, String mimetype) {
//		final List<Runnable> runnables = new ArrayList<Runnable>();
//		final String body = message.getText().toString();
		for (int i=0; i<recipientsAdapter.getCount(); i++) {
			BuddyItem bi = recipientsAdapter.getItem(i);
			
			try {
				jaxmppService.sendFile(bi.account.toString(), bi.jid.toString(), uri.toString(), mimetype);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//FileTransferUtility.startFileTransfer(this, jaxmpp, jid, uri, mimetype);
		}
	}
	
//	private void prepareResources(Menu menu, RosterItem ri) throws XMLException {
//		final Jaxmpp jaxmpp = getJaxmpp(ri.getSessionObject().getUserBareJid());
//		Map<String, Presence> all = jaxmpp.getSessionObject().getPresence().getPresences(ri.getJid());
//
//		final CapabilitiesModule capabilitiesModule = jaxmpp.getModule(CapabilitiesModule.class);
//		final String nodeName = jaxmpp.getSessionObject().getUserProperty(CapabilitiesModule.NODE_NAME_KEY);
//
//		for (Entry<String, Presence> entry : all.entrySet()) {
//			MenuItem mitem = menu.add(entry.getKey());
//			int iconRes = ClientIconsTool.getResourceImage(entry.getValue(), capabilitiesModule, nodeName);
//			boolean enabled = FileTransferUtility.resourceContainsFeatures(jaxmpp, entry.getValue().getFrom(),
//					FileTransferUtility.FEATURES);
//			mitem.setEnabled(enabled);
//			mitem.setIcon(iconRes);
//		}
//	}

}