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
package org.tigase.messenger.phone.pro.chat;

import java.util.Date;

import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.JaxmppService;
import org.tigase.messenger.phone.pro.MainActivity;
//import org.tigase.messenger.phone.pro.ClientIconsTool;
import org.tigase.messenger.phone.pro.MessengerApplication;
import org.tigase.messenger.phone.pro.Preferences;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.ChatTableMetaData;
import org.tigase.messenger.phone.pro.db.providers.ChatHistoryProvider;
import org.tigase.messenger.phone.pro.db.providers.OpenChatsProvider;
import org.tigase.messenger.phone.pro.roster.CPresence;

import tigase.jaxmpp.android.chat.OpenChatTableMetaData;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ChatView extends RelativeLayout {

	private ServiceConnection jaxmppServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			ChatView.this.jaxmppService = IJaxmppService.Stub.asInterface(service);
			try {
				jaxmppService.openChat("andrzej.wojcik@tigase.im","andrzej@hi-low.eu");
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			jaxmppService = null;
		}
		
	};
	
	private static final boolean DEBUG = false;

	private static final String TAG = "tigase";

//	private Chat chat;
//
	private ImageView clientTypeIndicator;
	private ListView lv;
	private EditText ed;

	private ImageView itemPresence;

	private final SharedPreferences prefs;
	
	private String account;
	private JID recipient;
	private String threadId;
//	private long chatId;
	private Uri uri;

	private IJaxmppService jaxmppService;
	private ContentObserver observer = new ContentObserver(null) {
		public void onChange(boolean self) {
			lv.post(new Runnable() {
				@Override
				public void run() {
					lv.smoothScrollToPosition(Integer.MAX_VALUE);//.setSelection(Integer.MAX_VALUE);
				}
			});			
		}
	};
	
	public ChatView(Context context) {
		super(context);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
	}

	public ChatView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
	}

	void cancelEdit() {
		if (ed == null)
			return;
		final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

		ed.post(new Runnable() {

			@Override
			public void run() {
				ed.clearComposingText();
				imm.hideSoftInputFromWindow(ed.getWindowToken(), 0);
			}
		});

	}

//	public Chat getChat() {
//		return chat;
//	}

	void init() {
		if (DEBUG)
			Log.i(TAG, "Zrobione");

		this.ed = (EditText) findViewById(R.id.chat_message_entry);
		this.ed.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				boolean ets = true;//prefs.getBoolean(Preferences.ENTER_TO_SEND_KEY, true);
				if (ets && keyCode == KeyEvent.KEYCODE_ENTER) {
					sendMessage();
					return true;
				}
				return false;
			}
		});
		this.ed.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus)
					cancelEdit();
			}
		});

		final Button b = (Button) findViewById(R.id.chat_send_button);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (DEBUG)
					Log.i(TAG, "Klikniete");

				sendMessage();

			}
		});

		lv = (ListView) findViewById(R.id.chat_conversation_history);
		lv.post(new Runnable() {

			@Override
			public void run() {
				lv.setSelection(Integer.MAX_VALUE);
			}
		});

		itemPresence = (ImageView) findViewById(R.id.user_presence);
		clientTypeIndicator = (ImageView) findViewById(R.id.client_type_indicator);
		
		Intent intent = new Intent(getContext(), JaxmppService.class);
		intent.putExtra("ID", "AIDL");
		this.getContext().bindService(intent, jaxmppServiceConnection, Context.BIND_AUTO_CREATE);		
	}
	
	void destroy() {
		this.getContext().getContentResolver().unregisterContentObserver(observer);
		this.getContext().unbindService(jaxmppServiceConnection);
	}

	protected void sendMessage() {
		if (ed == null)
			return;

		String t = ed.getText().toString();
		ed.setText("");

		if (t == null || t.length() == 0)
			return;
		if (DEBUG)
			Log.d(TAG, "Send: " + t);

		AsyncTask<String, Void, Void> task = new AsyncTask<String, Void, Void>() {
			@Override
			public Void doInBackground(String... ts) {
				String t = ts[0];
				Log.d(TAG, "Send: " + t);
				int state = ChatTableMetaData.STATE_OUT_NOT_SENT;
				try {
					//chat.sendMessage(t);
				boolean sent = jaxmppService.sendMessage(account, recipient.toString(), threadId, t);
				if (sent) {
					state = ChatTableMetaData.STATE_OUT_SENT;
				//} catch (Exception e) {
				}
				} catch (RemoteException ex) {
					
				}
				//	Log.e(TAG, e.getMessage(), e);
				//}
				// dbHelper.addChatHistory(1, chat, t);

				//Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + recipient.getBareJid().toString());

				ContentValues values = new ContentValues();
				values.put(ChatTableMetaData.FIELD_AUTHOR_JID, account);
				values.put(ChatTableMetaData.FIELD_JID, recipient.getBareJid().toString());
				values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
				values.put(ChatTableMetaData.FIELD_BODY, t);
				values.put(ChatTableMetaData.FIELD_THREAD_ID, threadId);
				values.put(ChatTableMetaData.FIELD_ACCOUNT, account);
				values.put(ChatTableMetaData.FIELD_STATE, state);

				getContext().getContentResolver().insert(uri, values);
				
				return null;
			}
		};
		task.execute(t);
	}

	public void setChat(String account, JID recipient, String threadId) {
//		this.chat = chat;
//		if (chat == null)
//			return;
		
		this.account = account;
		this.recipient = recipient;
		this.threadId = threadId;
		
		if (this.uri != null) {
			getContext().getContentResolver().unregisterContentObserver(observer);
		}
		uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + recipient.getBareJid().toString());
		this.getContext().getContentResolver().registerContentObserver(uri, true, observer);
		// maybe we should retrieve here recipients name as well? or maybe we should move it to method calling this method
		// or maybe it is not needed at all
//		Cursor c = getContext().getContentResolver().query(Uri.parse(OpenChatsProvider.OPEN_CHATS_URI), 
//				new String[] { OpenChatTableMetaData.FIELD_ID }, OpenChatTableMetaData.FIELD_ACCOUNT + " = ? AND " + OpenChatTableMetaData.FIELD_JID + "= ?", 
//				new String[] { account, recipient.getBareJid().toString() }, null);
//		try {
//			if (c.moveToNext()) {
//				this.chatId = c.getLong(0);
//			}
//		} finally {
//			c.close();
//		}
		
		// Not needed - handled in TigaseMobileMessengerActivity - set as
		// ActionBar subtitle
		TextView t = (TextView) findViewById(R.id.textView1);
		if (t != null) {
//			JaxmppCore jaxmpp = ((MessengerApplication) getContext().getApplicationContext()).getMultiJaxmpp().get(
//					chat.getSessionObject());
//
//			if (jaxmpp == null)
//				throw new RuntimeException("Account " + chat.getSessionObject().getUserBareJid() + " is unknown!");
//
//			RosterItem ri = jaxmpp.getRoster().get(chat.getJid().getBareJid());
//			t.setText("Chat with "
//					+ (ri == null ? chat.getJid().getBareJid().toString() : RosterDisplayTools.getDisplayName(ri)));
			t.setText("Chat with " + recipient.getBareJid().toString());
		}
	}

//	public void setImagePresence(final CPresence cp) {
//		if (itemPresence == null)
//			return;
//
//		itemPresence.post(new Runnable() {
//
//			@Override
//			public void run() {
//				if (cp == null)
//					itemPresence.setImageResource(R.drawable.user_offline);
//				else 
//					switch (cp) {
//					case chat:
//						itemPresence.setImageResource(R.drawable.user_free_for_chat);
//						break;
//					case online:
//						itemPresence.setImageResource(R.drawable.user_available);
//						break;
//					case away:
//						itemPresence.setImageResource(R.drawable.user_away);
//						break;
//					case xa:
//						itemPresence.setImageResource(R.drawable.user_extended_away);
//						break;
//					case dnd:
//						itemPresence.setImageResource(R.drawable.user_busy);
//						break;
//					case requested:
//						itemPresence.setImageResource(R.drawable.user_ask);
//						break;
//					case error:
//						itemPresence.setImageResource(R.drawable.user_error);
//						break;
//					case offline_nonauth:
//						itemPresence.setImageResource(R.drawable.user_noauth);
//						break;
//					default:
//						itemPresence.setImageResource(R.drawable.user_offline);
//						break;
//					}
//			}
//		});
//
//	}

	public void updateClientIndicator() {
		if (clientTypeIndicator == null)
			return;

//		clientTypeIndicator.setVisibility(View.INVISIBLE);
//		if (chat != null) {
//			try {
//				final String nodeName = chat.getSessionObject().getUserProperty(CapabilitiesModule.NODE_NAME_KEY);
//				JID jid = chat.getJid();
//				final Presence p = chat.getSessionObject().getPresence().getPresence(jid);
//				final CapabilitiesModule capabilitiesModule = ((MessengerApplication) (getContext().getApplicationContext())).getMultiJaxmpp().get(
//						chat.getSessionObject()).getModule(CapabilitiesModule.class);
//
//				final Integer pp = ClientIconsTool.getResourceImage(p, capabilitiesModule, nodeName);
//
//				if (pp != null) {
//					Runnable r = new Runnable() {
//
//						@Override
//						public void run() {
//							clientTypeIndicator.setImageResource(pp);
//							clientTypeIndicator.setVisibility(View.VISIBLE);
//						}
//					};
//
//					clientTypeIndicator.post(r);
//
//				}
//			} catch (Exception e) {
//			}
//		}
	}

}
