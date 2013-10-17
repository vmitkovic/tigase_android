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
package org.tigase.mobile.filetransfer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.tigase.mobile.ClientIconsTool;
import org.tigase.mobile.Features;
import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.ChatHistoryProvider;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.roster.FlatRosterAdapter;
import org.tigase.mobile.roster.GroupsRosterAdapter;
import org.tigase.mobile.ui.IconContextMenu;
import org.tigase.mobile.ui.IconContextMenu.IconContextItemSelectedListener;
import org.tigase.mobile.utils.AvatarHelper;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
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
	
	private class BuddyItem {
		private RosterItem rosterItem;
		private JID jid;
	}
	
	private ArrayAdapter<BuddyItem> recipientsAdapter;
	
	private Jaxmpp getJaxmpp(BareJID account) {
		return ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(account);
	}

	private RosterItem getRosterItem(long itemId) {
		final Cursor cursor = getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI + "/" + itemId), null, null,
				null, null);

		try {
			if (!cursor.moveToNext())
				return null;
			BareJID jid = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_JID)));
			BareJID account = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_ACCOUNT)));

			if (account != null && jid != null) {
				return getJaxmpp(account).getRoster().get(jid);
			}

		} finally {
			cursor.close();
		}
		return null;
	}

	@Override
	public void onCreate(Bundle bundle) {

		super.onCreate(bundle);

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
		
		final OnClickListener selectResourceListener = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? new OnClickListener() {

			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			@Override
			public void onClick(View v) {
				final BuddyItem bi = (BuddyItem) v.getTag();
				PopupMenu popupMenu = new PopupMenu(ShareActivity.this, v);
				final RosterItem r = bi.rosterItem;
				try {
					Presence p = r.getSessionObject().getPresence().getBestPresence(r.getJid());
					if (p != null && p.getType() == null) {
						prepareResources(popupMenu.getMenu(), r);
					}
				} catch (Exception e) {
				}

				IconContextMenu imenu = new IconContextMenu(ShareActivity.this, popupMenu.getMenu(), r.getJid().toString(),
						IconContextMenu.ICON_RIGHT);
				imenu.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() {

					@Override
					public void onIconContextItemSelected(MenuItem item, Object info) {
						String resource = item.getTitle().toString();
						bi.jid = JID.jidInstance(r.getJid(), resource);
						recipientsAdapter.notifyDataSetChanged();
						//finish();
					}

				});
				imenu.show();				
			}
			
		} : null;
		recipientsAdapter = new ArrayAdapter<BuddyItem>(this, R.layout.recipient_item) {
			
			@Override
			public long getItemId(int position) {
				BuddyItem bi = getItem(position);
				return bi.rosterItem.getId();
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
					name.setText(bi.rosterItem.getName());
					resource.setText(bi.jid == null || bi.jid.getResource() == null ? "" : bi.jid.getResource());
					AvatarHelper.setAvatarToImageView(bi.rosterItem.getJid(), avatar);
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
	            			new String[] { constraint.toString(),  Features.BYTESTREAMS, Features.FILE_TRANSFER }, null);	        
	            }
	        }
		});	
		recipientsTextView2.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> tv, View view, int pos, long id) {
				// TODO Auto-generated method stub
				RosterItem ri = getRosterItem(id);
				Log.v("ShareActivity", "adding recipient = " + ri.getName() + " -- " + ri.getJid().toString());		
				//recipientsAdapter.add(ri);
				int idx = 0;
				for (; idx<recipientsAdapter.getCount(); idx++) {
					if (ri.getName().compareTo(recipientsAdapter.getItem(idx).rosterItem.getName()) < 0)
						break;
				}
				BuddyItem bi = new BuddyItem();
				bi.rosterItem = ri;
				recipientsAdapter.insert(bi, idx);
				recipientsTextView2.setText(null);
				share.setEnabled(!recipientsAdapter.isEmpty());
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
				txt += textOrUri;
				
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
	
	private void sendMessage() {
		final List<Runnable> runnables = new ArrayList<Runnable>();
		final String body = message.getText().toString();
		for (int i=0; i<recipientsAdapter.getCount(); i++) {
			BuddyItem bi = recipientsAdapter.getItem(i);
			final JID jid = bi.jid == null ? JID.jidInstance(bi.rosterItem.getJid()) : bi.jid;
			final BareJID account = bi.rosterItem.getSessionObject().getUserBareJid();
			final Jaxmpp jaxmpp = getJaxmpp(account);
			final ContentResolver contentResolver = getContentResolver();
			runnables.add(new Runnable() {
				public void run() {
					try {
						jaxmpp.getModule(MessageModule.class).sendMessage(jid, null, body);		
						
						Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + jid.getBareJid().toString());

						ContentValues values = new ContentValues();
						values.put(ChatTableMetaData.FIELD_AUTHOR_JID, account.toString());
						values.put(ChatTableMetaData.FIELD_JID, jid.getBareJid().toString());
						values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
						values.put(ChatTableMetaData.FIELD_BODY, body);
						//values.put(ChatTableMetaData.FIELD_THREAD_ID, null);
						values.put(ChatTableMetaData.FIELD_ACCOUNT, account.toString());
						values.put(ChatTableMetaData.FIELD_STATE, ChatTableMetaData.STATE_OUT_SENT);

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
		final List<Runnable> runnables = new ArrayList<Runnable>();
		final String body = message.getText().toString();
		for (int i=0; i<recipientsAdapter.getCount(); i++) {
			BuddyItem bi = recipientsAdapter.getItem(i);
			final Jaxmpp jaxmpp = getJaxmpp(bi.rosterItem.getSessionObject().getUserBareJid());
			final JID jid = bi.jid == null ? FileTransferUtility.getBestJidForFeatures(jaxmpp, bi.rosterItem.getJid(), FileTransferUtility.FEATURES) : bi.jid;	
			
			FileTransferUtility.startFileTransfer(this, jaxmpp, jid, uri, mimetype);
		}
	}
	
	private void prepareResources(Menu menu, RosterItem ri) throws XMLException {
		final Jaxmpp jaxmpp = getJaxmpp(ri.getSessionObject().getUserBareJid());
		Map<String, Presence> all = jaxmpp.getSessionObject().getPresence().getPresences(ri.getJid());

		final CapabilitiesModule capabilitiesModule = jaxmpp.getModule(CapabilitiesModule.class);
		final String nodeName = jaxmpp.getSessionObject().getUserProperty(CapabilitiesModule.NODE_NAME_KEY);

		for (Entry<String, Presence> entry : all.entrySet()) {
			MenuItem mitem = menu.add(entry.getKey());
			int iconRes = ClientIconsTool.getResourceImage(entry.getValue(), capabilitiesModule, nodeName);
			boolean enabled = FileTransferUtility.resourceContainsFeatures(jaxmpp, entry.getValue().getFrom(),
					FileTransferUtility.FEATURES);
			mitem.setEnabled(enabled);
			mitem.setIcon(iconRes);
		}
	}

}