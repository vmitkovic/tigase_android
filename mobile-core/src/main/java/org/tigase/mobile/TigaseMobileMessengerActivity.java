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
package org.tigase.mobile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.tigase.mobile.MultiJaxmpp.ChatWrapper;
import org.tigase.mobile.accountstatus.AccountsStatusActivity;
import org.tigase.mobile.authenticator.AuthenticatorActivity;
import org.tigase.mobile.bookmarks.BookmarksActivity;
import org.tigase.mobile.chat.ChatActivity;
import org.tigase.mobile.chat.ChatHistoryFragment;
import org.tigase.mobile.chatlist.ChatListActivity;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.muc.JoinMucDialog;
import org.tigase.mobile.muc.MucRoomFragment;
import org.tigase.mobile.preferences.MessengerPreferenceActivity;
import org.tigase.mobile.roster.ContactActivity;
import org.tigase.mobile.roster.ContactFragment;
import org.tigase.mobile.roster.RosterFragment;
import org.tigase.mobile.security.SecureTrustManagerFactory.DataCertificateException;
import org.tigase.mobile.service.GeolocationFeature;
import org.tigase.mobile.service.JaxmppService;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.BaseEvent;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.AbstractMessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class TigaseMobileMessengerActivity extends FragmentActivity {

	private class DrawerMenuAdapter extends ArrayAdapter<DrawerMenuItem> {

		private final Context context;
		private final List<DrawerMenuItem> items;

		public DrawerMenuAdapter(Context context, int textViewResourceId, List<DrawerMenuItem> items) {
			super(context, textViewResourceId, items);
			this.context = context;
			this.items = items;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.main_left_drawer_item, parent, false);
			TextView textView = (TextView) rowView.findViewById(R.id.main_left_drawer_item_text);
			ImageView imageView = (ImageView) rowView.findViewById(R.id.main_left_drawer_item_icon);

			DrawerMenuItem item = items.get(position);

			textView.setText(item.text);
			imageView.setImageResource(item.icon);

			return rowView;
		}

		@Override
		public boolean isEnabled(int pos) {
			DrawerMenuItem item = getItem(pos);
			boolean connected = false;
			final MultiJaxmpp multi = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();
			for (JaxmppCore jaxmpp : multi.get()) {
				connected |= jaxmpp.isConnected();
			}
			return super.isEnabled(pos) && (!item.connectionRequired || connected);
		}

	}

	private class DrawerMenuItem {
		final boolean connectionRequired;
		final int icon;
		final int id;
		final int text;

		public DrawerMenuItem(int id, int text, int icon) {
			this(id, text, icon, false);
		}

		public DrawerMenuItem(int id, int text, int icon, boolean connectionRequired) {
			this.id = id;
			this.text = text;
			this.icon = icon;
			this.connectionRequired = connectionRequired;
		}
	}

	private class RosterClickReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// if (!active)
			// return;
			final long id = intent.getLongExtra("id", -1);
			final String resource = intent.getStringExtra("resource");

			final Cursor cursor = getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI + "/" + id), null, null,
					null, null);
			JID jid = null;
			BareJID account = null;
			try {
				cursor.moveToNext();
				jid = JID.jidInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_JID)));
				account = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_ACCOUNT)));

			} finally {
				cursor.close();
			}

			final Jaxmpp jaxmpp = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get(account);

			for (RosterItem i : jaxmpp.getRoster().getAll()) {
				if (id == i.getId()) {
					openChatWith(i, resource);
					break;
				}
			}

		}
	}

	public final static int ABOUT_DIALOG = 1;

	public static final String CERT_UNTRUSTED_ACTION = "org.tigase.mobile.CERT_UNTRUSTED_ACTION";

	public static final String CLIENT_FOCUS_MSG = "org.tigase.mobile.CLIENT_FOCUS_MSG";

	public final static int CONTACT_REMOVE_DIALOG = 2;

	private static final boolean DEBUG = false;

	public static final String ERROR_ACTION = "org.tigase.mobile.ERROR_ACTION";

	public static final String MUC_ERROR_ACTION = "org.tigase.mobile.MUC_ERROR_ACTION";

	// private ListView rosterList;

	public static final String MUC_MESSAGE_ACTION = "org.tigase.mobile.MUC_MESSAGE_ACTION";

	public static final String NEW_CHAT_MESSAGE_ACTION = "org.tigase.mobile.NEW_CHAT_MESSAGE_ACTION";

	public final static int NEWS_DIALOG = 3;

	public static final int REQUEST_CHAT = 3;

	public static final String ROSTER_CLICK_MSG = "org.tigase.mobile.ROSTER_CLICK_MSG";

	public static final int SELECT_FOR_SHARE = 2;

	public static final int SHOW_OCCUPANTS = 3;

	public final static String STATE_CURRENT_PAGE = "TigaseMobileMessengerActivity.STATE_CURRENT_PAGE";

	private static final String TAG = "TigaseMobileMessengerActivity";

	public static final String WARNING_ACTION = "org.tigase.mobile.WARNING_ACTION";

	protected static RosterFragment createRosterFragment(String string) {
		return RosterFragment.newInstance(string);
	}

	private final Listener<BaseEvent> chatListener;

	protected DrawerLayout drawerLayout;

	private ListView drawerList;

	protected ActionBarDrawerToggle drawerToggle;

	public final TigaseMobileMessengerActivityHelper helper;

	private SharedPreferences mPreferences;

	private final BroadcastReceiver mucErrorReceiver;

	private final OnSharedPreferenceChangeListener prefChangeListener;

	private final RosterClickReceiver rosterClickReceiver = new RosterClickReceiver();

	private boolean rosterLayoutChanged = false;
	
	public TigaseMobileMessengerActivity() {
		helper = TigaseMobileMessengerActivityHelper.createInstance();

		this.mucErrorReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				showMucError(intent.getExtras());
			}
		};

		this.chatListener = new Listener<BaseEvent>() {

			@Override
			public void handleEvent(BaseEvent be) throws JaxmppException {
				if (be instanceof AbstractMessageEvent)
					onMessageEvent((AbstractMessageEvent) be);
			}
		};
        this.prefChangeListener = new OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                        if (Preferences.ROSTER_LAYOUT_KEY.equals(key) || Preferences.ROSTER_SORTING_KEY.equals(key)) {
                        	rosterLayoutChanged = true;                       	
                        }
                }
        };
	}

	protected ChatWrapper findChatWrapper(final RosterItem rosterItem) {
		List<ChatWrapper> l = getChatList();
		for (int i = 0; i < l.size(); i++) {
			ChatWrapper c = l.get(i);
			if (c.isChat() && c.getChat().getSessionObject() == rosterItem.getSessionObject()
					&& c.getChat().getJid().getBareJid().equals(rosterItem.getJid()))
				return c;
		}
		return null;
	}

	protected List<ChatWrapper> getChatList() {
		return ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().getChats();
	}

	private void notifyPageChange(int msg) {
		Intent intent = new Intent();
		intent.setAction(CLIENT_FOCUS_MSG);
		intent.putExtra("page", msg);

		sendBroadcast(intent);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (DEBUG)
			Log.d(TAG, "onActivityResult()");
		if (requestCode == REQUEST_CHAT && resultCode == Activity.RESULT_OK) {
			openChat(data.getExtras());
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (DEBUG)
			Log.d(TAG, "onCreate()");

		IntentFilter filter = new IntentFilter(JaxmppService.MUC_ERROR_MSG);
		registerReceiver(mucErrorReceiver, filter);

		super.onCreate(savedInstanceState);

		this.mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		this.mPreferences.registerOnSharedPreferenceChangeListener(prefChangeListener);

		boolean autostart = mPreferences.getBoolean(Preferences.AUTOSTART_KEY, true);
		autostart &= mPreferences.getBoolean(Preferences.SERVICE_ACTIVATED, true);
		if (autostart && !JaxmppService.isServiceActive()) {
			Intent intent = new Intent(this, JaxmppService.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startService(intent);
		}

		AccountManager accountManager = AccountManager.get(this);
		Account[] accounts = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
		String previouslyStartedVersion = mPreferences.getString(Preferences.LAST_STARTED_VERSION, null);
		mPreferences.edit().putString(Preferences.LAST_STARTED_VERSION, getResources().getString(R.string.app_version)).commit();

		if (previouslyStartedVersion == null && (accounts == null || accounts.length == 0)) {
			Intent intent = new Intent(this, AuthenticatorActivity.class);
			intent.putExtra("new", true);
			startActivity(intent);
			// finish();
		}

		setContentView(R.layout.roster_main);

		helper.updateIsXLarge(findViewById(R.id.main_detail_container) != null);

		getSupportFragmentManager().beginTransaction().replace(R.id.roster_fragment, new RosterFragment()).commit();
//		if (helper.isXLarge()) {
//			// setContentView(R.layout.main_layout);
//			((RosterFragment) getSupportFragmentManager().findFragmentById(R.id.roster_fragment)).setActivateOnItemClick(true);
//		} else {
//			// setContentView(R.layout.roster_main);
//		}

		this.drawerList = (ListView) findViewById(R.id.main_left_drawer);
		this.drawerLayout = (DrawerLayout) findViewById(R.id.roster_main);

		// creating list of items available in drawer menu
		final List<DrawerMenuItem> drawerMenuItems = new ArrayList<DrawerMenuItem>();
		drawerMenuItems.add(new DrawerMenuItem(R.id.accountsList, R.string.accounts, R.drawable.ic_menu_account_list));
		drawerMenuItems.add(new DrawerMenuItem(R.id.joinMucRoom, R.string.join_muc_room, R.drawable.group_chat, true));
		drawerMenuItems.add(new DrawerMenuItem(R.id.bookmarksShow, R.string.bookmarks_show, android.R.drawable.star_off, true));
		drawerMenuItems.add(new DrawerMenuItem(R.id.propertiesButton, R.string.propertiesButton,
				android.R.drawable.ic_menu_preferences));
		drawerMenuItems.add(new DrawerMenuItem(R.id.aboutButton, R.string.aboutButton, android.R.drawable.ic_menu_info_details));

		this.drawerList.setAdapter(new DrawerMenuAdapter(this.getApplicationContext(), R.layout.main_left_drawer_item,
				drawerMenuItems));
		this.drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.accept,
				R.string.accept) {

		};
		drawerLayout.setDrawerListener(this.drawerToggle);
		this.drawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView parent, View view, int position, long id) {
				DrawerMenuItem item = drawerMenuItems.get(position);
				if (item != null) {
					drawerLayout.closeDrawers();
					onOptionsItemSelected(item.id);
				}
			}
		});

		processingNotificationIntent(getIntent());

		try {
			int codeC = getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
			int codeP = mPreferences.getInt("news_dialog_displayed_for", -1);
			if (codeC != codeP) {
				mPreferences.edit().putInt("news_dialog_displayed_for", codeC).commit();
				showNewsInfo();
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle bundle) {
		switch (id) {
		case NEWS_DIALOG: {

			String str = bundle.getString("news_html");

			Builder bldr = new AlertDialog.Builder(this);
			bldr.setTitle("News");
			bldr.setCancelable(true);
			bldr.setMessage(Html.fromHtml(str));
			return bldr.create();
		}

		case CONTACT_REMOVE_DIALOG:
			return null;
		case ABOUT_DIALOG: {

			final Dialog dialog = new Dialog(this);
			dialog.setCancelable(true);
			dialog.setCanceledOnTouchOutside(true);

			dialog.setContentView(R.layout.about_dialog);
			dialog.setTitle(getString(R.string.aboutButton));

			TextView tos = (TextView) dialog.findViewById(R.id.aboutTermsOfService);
			tos.setText(Html.fromHtml("<a href='" + getResources().getString(R.string.termsOfServiceURL) + "'>"
					+ getResources().getString(R.string.termsOfService) + "</a>"));
			tos.setMovementMethod(LinkMovementMethod.getInstance());

			TextView pp = (TextView) dialog.findViewById(R.id.aboutPrivacyPolicy);
			pp.setText(Html.fromHtml("<a href='" + getResources().getString(R.string.privacyPolicyURL) + "'>"
					+ getResources().getString(R.string.privacyPolicy) + "</a>"));
			pp.setMovementMethod(LinkMovementMethod.getInstance());

			Button okButton = (Button) dialog.findViewById(R.id.okButton);
			okButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					dialog.cancel();
				}
			});
			return dialog;
		}
		default:
			return null;
		}
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mucErrorReceiver);

		super.onDestroy();
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (DEBUG)
			Log.d(TAG, "onDetachedFromWindow()");
	}

	protected void onMessageEvent(final AbstractMessageEvent be) {
		try {
			// NPE - why be.getMessage() is null here?
			if (be.getMessage() == null || be.getMessage().getFrom() == null)
				return;
			BareJID from = be.getMessage().getFrom().getBareJid();
			RosterItem it = be.getSessionObject().getRoster().get(from);
			if (it != null) {
				Uri insertedItem = ContentUris.withAppendedId(Uri.parse(RosterProvider.CONTENT_URI), it.getId());
				getApplicationContext().getContentResolver().notifyChange(insertedItem, null);
			}
		} catch (Exception ex) {
			Log.e(TAG, ex.getMessage(), ex);
		}
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		if (DEBUG)
			Log.d(TAG, "onNewIntent() action=" + intent.getAction());
		processingNotificationIntent(intent);
	}

	private boolean onOptionsItemSelected(int id) {
		if (id == R.id.showChatsButton) {
			Intent chatListActivity = new Intent(this, ChatListActivity.class);
			this.startActivityForResult(chatListActivity, TigaseMobileMessengerActivity.REQUEST_CHAT);
			return true;
		} else if (id == R.id.joinMucRoom) {
			JoinMucDialog newFragment = JoinMucDialog.newInstance();
			AsyncTask<Room, Void, Void> r = new AsyncTask<Room, Void, Void>() {

				@Override
				protected Void doInBackground(Room... params) {
					final long roomId = params[0].getId();

					Intent intent = new Intent(TigaseMobileMessengerActivity.this, TigaseMobileMessengerActivity.class);
					intent.setAction(TigaseMobileMessengerActivity.MUC_MESSAGE_ACTION);
					intent.putExtra("roomId", roomId);
					TigaseMobileMessengerActivity.this.startActivity(intent);
					return null;
				}
			};
			newFragment.setAsyncTask(r);
			newFragment.show(getSupportFragmentManager(), "dialog");
			return true;
		} else if (id == android.R.id.home) {
			// setVisiblePage(1);
			return true;
		} else if (id == R.id.aboutButton) {
			showDialog(ABOUT_DIALOG);
			return true;
		} else if (id == R.id.propertiesButton) {
			Intent intent = new Intent().setClass(this, MessengerPreferenceActivity.class);
			this.startActivityForResult(intent, 0);
			return true;
		} else if (id == R.id.disconnectButton) {
			mPreferences.edit().putBoolean(Preferences.SERVICE_ACTIVATED, false).commit();
			final MessengerApplication app = (MessengerApplication) getApplicationContext();
			for (final JaxmppCore j : app.getMultiJaxmpp().get()) {
				(new Thread() {
					@Override
					public void run() {
						try {
							GeolocationFeature.updateLocation(j, null, (Context) null);
							((Jaxmpp) j).disconnect(false);
							app.clearPresences(j.getSessionObject(), false);
						} catch (Exception e) {
							Log.e(TAG, "cant; disconnect account " + j.getSessionObject().getUserBareJid(), e);
						}
					}
				}).start();
			}						
			stopService(new Intent(TigaseMobileMessengerActivity.this, JaxmppService.class));
			return true;
		} else if (id == R.id.connectButton) {
			mPreferences.edit().putBoolean(Preferences.SERVICE_ACTIVATED, true).commit();

			Intent intent = new Intent(TigaseMobileMessengerActivity.this, JaxmppService.class);
			intent.putExtra("focused", true);
			startService(intent);
			return true;
		} else if (id == R.id.bookmarksShow) {
			Intent intent = new Intent(TigaseMobileMessengerActivity.this, BookmarksActivity.class);
			startActivityForResult(intent, REQUEST_CHAT);
			return true;
		} else if (id == R.id.accountsList) {
			Intent intent = new Intent(TigaseMobileMessengerActivity.this, AccountsStatusActivity.class);
			startActivity(intent);
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		boolean result = onOptionsItemSelected(item.getItemId());
		if (result)
			return result;
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		unregisterReceiver(rosterClickReceiver);
		final MultiJaxmpp multi = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();
		multi.removeListener(this.chatListener);
		notifyPageChange(-1);
		// TODO Auto-generated method stub
		super.onPause();
		if (DEBUG)
			Log.d(TAG, "onPause()");

	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (DEBUG)
			Log.d(TAG, "onResume()");

		registerReceiver(rosterClickReceiver, new IntentFilter(ROSTER_CLICK_MSG));

		final MultiJaxmpp multi = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();

		multi.addListener(this.chatListener);

		if (rosterLayoutChanged) {
        	Fragment frag = getSupportFragmentManager().findFragmentById(R.id.roster_fragment);
        	if (frag != null) {
        		getSupportFragmentManager().beginTransaction().replace(R.id.roster_fragment, new RosterFragment()).commit();
        	}			
		}
		
		if (helper.isXLarge()) {
			Fragment frag = getSupportFragmentManager().findFragmentById(R.id.main_detail_container);
			if (frag != null) {
				getSupportFragmentManager().beginTransaction().remove(frag).commit();
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (DEBUG)
			Log.d(TAG, "onStop()");
	}

	private void openChat(Bundle extra) {
		if (extra == null)
			return;

		String account = extra.getString("account");
		long chatId = extra.getLong("chatId", 0);
		long roomId = extra.getLong("roomId", 0);

		Intent intent = null;
		if (chatId != 0) {
			intent = new Intent(this, TigaseMobileMessengerActivity.class);
			intent.setAction(TigaseMobileMessengerActivity.NEW_CHAT_MESSAGE_ACTION);
			intent.putExtra("chatId", chatId);
		} else if (roomId != 0) {
			intent = new Intent(this, TigaseMobileMessengerActivity.class);
			intent.setAction(TigaseMobileMessengerActivity.MUC_MESSAGE_ACTION);
			intent.putExtra("roomId", roomId);
		}
		if (intent != null) {
			intent.putExtra("account", account);
			startActivity(intent);
		}
	}

	protected void openChatWith(final RosterItem rosterItem, final String resource) {
		Runnable r = new Runnable() {

			@Override
			public void run() {

				if (helper.isXLarge()) {
					Bundle arguments = new Bundle();
					arguments.putString("jid", rosterItem.getJid().toString());
					arguments.putString("account", rosterItem.getSessionObject().getUserBareJid().toString());
					ContactFragment fragment = new ContactFragment();
					fragment.setArguments(arguments);
					getSupportFragmentManager().beginTransaction().replace(R.id.main_detail_container, fragment).commit();
				} else {
					Intent intent = new Intent(TigaseMobileMessengerActivity.this, ContactActivity.class);
					intent.putExtra("jid", rosterItem.getJid().toString());
					intent.putExtra("account", rosterItem.getSessionObject().getUserBareJid().toString());
					TigaseMobileMessengerActivity.this.startActivity(intent);
				}
			}
		};
		drawerList.postDelayed(r, 750);
	}

	private void processingNotificationIntent(final Intent intent) {
		if (intent == null)
			return;	
		
		if (DEBUG)
			Log.d(TAG, "processingNotificationIntent() action=" + intent.getAction());
		// this.currentPage = findChatPage(intent.getExtras());

		helper.updateActionBar(this, null);

		if (intent.getData() instanceof Uri) {
			Uri uri = intent.getData();
			if (DEBUG)
				Log.d(TAG, "onCreate(" + uri + ")");

			JID jid = JID.jidInstance(uri.getPath().substring(1));
			for (JaxmppCore jaxmpp : ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get()) {
				RosterItem ri = jaxmpp.getRoster().get(jid.getBareJid());
				if (ri != null) {					

					ChatWrapper wrapper = this.findChatWrapper(ri);
					if (wrapper == null) {
						try { jaxmpp.createChat(jid); } catch (Exception ex) {}
						wrapper = this.findChatWrapper(ri);
					}
					
					Intent newintent = new Intent(this, ChatActivity.class);
					newintent.putExtra("chatId", wrapper.getChat().getId());
					newintent.putExtra("account", jaxmpp.getSessionObject().getUserBareJid().toString());
					this.finish();
					startActivity(newintent);
					return;
				}
			}
		}

		
		final Bundle bundle = intent.getExtras();
		drawerList.post(new Runnable() {
			@Override
			public void run() {
				if (DEBUG)
					Log.d(TAG, "processing posted new intent. action=" + intent.getAction());

				if (intent.getAction() != null && MUC_MESSAGE_ACTION.equals(intent.getAction())) {
					// setCurrentPage(findChatPage(intent.getExtras()));
					MucRoomFragment.openRoom(TigaseMobileMessengerActivity.this, intent.getStringExtra("account"),
							intent.getLongExtra("roomId", 0), helper.isXLarge());
				} else if (intent.getAction() != null && NEW_CHAT_MESSAGE_ACTION.equals(intent.getAction())) {
					// setCurrentPage(findChatPage(intent.getExtras()));
					ChatHistoryFragment.openChat(TigaseMobileMessengerActivity.this, intent.getStringExtra("account"),
							intent.getLongExtra("chatId", 0), helper.isXLarge());
				} else if (intent.getAction() != null && CERT_UNTRUSTED_ACTION.equals(intent.getAction())) {
					DataCertificateException cause = (DataCertificateException) bundle.getSerializable("cause");
					String account = bundle.getString("account");
					TrustCertDialog newFragment = TrustCertDialog.newInstance(account, cause);
					newFragment.show(getSupportFragmentManager(), "dialog");
				} else if (intent.getAction() != null && MUC_ERROR_ACTION.equals(intent.getAction())) {
					showMucError(bundle);
				} else if (intent.getAction() != null && ERROR_ACTION.equals(intent.getAction())) {
					String account = bundle.getString("account");
					String message = bundle.getString("message");

					ErrorDialog newFragment = ErrorDialog.newInstance("Error", account, message);
					newFragment.show(getSupportFragmentManager(), "dialog");
				} else if (intent.getAction() != null && WARNING_ACTION.equals(intent.getAction())) {
					if (bundle.getInt("messageId", -1) != -1) {
						WarningDialog.showWarning(TigaseMobileMessengerActivity.this, bundle.getInt("messageId"));
					} else if (bundle.getString("message") != null) {
						WarningDialog.showWarning(TigaseMobileMessengerActivity.this, bundle.getString("message"));
					}

				}
			}
		});

	}

	private void showMucError(final Bundle bundle) {
		String room = "Room: " + bundle.getString("roomJid");
		String message = bundle.getString("errorMessage");
		String account = bundle.getString("account");

		ErrorDialog newFragment = ErrorDialog.newInstance("Event", account, "Room: " + room + "\n\n" + message);
		newFragment.show(getSupportFragmentManager(), "dialog");
	}

	protected void showNewsInfo() {
		Bundle b = new Bundle();

		StringBuilder sb = new StringBuilder();
		try {
			InputStream in = getResources().openRawResource(R.raw.news_html);
			int c;
			while ((c = in.read()) != -1) {
				sb.append((char) c);
			}

			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (sb.toString().length() != 0) {
			b.putString("news_html", sb.toString());
			showDialog(NEWS_DIALOG, b);
		}
	}
}
