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
package org.tigase.mobile.chat;

import java.util.Date;

import org.tigase.mobile.ClientIconsTool;
import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.Preferences;
import org.tigase.mobile.R;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.providers.ChatHistoryProvider;
import org.tigase.mobile.db.providers.DBChatManager;
import org.tigase.mobile.roster.CPresence;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.ChatState;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
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

	private static final boolean DEBUG = false;

	private static final String TAG = "tigase";

	private Chat chat;

	private ImageView clientTypeIndicator;

	protected boolean composing = false;

	private EditText ed;

	private ImageView itemPresence;

	private final SharedPreferences prefs;

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

	public Chat getChat() {
		return chat;
	}

	void init() {
		if (DEBUG)
			Log.i(TAG, "Zrobione");

		this.ed = (EditText) findViewById(R.id.chat_message_entry);
		this.ed.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				boolean ets = prefs.getBoolean(Preferences.ENTER_TO_SEND_KEY, true);
				// this is not always called - sometimes we need to use
				// TextWatcher
				if (ets && keyCode == KeyEvent.KEYCODE_ENTER) {
					sendMessage();
					return true;
				} else {
					updateComposing(true);
				}
				return false;
			}
		});
		this.ed.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				updateComposing(s.length() > 0);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub

			}

		});
		this.ed.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					updateComposing(false);
					cancelEdit();
				}
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

		final ListView lv = (ListView) findViewById(R.id.chat_conversation_history);
		lv.post(new Runnable() {

			@Override
			public void run() {
				lv.setSelection(Integer.MAX_VALUE);
			}
		});

		itemPresence = (ImageView) findViewById(R.id.user_presence);
		clientTypeIndicator = (ImageView) findViewById(R.id.client_type_indicator);
	}

	protected void sendMessage() {
		composing = false;

		if (ed == null)
			return;

		String t = ed.getText().toString();
		ed.setText("");

		if (t == null || t.length() == 0)
			return;
		if (DEBUG)
			Log.d(TAG, "Send: " + t);

		final JaxmppCore jaxmpp = ((MessengerApplication) getContext().getApplicationContext()).getMultiJaxmpp().get(
				chat.getSessionObject());
		chat.setMessageDeliveryReceiptsEnabled(DBChatManager.isReceiptAvailable(jaxmpp, chat.getJid()));

		AsyncTask<String, Void, Void> task = new AsyncTask<String, Void, Void>() {
			@Override
			public Void doInBackground(String... ts) {
				String t = ts[0];
				Log.d(TAG, "Send: " + t);
				int state;
				Message msg = null;
				try {
					msg = chat.sendMessage(t);
					state = ChatTableMetaData.STATE_OUT_SENT;
				} catch (Exception e) {
					state = ChatTableMetaData.STATE_OUT_NOT_SENT;
					Log.e(TAG, e.getMessage(), e);
				}
				// dbHelper.addChatHistory(1, chat, t);

				Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + chat.getJid().getBareJid().toString());

				ContentValues values = new ContentValues();
				values.put(ChatTableMetaData.FIELD_AUTHOR_JID, chat.getSessionObject().getUserBareJid().toString());
				values.put(ChatTableMetaData.FIELD_JID, chat.getJid().getBareJid().toString());
				values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
				values.put(ChatTableMetaData.FIELD_BODY, t);
				values.put(ChatTableMetaData.FIELD_THREAD_ID, chat.getThreadId());
				values.put(ChatTableMetaData.FIELD_ACCOUNT, chat.getSessionObject().getUserBareJid().toString());
				values.put(ChatTableMetaData.FIELD_STATE, state);

				int receiptStatus = 0;
				String msgId = null;
				try {
					if (msg != null) {
						receiptStatus = msg.getChildrenNS("request", MessageModule.RECEIPTS_XMLNS) != null ? 1 : 0;
						msgId = msg.getId();
					}
				} catch (Exception e) {
					Log.w(TAG, "Can't check Receipt Status", e);
					// do nothing
				}
				values.put(ChatTableMetaData.FIELD_RECEIPT_STATUS, receiptStatus);
				values.put(ChatTableMetaData.FIELD_MESSAGE_ID, msgId);

				getContext().getContentResolver().insert(uri, values);

				return null;
			}
		};
		task.execute(t);
	}

	public void setChat(Chat chat) {
		this.chat = chat;
		if (chat == null)
			return;

		// Not needed - handled in TigaseMobileMessengerActivity - set as
		// ActionBar subtitle
		TextView t = (TextView) findViewById(R.id.textView1);
		if (t != null) {
			JaxmppCore jaxmpp = ((MessengerApplication) getContext().getApplicationContext()).getMultiJaxmpp().get(
					chat.getSessionObject());

			if (jaxmpp == null)
				throw new RuntimeException("Account " + chat.getSessionObject().getUserBareJid() + " is unknown!");

			RosterItem ri = jaxmpp.getRoster().get(chat.getJid().getBareJid());
			t.setText("Chat with "
					+ (ri == null ? chat.getJid().getBareJid().toString() : RosterDisplayTools.getDisplayName(ri)));
		}
	}

	public void setImagePresence(final CPresence cp) {
		if (itemPresence == null)
			return;

		itemPresence.post(new Runnable() {

			@Override
			public void run() {
				if (cp == null)
					itemPresence.setImageResource(R.drawable.user_offline);
				else
					switch (cp) {
					case chat:
						itemPresence.setImageResource(R.drawable.user_free_for_chat);
						break;
					case online:
						itemPresence.setImageResource(R.drawable.user_available);
						break;
					case away:
						itemPresence.setImageResource(R.drawable.user_away);
						break;
					case xa:
						itemPresence.setImageResource(R.drawable.user_extended_away);
						break;
					case dnd:
						itemPresence.setImageResource(R.drawable.user_busy);
						break;
					case requested:
						itemPresence.setImageResource(R.drawable.user_ask);
						break;
					case error:
						itemPresence.setImageResource(R.drawable.user_error);
						break;
					case offline_nonauth:
						itemPresence.setImageResource(R.drawable.user_noauth);
						break;
					default:
						itemPresence.setImageResource(R.drawable.user_offline);
						break;
					}
			}
		});

	}

	public void updateClientIndicator() {
		if (clientTypeIndicator == null)
			return;

		clientTypeIndicator.setVisibility(View.INVISIBLE);
		if (chat != null) {
			try {
				final String nodeName = chat.getSessionObject().getUserProperty(CapabilitiesModule.NODE_NAME_KEY);
				JID jid = chat.getJid();
				final Presence p = chat.getSessionObject().getPresence().getPresence(jid);
				final CapabilitiesModule capabilitiesModule = ((MessengerApplication) (getContext().getApplicationContext())).getMultiJaxmpp().get(
						chat.getSessionObject()).getModule(CapabilitiesModule.class);

				final Integer pp = ClientIconsTool.getResourceImage(p, capabilitiesModule, nodeName);

				if (pp != null) {
					Runnable r = new Runnable() {

						@Override
						public void run() {
							clientTypeIndicator.setImageResource(pp);
							clientTypeIndicator.setVisibility(View.VISIBLE);
						}
					};

					clientTypeIndicator.post(r);

				}
			} catch (Exception e) {
			}
		}
	}

	private void updateComposing(boolean value) {
		Log.v(TAG, "updating composing from " + composing + " to " + value);
		if (composing != value) {
			composing = value;
			new Thread() {
				@Override
				public void run() {
					try {
						chat.setLocalChatState(composing ? ChatState.composing : ChatState.active);
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
	}
}
