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
import java.util.List;

import org.tigase.messenger.phone.pro.CustomHeader;
import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.MessengerApplication;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.MainActivity;
//import org.tigase.messenger.phone.pro.TigaseMobileMessengerActivityHelper;
//import org.tigase.messenger.phone.pro.chat.ChatListActivity;
import org.tigase.messenger.phone.pro.db.ChatTableMetaData;
import org.tigase.messenger.phone.pro.db.providers.ChatHistoryProvider;
import org.tigase.messenger.phone.pro.db.providers.OpenChatsProvider;
import org.tigase.messenger.phone.pro.db.providers.RosterProvider;
//import org.tigase.messenger.phone.pro.filetransfer.FileTransferUtility;
//import org.tigase.messenger.phone.pro.muc.MucActivity;
import org.tigase.messenger.phone.pro.roster.CPresence;
//import org.tigase.messenger.phone.pro.roster.ContactActivity;

import org.tigase.messenger.phone.pro.roster.ContactFragment;
import org.tigase.messenger.phone.pro.roster.RosterAdapterHelper;
import org.tigase.messenger.phone.pro.service.JaxmppService;

import tigase.jaxmpp.android.chat.OpenChatTableMetaData;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.modules.chat.xep0085.ChatState;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SlidingPaneLayout;
import android.text.ClipboardManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChatHistoryFragment extends Fragment implements LoaderCallbacks<Cursor>, CustomHeader {

	private class Holder {
		TextView description;
		ImageView status;
		TextView title;
	}

	private static final boolean DEBUG = true;

	private static final String TAG = "tigase-chat";

	public static final String FRAG_TAG = "ChatHistoryFragment";

	public static Fragment newInstance(String account, JID recipient) {
		ChatHistoryFragment f = new ChatHistoryFragment();

		Bundle args = new Bundle();
		args.putString("recipient", recipient.toString());
		args.putString("account", account);
		f.setArguments(args);

		if (DEBUG)
			Log.d(TAG, "Creating ChatFragment recipient=" + recipient);

		return f;
	}

	// private Cursor c;

	private ChatAdapter chatAdapter;

	// private TigaseMobileMessengerActivityHelper helper;

	private ChatView layout;

	private ListView lv;

	private ContentObserver observer = null;

	private String account;
	private JID recipient;
	private String threadId;
	private String name;

	// private final Listener<PresenceEvent> presenceListener;

	public static void openChat(FragmentActivity activity, String account, String recipient, boolean xlarge) {
		Intent detailIntent = new Intent(activity, ChatActivity.class);
		detailIntent.putExtra("recipient", recipient);
		detailIntent.putExtra("account", account);
		activity.startActivity(detailIntent);
	}

	public ChatHistoryFragment() {
		super();
		// this.presenceListener = new Listener<PresenceModule.PresenceEvent>()
		// {
		//
		// @Override
		// public void handleEvent(PresenceEvent be) throws JaxmppException {
		// if (DEBUG)
		// Log.d(TAG, "Received presence " + be.getJid() + " :: " +
		// be.getPresence());
		// if (ChatHistoryFragment.this.chat != null
		// &&
		// ChatHistoryFragment.this.chat.getJid().getBareJid().equals(be.getJid().getBareJid()))
		// updatePresence();
		// }
		// };
		// this.chatUpdateListener = new Listener<MessageModule.MessageEvent>()
		// {
		//
		// @Override
		// public void handleEvent(MessageEvent be) throws JaxmppException {
		// layout.updateClientIndicator();
		// }
		// };
	}

	private void clearMessageHistory() {
		getActivity().getApplicationContext().getContentResolver().delete(
				Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + Uri.encode(recipient.getBareJid().toString())), null, null);
	}

	private void copyMessageBody(final long id) {
		ClipboardManager clipMan = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
		Cursor cc = null;
		try {
			cc = getChatEntry(id);
			String t = cc.getString(cc.getColumnIndex(ChatTableMetaData.FIELD_BODY));
			clipMan.setText(t);
		} finally {
			if (cc != null && !cc.isClosed())
				cc.close();
		}

	}

	// public Chat getChat() {
	// return chat;
	// }

	private Cursor getChatEntry(long id) {
		Cursor cursor = getActivity().getApplicationContext().getContentResolver().query(
				Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + Uri.encode(recipient.getBareJid().toString()) + "/" + id), null,
				null, null, null);
		cursor.moveToNext();
		return cursor;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getArguments() != null) {
			if (getArguments().containsKey("chatId")) {
				long chatId = getArguments().getLong("chatId");
				Cursor c = this.getActivity().getContentResolver().query(
						Uri.parse(OpenChatsProvider.OPEN_CHATS_URI),
						new String[] { OpenChatTableMetaData.FIELD_ACCOUNT, OpenChatTableMetaData.FIELD_JID,
								OpenChatTableMetaData.FIELD_THREAD_ID, OpenChatsProvider.FIELD_NAME },
						"open_chats." + OpenChatTableMetaData.FIELD_ID + "= ?", new String[] { String.valueOf(chatId) }, null);
				try {
					Log.v(TAG, "found " + c.getCount() + " for chatId = " + chatId);
					if (c.moveToNext()) {
						this.account = c.getString(0);
						this.recipient = JID.jidInstance(c.getString(1));
						this.threadId = c.getString(2);
						this.name = c.getString(3);
					}
				} finally {
					c.close();
				}
			} else if (getArguments().containsKey("recipient") || getArguments().containsKey("jid")) {
				String recipient = getArguments().containsKey("recipient") ? getArguments().getString("recipient")
						: getArguments().getString("jid");
				this.recipient = JID.jidInstance(recipient);
				this.account = getArguments().getString("account");
				// fix thread id
				// maybe we should retrieve here recipients name as well? or
				// maybe we should move it to method calling this method
				// or maybe it is not needed at all
				Cursor c = this.getActivity().getContentResolver().query(
						Uri.parse(OpenChatsProvider.OPEN_CHATS_URI),
						new String[] { OpenChatTableMetaData.FIELD_THREAD_ID, OpenChatsProvider.FIELD_NAME },
						"open_chats." + OpenChatTableMetaData.FIELD_ACCOUNT + " = ? AND open_chats."
								+ OpenChatTableMetaData.FIELD_JID + "= ?",
						new String[] { account, this.recipient.getBareJid().toString() }, null);
				try {
					if (c.moveToNext()) {
						this.threadId = c.getString(0);
						this.name = c.getString(1);
					}
				} finally {
					c.close();
				}
			} else {
				Log.e(TAG, "something gone really bad - no proper arguments found!");
			}
		}

		if (account != null && recipient != null) {
			layout.setChat(account, recipient, threadId);
			chatAdapter.setRecipientName(name);
			getLoaderManager().initLoader(this.recipient.hashCode(), null, this);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == MainActivity.SELECT_FOR_SHARE && resultCode == Activity.RESULT_OK) {
			Uri selected = data.getData();
			String mimetype = data.getType();
			try {
				IJaxmppService jaxmppService = ((MainActivity) getActivity()).getJaxmppService();
				jaxmppService.sendFile(account, recipient.toString(), selected.toString(), mimetype);
			} catch (RemoteException ex) {
				ex.printStackTrace();
			}
		}
	}

	// @Override
	// public boolean onContextItemSelected(MenuItem item) {
	// if (item.getItemId() == R.id.detailsMessage) {
	// AdapterContextMenuInfo info = (AdapterContextMenuInfo)
	// item.getMenuInfo();
	// showMessageDetails(info.id);
	// return true;
	// } else if (item.getItemId() == R.id.copyMessage) {
	// AdapterContextMenuInfo info = (AdapterContextMenuInfo)
	// item.getMenuInfo();
	// copyMessageBody(info.id);
	// return true;
	// } else if (item.getItemId() == R.id.clearMessageHistory) {
	// clearMessageHistory();
	// return true;
	// } else {
	// return super.onContextItemSelected(item);
	// }
	// }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		this.setHasOptionsMenu(true);
		super.onCreate(savedInstanceState);
		this.setHasOptionsMenu(true);
		this.setRetainInstance(true);

		
		
		this.chatAdapter = new ChatAdapter(getActivity(), R.layout.chat_item_his);
		chatAdapter.registerDataSetObserver(new DataSetObserver() {

			@Override
			public void onChanged() {
				super.onChanged();
				if (DEBUG)
					Log.i(TAG, "Changed!");
				lv.post(new Runnable() {

					@Override
					public void run() {
						lv.setSelection(Integer.MAX_VALUE);
					}
				});
			}
		});

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// MenuInflater m = new MenuInflater(getActivity());
		// m.inflate(R.menu.chat_context_menu, menu);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(getActivity().getApplicationContext(), Uri.parse(ChatHistoryProvider.CHAT_URI + "/"
				+ Uri.encode(recipient.getBareJid().toString())), null, null, null, null);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// menu.clear();

		inflater.inflate(R.menu.chat_main_menu, menu);

		MenuItemCompat.setShowAsAction(menu.findItem(R.id.closeChatButton), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		MenuItem shareItem = menu.findItem(R.id.shareButton);
		MenuItemCompat.setShowAsAction(shareItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.showContact), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		
		// MenuItem showChats = menu.findItem(R.id.showChatsButton);
		// if (showChats != null) {
		// showChats.setVisible(getActivity() instanceof ChatActivity);
		// }
		//
		// // Share button support
		// MenuItem share = menu.findItem(R.id.shareButton);
		//
		// boolean visible = false;
		// if (chat != null) {
		// final Jaxmpp jaxmpp = ((MessengerApplication)
		// getActivity().getApplicationContext()).getMultiJaxmpp().get(
		// chat.getSessionObject());
		// try {
		// JID jid = chat.getJid();
		//
		// if (jid.getResource() == null) {
		// jid = FileTransferUtility.getBestJidForFeatures(jaxmpp,
		// jid.getBareJid(), FileTransferUtility.FEATURES);
		// }
		//
		// if (jid != null) {
		// visible = FileTransferUtility.resourceContainsFeatures(jaxmpp, jid,
		// FileTransferUtility.FEATURES);
		// }
		// } catch (XMLException e) {
		// }
		// } else {
		// Log.v(TAG, "no chat for fragment");
		// }
		// share.setVisible(visible);
		//
		// MenuItem showContact = menu.findItem(R.id.showContact);
		// showContact.setVisible(!helper.isXLarge());

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// this.helper = TigaseMobileMessengerActivityHelper.createInstance();

		this.layout = (ChatView) inflater.inflate(R.layout.chat, null);
		layout.setChatHistoryFragment(this);
		layout.init();

		if (DEBUG)
			Log.d(TAG, "onActivityCreated ChatFragment " + savedInstanceState);
		if (DEBUG)
			Log.d(TAG, "Arguments: " + getArguments());
		if (DEBUG)
			Log.d(TAG, "Activity: " + getActivity());

		this.lv = (ListView) layout.findViewById(R.id.chat_conversation_history);
		registerForContextMenu(lv);

		lv.setAdapter(chatAdapter);

		return layout;
	}

	@Override
	public void onDestroyView() {
		if (this.layout != null)
			this.layout.destroy();
		super.onDestroyView();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		chatAdapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		chatAdapter.swapCursor(cursor);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// if (item.getItemId() == R.id.showChatsButton) {
		// SlidingPaneLayout slidingPaneLayout = (SlidingPaneLayout)
		// getActivity().findViewById(R.id.chat_sliding_pane_layout);
		// if (slidingPaneLayout.isOpen()) {
		// slidingPaneLayout.closePane();
		// } else {
		// slidingPaneLayout.openPane();
		// }
		// } else
		if (item.getItemId() == R.id.closeChatButton) {
			layout.cancelEdit();

			new Thread() {
				public void run() {
					IJaxmppService jaxmppService = ((MainActivity) getActivity()).getJaxmppService();
					try {
						jaxmppService.closeChat(account, recipient.toString(), threadId);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}.start();

			getActivity().onBackPressed();

		} else if (item.getItemId() == R.id.shareImageButton) {
			Log.v(TAG, "share selected for = " + recipient.toString());
			Intent pickerIntent = new Intent(Intent.ACTION_PICK);
			pickerIntent.setType("image/*");
			pickerIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			startActivityForResult(pickerIntent, MainActivity.SELECT_FOR_SHARE);
		} else if (item.getItemId() == R.id.shareVideoButton) {
			 Log.v(TAG, "share selected for = " + recipient.toString());
			 Intent pickerIntent = new Intent(Intent.ACTION_PICK);
			 pickerIntent.setType("video/*");
			 pickerIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			 startActivityForResult(pickerIntent, MainActivity.SELECT_FOR_SHARE);
		} else if (item.getItemId() == R.id.showContact) {
			// Intent intent = new Intent(getActivity(), ContactActivity.class);
			// intent.putExtra("jid", chat.getJid().getBareJid().toString());
			// intent.putExtra("account",
			// chat.getSessionObject().getUserBareJid().toString());
			// startActivity(intent);
			Bundle args = new Bundle();
			args.putString("account", account);
			args.putString("jid", recipient.getBareJid().toString());
			ContactFragment frag = new ContactFragment();
			frag.setArguments(args);
			((MainActivity) getActivity()).switchFragments(frag, ContactFragment.FRAG_TAG);
		}
		return true;
	}

	@Override
	public void onPause() {
		Intent intent = new Intent();
		intent.setAction(JaxmppService.CLIENT_FOCUS);
		intent.putExtra("chat", "");
		getActivity().sendBroadcast(intent);
		if (layout != null) {
			layout.composing = false;
		}
		setLocalChatState(ChatState.inactive);
		super.onPause();
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
				&& Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			MenuInflater inflater = new MenuInflater(this.getActivity().getApplicationContext());
			onCreateOptionsMenu(menu, inflater);
		}

		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onResume() {
		// if (((ChatAdapter) lv.getAdapter()).getCursor().isClosed()) {
		// ((ChatAdapter) lv.getAdapter()).swapCursor(getCursor());
		// }

		super.onResume();

		updatePresence();
		layout.updateClientIndicator();

		Intent intent = new Intent();
		intent.setAction(JaxmppService.CLIENT_FOCUS);
		if (this.recipient != null)
			intent.putExtra("chat", this.recipient.toString());
		getActivity().sendBroadcast(intent);

		Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + Uri.encode(recipient.getBareJid().toString()));
		ContentValues values = new ContentValues();
		values.put(ChatTableMetaData.FIELD_AUTHOR_JID, recipient.getBareJid().toString());
		values.put(ChatTableMetaData.FIELD_STATE, ChatTableMetaData.STATE_INCOMING);
		getActivity().getContentResolver().update(uri, values, null, null);

		this.setLocalChatState(ChatState.active);
		this.notifyChatState();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (DEBUG)
			Log.d(TAG, "Save state of ChatFragment");
		if (outState != null)
			outState.putString("recipient", recipient.toString());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStart() {
		if (DEBUG)
			Log.d(TAG, "Start ChatFragment");
		// final MultiJaxmpp jaxmpp = ((MessengerApplication)
		// getActivity().getApplicationContext()).getMultiJaxmpp();
		//
		// jaxmpp.addListener(PresenceModule.ContactAvailable,
		// this.presenceListener);
		// jaxmpp.addListener(PresenceModule.ContactUnavailable,
		// this.presenceListener);
		// jaxmpp.addListener(PresenceModule.ContactChangedPresence,
		// this.presenceListener);
		//
		// jaxmpp.addListener(MessageModule.ChatUpdated,
		// this.chatUpdateListener);

		super.onStart();

		if (observer == null) {
			observer = new ContentObserver(null) {
				@Override
				public void onChange(boolean selfChange) {
					layout.post(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							updatePresence();
							notifyChatState();
						}
					});
				}
			};
		}

		getActivity().getContentResolver().registerContentObserver(Uri.parse(RosterProvider.CONTENT_URI), true, observer);
		getActivity().getContentResolver().registerContentObserver(Uri.parse(OpenChatsProvider.OPEN_CHATS_URI), true, observer);

		updatePresence();
		layout.updateClientIndicator();
	}

	@Override
	public void onStop() {
		if (DEBUG)
			Log.d(TAG, "Stop ChatFragment");
		// final MultiJaxmpp jaxmpp = ((MessengerApplication)
		// getActivity().getApplicationContext()).getMultiJaxmpp();
		//
		// jaxmpp.removeListener(MessageModule.ChatUpdated,
		// this.chatUpdateListener);
		//
		// jaxmpp.removeListener(PresenceModule.ContactAvailable,
		// this.presenceListener);
		// jaxmpp.removeListener(PresenceModule.ContactUnavailable,
		// this.presenceListener);
		// jaxmpp.removeListener(PresenceModule.ContactChangedPresence,
		// this.presenceListener);
		if (observer != null) {
			getActivity().getContentResolver().unregisterContentObserver(observer);
			observer = null;
		}
		super.onStop();
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (!isVisibleToUser && layout != null) {
			layout.composing = false;
		}
		setLocalChatState(isVisibleToUser ? ChatState.active : ChatState.inactive);
	}

	// private void setChatId(final BareJID account, final long chatId) {
	// MultiJaxmpp multi = ((MessengerApplication)
	// getActivity().getApplicationContext()).getMultiJaxmpp();
	//
	// chatWrapper = multi.getChatById(chatId);
	// if (chatWrapper != null) {
	// chat = chatWrapper.getChat();
	//
	// Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" +
	// Uri.encode(chat.getJid().getBareJid().toString()));
	// ContentValues values = new ContentValues();
	// values.put(ChatTableMetaData.FIELD_AUTHOR_JID,
	// chat.getJid().getBareJid().toString());
	// values.put(ChatTableMetaData.FIELD_STATE,
	// ChatTableMetaData.STATE_INCOMING);
	// getActivity().getContentResolver().update(uri, values, null, null);
	//
	// return;
	// }
	//
	// throw new RuntimeException("Chat (id:" + chatId + ", account:" + account
	// + ")  not found!");
	// }

	private void showMessageDetails(final long id) {
		Cursor cc = null;
		final java.text.DateFormat df = DateFormat.getDateFormat(getActivity());
		final java.text.DateFormat tf = DateFormat.getTimeFormat(getActivity());

		try {
			cc = getChatEntry(id);

			Dialog alertDialog = new Dialog(getActivity());
			alertDialog.setContentView(R.layout.chat_item_details_dialog);
			alertDialog.setCancelable(true);
			alertDialog.setCanceledOnTouchOutside(true);
			alertDialog.setTitle("Message details");

			TextView msgDetSender = (TextView) alertDialog.findViewById(R.id.msgDetSender);
			msgDetSender.setText(cc.getString(cc.getColumnIndex(ChatTableMetaData.FIELD_JID)));

			Date timestamp = new Date(cc.getLong(cc.getColumnIndex(ChatTableMetaData.FIELD_TIMESTAMP)));
			TextView msgDetReceived = (TextView) alertDialog.findViewById(R.id.msgDetReceived);
			msgDetReceived.setText(df.format(timestamp) + " " + tf.format(timestamp));

			final int state = cc.getInt(cc.getColumnIndex(ChatTableMetaData.FIELD_STATE));
			TextView msgDetState = (TextView) alertDialog.findViewById(R.id.msgDetState);
			switch (state) {
			case ChatTableMetaData.STATE_INCOMING:
				msgDetState.setText("Received");
				break;
			case ChatTableMetaData.STATE_OUT_SENT:
				msgDetState.setText("Sent");
				break;
			case ChatTableMetaData.STATE_OUT_NOT_SENT:
				msgDetState.setText("Not sent");
				break;
			default:
				msgDetState.setText("?");
				break;
			}

			alertDialog.show();
		} finally {
			if (cc != null && !cc.isClosed())
				cc.close();
		}
	}

	protected void notifyChatState() {
		if (getActivity() instanceof MainActivity) {
			MainActivity activity = (MainActivity) getActivity();
			try {
				String chatStateStr = activity.getJaxmppService().getRecipientChatState(account, recipient.toString(), threadId);
				if (chatStateStr != null && chatStateStr.length() > 0) {
					ChatState state = ChatState.valueOf(chatStateStr);
					if (state != ChatState.active) {
						Toast toast = Toast.makeText(activity, name + " is " + state.name(), 5);
						toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 2);
						toast.show();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected void updatePresence() {
		if (getActivity() instanceof MainActivity) {
			MainActivity activity = (MainActivity) getActivity();
			activity.fragmentChanged(this);
		}
		// if (chat != null) {
		// CPresence cp = RosterDisplayTools.getShowOf(chat.getSessionObject(),
		// chat.getJid().getBareJid());
		//
		// layout.setImagePresence(cp);
		//
		// helper.updateActionBar(getActivity(), chatWrapper);
		//
		// }
	}

	@Override
	public int getHeaderViewId() {
		return R.layout.actionbar_status;
	}

	@Override
	public View updateHeaderView(View view) {
		CPresence p = null;
		try {
			p = ((MainActivity) getActivity()).getJaxmppService().getBestPresence(account, recipient.getBareJid().toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		int icon = R.drawable.user_offline;
		String descr = null;
		if (p != null) {
			icon = RosterAdapterHelper.cPresenceToImageResource(p.getStatus());
			descr = p.getDescription();
		}

		Holder holder = (Holder) view.getTag();
		if (holder == null) {
			holder = new Holder();
			holder.title = (TextView) view.findViewById(R.id.title);
			holder.description = (TextView) view.findViewById(R.id.description);
			holder.status = (ImageView) view.findViewById(R.id.status);
			view.setTag(holder);
		}
		holder.title.setText(name);
		holder.description.setText(descr == null ? "" : descr);
		holder.status.setImageResource(icon);
		// BareJID jid = c.getChat().getJid().getBareJid();
		// RosterItem ri = c.getChat().getSessionObject().getRoster().get(jid);
		// subtitle = "Chat with " + (ri != null ? ri.getName() :
		// jid.toString());
		//
		// icon = R.drawable.user_offline;
		// CPresence p =
		// RosterDisplayTools.getShowOf(c.getChat().getSessionObject(),
		// c.getChat().getJid().getBareJid());
		// c.getChat().getSessionObject().getPresence().getPresence(c.getChat().getJid());
		// switch (p) {
		// case chat:
		// icon = R.drawable.user_free_for_chat;
		// break;
		// case online:
		// icon = R.drawable.user_available;
		// break;
		// case away:
		// icon = R.drawable.user_away;
		// break;
		// case xa:
		// icon = R.drawable.user_extended_away;
		// break;
		// case dnd:
		// icon = R.drawable.user_busy;
		// break;
		// default:
		// break;
		// }
		return view;
	}

	private void setLocalChatState(final ChatState state) {
		new Thread() {
			public void run() {
				try {
					((MainActivity) getActivity()).getJaxmppService().setOwnChatState(account, recipient.toString(), threadId,
							state.name());
				} catch (Exception ex) {
					Log.e(TAG, "Exception setting local chat state", ex);
				}
			}
		}.start();
	}
}
