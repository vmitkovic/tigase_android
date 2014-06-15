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
package org.tigase.messenger.phone.pro.muc;

import org.tigase.messenger.phone.pro.CustomHeader;
import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.Preferences;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.providers.ChatHistoryProvider;
import org.tigase.messenger.phone.pro.db.providers.OpenChatsProvider;
import org.tigase.messenger.phone.pro.db.providers.RosterProvider;
import org.tigase.messenger.phone.pro.roster.CPresence;
import org.tigase.messenger.phone.pro.roster.RosterAdapterHelper;
import org.tigase.messenger.phone.pro.service.JaxmppService;

import tigase.jaxmpp.android.chat.OpenChatTableMetaData;
import tigase.jaxmpp.core.client.JID;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MucRoomFragment extends Fragment implements LoaderCallbacks<Cursor>, CustomHeader {
	
	private class Holder {
		TextView description;
		ImageView status;
		TextView title;
	}
	
	private static final boolean DEBUG = false;
	public static final String FRAG_TAG = "MucRoomFragment";
	private static final String TAG = "MUC";

	protected static final int SHOW_OCCUPANTS = 1021;
	
	public static Fragment newInstance(String account, JID roomJid) {
		MucRoomFragment f = new MucRoomFragment();

		Bundle args = new Bundle();
		args.putString("room", roomJid.toString());
		args.putString("account", account);
		f.setArguments(args);

		return f;
	}

//	public static void openRoom(FragmentActivity activity, String account, long roomId, boolean xlarge) {
//			Intent newIntent = new Intent(activity, MucActivity.class);
//			newIntent.putExtra("roomId", roomId);
//			newIntent.putExtra("account", account);
//			activity.startActivity(newIntent);
//	}

//	private ChatWrapper chatWrapper;
//
//	private Listener<ConnectorEvent> connectionListener;

	private EditText ed;

//	private TigaseMobileMessengerActivityHelper helper;
//
//	private JaxmppCore jaxmpp;

	private ListView lv;

	private MucAdapter mucAdapter;

//	private final Listener<MucEvent> mucListener;

	private final OnClickListener nickameClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			addNicknameToEdit((((TextView) v).getText()).toString());
		}
	};

	private SharedPreferences prefs;

	private ProgressBar progressBar;

	private String account;
	private JID room;
	private String name;
	
	private ContentObserver observer = null;

	private Button sendButton;

	private ImageView stateImage;

	private View view;

	public MucRoomFragment() {
//		this.mucListener = new Listener<MucModule.MucEvent>() {
//
//			@Override
//			public void handleEvent(MucEvent be) throws JaxmppException {
//				onMucEvent(be);
//			}
//		};
//		this.connectionListener = new Listener<ConnectorEvent>() {
//
//			@Override
//			public void handleEvent(ConnectorEvent be) throws JaxmppException {
//				updatePresenceImage();
//			}
//		};
	}

	void addNicknameToEdit(String n) {
		String ttt = ed.getText().toString();
		if (ttt == null || ttt.length() == 0) {
			ed.append(n + ": ");
		} else {
			ed.append(" " + n);
		}
	}

	void cancelEdit() {
		if (ed == null)
			return;
		final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

		ed.post(new Runnable() {

			@Override
			public void run() {
				ed.clearComposingText();
				imm.hideSoftInputFromWindow(ed.getWindowToken(), 0);
			}
		});

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getArguments() != null) {
			if (getArguments().containsKey("roomId")) {
				long roomId = getArguments().getLong("roomId");
				Cursor c = this.getActivity().getContentResolver().query(Uri.parse(OpenChatsProvider.OPEN_CHATS_URI), 
						new String[] { OpenChatTableMetaData.FIELD_ACCOUNT, OpenChatTableMetaData.FIELD_JID, OpenChatTableMetaData.FIELD_NICKNAME }, 
						"open_chats."+OpenChatTableMetaData.FIELD_ID + "= ?", new String[] { String.valueOf(roomId) }, null);
				try {
					Log.v(TAG, "found " + c.getCount() + " for chatId = " + roomId);
					if (c.moveToNext()) {
						this.account = c.getString(0);
						this.room = JID.jidInstance(c.getString(1));
						this.name = c.getString(2);
					}
				} finally {
					c.close();
				}					
			}
			else if (getArguments().containsKey("recipient") || getArguments().containsKey("jid")) {
				String recipient = getArguments().containsKey("recipient") ? getArguments().getString("recipient") : getArguments().getString("jid");
				this.room = JID.jidInstance(recipient);
				this.account = getArguments().getString("account");
				// fix thread id
				// maybe we should retrieve here recipients name as well? or maybe we should move it to method calling this method
				// or maybe it is not needed at all
				Cursor c = this.getActivity().getContentResolver().query(Uri.parse(OpenChatsProvider.OPEN_CHATS_URI), 
						new String[] { OpenChatTableMetaData.FIELD_NICKNAME }, "open_chats." + OpenChatTableMetaData.FIELD_ACCOUNT + " = ? AND open_chats." + OpenChatTableMetaData.FIELD_JID + "= ?", 
						new String[] { account, this.room.getBareJid().toString() }, null);
				try {
					if (c.moveToNext()) {
						this.name = c.getString(0);
					}
				} finally {
					c.close();
				}			
			}
			else {
				Log.e(TAG, "something gone really bad - no proper arguments found!");
			}
		}	

		
		this.mucAdapter = new MucAdapter(getActivity(), R.layout.muc_chat_item, nickameClickListener);
		mucAdapter.setParticipantName(name);
		getLoaderManager().initLoader(this.room.hashCode(), null, this);
		mucAdapter.registerDataSetObserver(new DataSetObserver() {

			@Override
			public void onChanged() {
				super.onChanged();
				if (DEBUG)
					Log.i(TAG, "Changed!");
				if (lv != null)
					lv.post(new Runnable() {

						@Override
						public void run() {
							lv.setSelection(Integer.MAX_VALUE);
						}
					});
			}
		});

		TextView title = (TextView) view.findViewById(R.id.textView1);
		if (title != null) {
			title.setText("--" + room.getBareJid().toString());
		}
		
		// maybe we should do this thru some interprocess listener registration? as RosterUpdateCallback.aidl
		ed.setEnabled(true);//room.getState() == State.joined);
		sendButton.setEnabled(true);//room.getState() == State.joined);
		lv.setAdapter(mucAdapter);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SHOW_OCCUPANTS && resultCode == Activity.RESULT_OK) {
			String n = data.getStringExtra("nickname");
			if (n != null) {
				addNicknameToEdit(n);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);
		this.setHasOptionsMenu(true);

		this.prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

//		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();
//		jaxmpp.addListener(MucModule.StateChange, this.mucListener);
//		jaxmpp.addListener(Connector.StateChanged, this.connectionListener);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(getActivity().getApplicationContext(), Uri.parse(ChatHistoryProvider.CHAT_URI + "/"
				+ Uri.encode(room.getBareJid().toString())), null, null, null, null);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.muc_main_menu, menu);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.closeChatButton), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.showOccupantsButton), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
//		MenuItem showChats = menu.findItem(R.id.showChatsButton);
//		if (showChats != null) {
//			showChats.setVisible(getActivity() instanceof MucActivity);
//		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//		this.helper = TigaseMobileMessengerActivityHelper.createInstance();

		this.view = inflater.inflate(R.layout.muc_conversation, container, false);

		this.ed = (EditText) view.findViewById(R.id.chat_message_entry);
		this.ed.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				boolean ets = prefs.getBoolean(Preferences.ENTER_TO_SEND_KEY, true);
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

		this.sendButton = (Button) view.findViewById(R.id.chat_send_button);
		sendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (DEBUG)
					Log.i(TAG, "Klikniete");

				sendMessage();

			}
		});

		this.lv = (ListView) view.findViewById(R.id.chat_conversation_history);

		lv.post(new Runnable() {

			@Override
			public void run() {
				lv.setSelection(Integer.MAX_VALUE);
			}
		});

		return view;
	}

	@Override
	public void onDestroy() {
//		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();
//		jaxmpp.removeListener(MucModule.StateChange, this.mucListener);
//		jaxmpp.removeListener(Connector.StateChanged, this.connectionListener);

		super.onDestroy();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mucAdapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mucAdapter.swapCursor(cursor);
	}

//	protected void onMucEvent(MucEvent be) {
//		updatePresenceImage();
//	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.showOccupantsButton) {
			Intent chatListActivity = new Intent(getActivity(), OccupantsListActivity.class);
			//chatListActivity.putExtra("roomId", room.getId());
			chatListActivity.putExtra("jid", room.toString());
			chatListActivity.putExtra("account", account);

			this.startActivityForResult(chatListActivity, SHOW_OCCUPANTS);
//		} else if (item.getItemId() == R.id.showChatsButton) {
////			Intent chatListActivity = new Intent(getActivity(), ChatListActivity.class);
////			this.getActivity().startActivityForResult(chatListActivity, TigaseMobileMessengerActivity.REQUEST_CHAT);
//			SlidingPaneLayout slidingPaneLayout = (SlidingPaneLayout) getActivity().findViewById(R.id.chat_sliding_pane_layout);
//			if (slidingPaneLayout.isOpen()) {
//				slidingPaneLayout.closePane();
//			}
//			else {
//				slidingPaneLayout.openPane();
//			}			
		} else if (item.getItemId() == R.id.closeChatButton) {
			cancelEdit();

			final IJaxmppService jaxmppService = ((MainActivity) getActivity()).getJaxmppService();		
			new Thread() {
				public void run() {
					try {
						Log.v(TAG, "leaving room " + room.getBareJid().toString());
						jaxmppService.leaveRoom(account, room.getBareJid().toString());
					} catch (RemoteException e) {
						Log.e(TAG, "Exception closing room " + room.getBareJid().toString());
					}
				}
			}.start();
			getActivity().onBackPressed();
			
			// final ViewPager viewPager = ((TigaseMobileMessengerActivity)
			// this.getActivity()).viewPager;
//			final Jaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(
//					room.getSessionObject());
//			final MucModule cm = jaxmpp.getModule(MucModule.class);
//
//			// viewPager.setCurrentItem(1);
//			AsyncTask<Void, Void, Void> t = new AsyncTask<Void, Void, Void>() {
//
//				@Override
//				protected Void doInBackground(Void... params) {
//					try {
//						cm.leave(room);
//					} catch (JaxmppException e) {
//						Log.w(TAG, "Chat close problem!", e);
//					}
//					return null;
//				}
//
//				@Override
//				protected void onPostExecute(Void param) {
//					NavUtils.navigateUpTo(getActivity(), new Intent(getActivity(),
//							TigaseMobileMessengerActivity.class));		
//				}
//			};
//
//			t.execute();
		}
		return true;
	}

	@Override
	public void onPause() {
		Intent intent = new Intent();
		intent.setAction(JaxmppService.CLIENT_FOCUS);
		intent.putExtra("room", "");		
		getActivity().sendBroadcast(intent);
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
		super.onResume();
		updatePresenceImage();

		Intent intent = new Intent();
		intent.setAction(JaxmppService.CLIENT_FOCUS);
		intent.putExtra("room", room.getBareJid().toString());
		getActivity().sendBroadcast(intent);	
	}

	@Override
	public void onStart() {
		super.onStart();
		
		if (observer == null) {
			observer = new ContentObserver(null) {
				@Override
				public void onChange(boolean selfChange) {
					lv.post(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							updatePresenceImage();
						}				
					});
				}
			};
		}		
		
		getActivity().getContentResolver().registerContentObserver(Uri.parse(OpenChatsProvider.OPEN_CHATS_URI), true, observer);		

		updatePresenceImage();
	}
	
	public void onStop() {
		if (observer != null) {
			getActivity().getContentResolver().unregisterContentObserver(observer);
			observer = null;
		}
		super.onStop();
	}
	
	protected void sendMessage() {
		if (ed == null)
			return;

		final String t = ed.getText().toString();
		ed.setText("");

		if (t == null || t.length() == 0)
			return;
		if (DEBUG)
			Log.d(TAG, "Send: " + t);

		final IJaxmppService jaxmppService = ((MainActivity) getActivity()).getJaxmppService();		
		new Thread() {
			public void run() {
				try {
					jaxmppService.sendRoomMessage(account, room.getBareJid()
							.toString(), t);
				} catch (RemoteException e) {
					Log.e(TAG, "Exception sending message to room "
							+ room.getBareJid().toString());
				}
			}
		}.start();
//		AsyncTask<String, Void, Void> task = new AsyncTask<String, Void, Void>() {
//			@Override
//			public Void doInBackground(String... ts) {
//				String t = ts[0];
//				Log.d(TAG, "Send: " + t);
//				try {
//					room.sendMessage(t);
//				} catch (Exception e) {
//					Log.e(TAG, e.getMessage(), e);
//				}
//
//				return null;
//			}
//		};
//		task.execute(t);
		updatePresenceImage();
	}

	private void updatePresenceImage() {
		if (getActivity() instanceof MainActivity) {
			MainActivity activity = (MainActivity) getActivity();
			activity.fragmentChanged(this);					
		}		
		
		if (view != null) {
//			final boolean connected = jaxmpp.isConnected();
//			Runnable r = new Runnable() {
//
//				@Override
//				public void run() {
//					// TODO Auto-generated method stub
//
//					Log.i(TAG, "MUC STATE: " + room.getState() + ", Connected: " + connected);
//
//					if (ed != null) {
//						if (!connected) {
//							Log.i(TAG, "MUC Field Button  false false 0");
//
//							ed.setEnabled(false);
//							sendButton.setEnabled(false);
//						} else if (room.getState() == State.joined && !ed.isEnabled()) {
//							Log.i(TAG, "MUC Field Button  true true ");
//
//							ed.setEnabled(true);
//							sendButton.setEnabled(true);
//						} else if (room.getState() != State.joined && ed.isEnabled()) {
//							Log.i(TAG, "MUC Field Button  false false 1");
//
//							ed.setEnabled(false);
//							sendButton.setEnabled(false);
//						}
//					}
//
//					Log.i(TAG, "MUC state image " + (stateImage != null));
//					if (stateImage != null) {
//						stateImage.post(new Runnable() {
//
//							@Override
//							public void run() {
//								if (!connected) {
//									Log.i(TAG, "MUC state image off");
//
//									progressBar.setVisibility(View.GONE);
//									stateImage.setImageResource(R.drawable.user_offline);
//								} else if (room.getState() == State.not_joined) {
//									Log.i(TAG, "MUC state image off");
//
//									progressBar.setVisibility(View.GONE);
//									stateImage.setImageResource(R.drawable.user_offline);
//								} else if (room.getState() == State.requested) {
//									Log.i(TAG, "MUC state image wait");
//
//									progressBar.setVisibility(View.VISIBLE);
//									stateImage.setVisibility(View.GONE);
//								} else if (room.getState() == State.joined) {
//									Log.i(TAG, "MUC state image oavailable");
//
//									progressBar.setVisibility(View.GONE);
//									stateImage.setImageResource(R.drawable.user_available);
//								}
//							}
//						});
//					}
//
//					helper.updateActionBar(getActivity(), chatWrapper);
//				}
//			};
//			view.post(r);
		}
	}

	@Override
	public int getHeaderViewId() {
		return R.layout.actionbar_status;
	}

	@Override
	public View updateHeaderView(View view) {
		int state = CPresence.OFFLINE;
		Cursor c = this.getActivity().getContentResolver().query(Uri.parse(OpenChatsProvider.OPEN_CHATS_URI), 
				new String[] { OpenChatTableMetaData.FIELD_NICKNAME, OpenChatsProvider.FIELD_STATE }, 
				"open_chats." + OpenChatTableMetaData.FIELD_ACCOUNT + " = ? AND open_chats." + OpenChatTableMetaData.FIELD_JID + "= ?", 
				new String[] { account, this.room.getBareJid().toString() }, null);
		try {
			if (c.moveToNext()) {
				//this.name = c.getString(0);
				try {
					state = c.getInt(1);
				}
				catch (NullPointerException ex) {
					// in case on NPE, just ignore exception
				}
			}
		} finally {
			c.close();
		}			
		// maybe we could use it for room subject?
		String descr = null;
		int icon = RosterAdapterHelper.cPresenceToImageResource(state);
		
		Holder holder = (Holder) view.getTag();
		if (holder == null) {
			holder = new Holder();
			holder.title = (TextView) view.findViewById(R.id.title);
			holder.description = (TextView) view.findViewById(R.id.description);
			holder.status = (ImageView) view.findViewById(R.id.status);
			view.setTag(holder);
		}
		holder.title.setText("Room " + room.getBareJid().toString());
		holder.description.setText(descr == null ? "" : descr);
		holder.status.setImageResource(icon);
		return view;
	}
}
