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
import org.tigase.mobile.accountstatus.AccountsStatusFragment;
import org.tigase.mobile.authenticator.AuthenticatorActivity;
import org.tigase.mobile.bookmarks.BookmarksActivity;
import org.tigase.mobile.chat.ChatHistoryFragment;
import org.tigase.mobile.chatlist.ChatListActivity;
import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.providers.ChatHistoryProvider;
import org.tigase.mobile.db.providers.RosterProvider;
import org.tigase.mobile.filetransfer.FileTransferUtility;
import org.tigase.mobile.muc.JoinMucDialog;
import org.tigase.mobile.muc.MucRoomFragment;
import org.tigase.mobile.preferences.MessengerPreferenceActivity;
import org.tigase.mobile.roster.AccountSelectorDialogFragment;
import org.tigase.mobile.roster.ContactEditActivity;
import org.tigase.mobile.roster.RosterFragment;
import org.tigase.mobile.security.SecureTrustManagerFactory.DataCertificateException;
import org.tigase.mobile.service.JaxmppService;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.BaseEvent;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.ChatState;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.AbstractMessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

	private static final boolean DEBUG = true;

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

	private MyFragmentPageAdapter adapter;

	private final Listener<BaseEvent> chatListener;

	protected DrawerLayout drawerLayout;

	private ListView drawerList;

	protected ActionBarDrawerToggle drawerToggle;

	public final TigaseMobileMessengerActivityHelper helper;

	private SharedPreferences mPreferences;

	private final BroadcastReceiver mucErrorReceiver;

	private final OnSharedPreferenceChangeListener prefChangeListener;;

	private final RosterClickReceiver rosterClickReceiver = new RosterClickReceiver();

	public ViewPager viewPager;

	private ViewPager viewRoster;

	private int XcurrentPage = -1;

	public TigaseMobileMessengerActivity() {
		helper = TigaseMobileMessengerActivityHelper.createInstance(this);

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
					((MyFragmentPageAdapter) viewPager.getAdapter()).setRefreshRoster(true);
					viewPager.getAdapter().notifyDataSetChanged();
				}
			}
		};
	}

	protected Integer findChat(final long chatId) {
		List<ChatWrapper> l = getChatList();
		for (int i = 0; i < l.size(); i++) {
			ChatWrapper c = l.get(i);
			if (c.isChat() && c.getChat().getId() == chatId)
				return i;
		}
		return null;
	}

	protected Integer findChat(final RosterItem rosterItem) {
		List<ChatWrapper> l = getChatList();
		for (int i = 0; i < l.size(); i++) {
			ChatWrapper c = l.get(i);
			if (c.isChat() && c.getChat().getSessionObject() == rosterItem.getSessionObject()
					&& c.getChat().getJid().getBareJid().equals(rosterItem.getJid()))
				return i;
		}
		return null;
	}

	private int findChatPage(Bundle incomingExtras) {
		if (incomingExtras != null) {
			long chatId = incomingExtras.getLong("chatId", -1);
			long roomId = incomingExtras.getLong("roomId", -1);
			incomingExtras = null;
			if (DEBUG)
				Log.d(TAG, "Intent with data? chatId=" + chatId);
			if (roomId != -1) {
				final Integer idx = findRoom(roomId);
				if (idx != null) {
					int currentPage = idx + (helper.isXLarge() ? 1 : 2);
					if (DEBUG)
						Log.d(TAG, "Set current page " + currentPage);
					return currentPage;
				}
			} else if (chatId != -1) {
				final Integer idx = findChat(chatId);
				if (idx != null) {
					int currentPage = idx + (helper.isXLarge() ? 1 : 2);
					if (DEBUG)
						Log.d(TAG, "Set current page " + currentPage);
					return currentPage;
				}
			}
		}
		return -1;
	}

	protected Integer findRoom(final long chatId) {
		List<ChatWrapper> l = getChatList();
		for (int i = 0; i < l.size(); i++) {
			ChatWrapper c = l.get(i);
			if (c.isRoom() && c.getRoom().getId() == chatId)
				return i;
		}
		return null;
	}

	public ChatWrapper getChatByPageIndex(int page) {
		int x = page - (helper.isXLarge() ? 1 : 2);
		if (x < 0)
			return null;
		List<ChatWrapper> chats = getChatList();
		if (x >= chats.size())
			return null;
		return chats.get(x);
	};

	protected List<ChatWrapper> getChatList() {
		return ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().getChats();
	}

	public int getCurrentPage() {
		return XcurrentPage;
	}

	private void notifyPageChange(int msg) {
		Intent intent = new Intent();
		intent.setAction(CLIENT_FOCUS_MSG);
		intent.putExtra("page", msg);

		if (msg > 1) {
			final ChatWrapper chat = getChatByPageIndex(msg);
			if (chat != null && chat.isChat()) {
				Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/"
						+ Uri.encode(chat.getChat().getJid().getBareJid().toString()));
				ContentValues values = new ContentValues();
				values.put(ChatTableMetaData.FIELD_AUTHOR_JID, chat.getChat().getJid().getBareJid().toString());
				values.put(ChatTableMetaData.FIELD_STATE, ChatTableMetaData.STATE_INCOMING);
				getContentResolver().update(uri, values, null, null);

				intent.putExtra("chatId", chat.getChat().getId());
			} else if (chat != null && chat.isRoom())
				intent.putExtra("roomId", chat.getRoom().getId());
		}

		sendBroadcast(intent);

		helper.updateActionBar();
		helper.invalidateOptionsMenu();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (DEBUG)
			Log.d(TAG, "onActivityResult()");
		if (requestCode == REQUEST_CHAT && resultCode == Activity.RESULT_OK) {
			setCurrentPage(findChatPage(data.getExtras()));
			// Moved to ChatHistoryFragment
		} else if (requestCode == SELECT_FOR_SHARE && resultCode == Activity.RESULT_OK) {
			Uri selected = data.getData();
			String mimetype = data.getType();
			final int p = getCurrentPage();
			ChatWrapper chatW = getChatByPageIndex(p);
			Chat chat = chatW.getChat();
			if (chat == null)
				return;
			RosterItem ri = chat.getSessionObject().getRoster().get(chat.getJid().getBareJid());
			JID jid = chat.getJid();
			final Jaxmpp jaxmpp = ((MessengerApplication) TigaseMobileMessengerActivity.this.getApplicationContext()).getMultiJaxmpp().get(
					ri.getSessionObject());
			if (jid.getResource() == null) {
				jid = FileTransferUtility.getBestJidForFeatures(jaxmpp, jid.getBareJid(), FileTransferUtility.FEATURES);
			}
			if (jid != null) {
				FileTransferUtility.startFileTransfer(this, jaxmpp, chat.getJid(), selected, mimetype);
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onBackPressed() {
		if (getCurrentPage() == 0 || !helper.isXLarge() && getCurrentPage() > 1) {
			setVisiblePage(1);
		} else
			super.onBackPressed();
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

		if (savedInstanceState != null && getCurrentPage() == -1) {
			// currentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE_KEY,
			// -1);
			if (getChatByPageIndex(getCurrentPage()) == null)
				setCurrentPage(-1);
		}

		if (getCurrentPage() == -1) {
			// this should not happened as there is not valid position
			setCurrentPage(helper.isXLarge() ? 0 : 1);
		}

		if (helper.isXLarge()) {
			setContentView(R.layout.all);
		} else {
			setContentView(R.layout.roster_main);
		}

		this.drawerList = (ListView) findViewById(R.id.main_left_drawer);
		this.drawerLayout = (DrawerLayout) findViewById(R.id.roster_main);

		// creating list of items available in drawer menu
		final List<DrawerMenuItem> drawerMenuItems = new ArrayList<DrawerMenuItem>();
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

		this.viewRoster = (ViewPager) findViewById(R.id.viewRoster);
		this.viewPager = (ViewPager) findViewById(R.id.viewSwitcher);
		this.viewPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				if (DEBUG)
					Log.i(TigaseMobileMessengerActivity.TAG, "PageScrollStateChanged: " + state);
			}

			@Override
			public void onPageSelected(int position) {
				if (DEBUG)
					Log.i(TigaseMobileMessengerActivity.TAG, "PageSelected: " + position);

				setCurrentPage(position);
				notifyPageChange(position);
			}
		});

		if (viewRoster != null) {
			viewRoster.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
				@Override
				public int getCount() {
					// TODO Auto-generated method stub
					return 1;
				}

				@Override
				public Fragment getItem(int i) {
					return createRosterFragment(mPreferences.getString(Preferences.ROSTER_LAYOUT_KEY, "flat"));
				}
			});
		}

		this.adapter = new MyFragmentPageAdapter(getSupportFragmentManager()) {

			@Override
			public int getCount() {
				if (helper.isXLarge())
					return 1 + getChatList().size();
				int n = 2 + getChatList().size();
				return n;
			}

			@Override
			public Fragment getItem(int i) {
				if (DEBUG)
					Log.i(TAG, "FragmentPagerAdapter.getItem(" + i + ")");
				if (i == 0) {
					return AccountsStatusFragment.newInstance();
				} else if (!helper.isXLarge() && i == 1) {
					Fragment f = createRosterFragment(mPreferences.getString(Preferences.ROSTER_LAYOUT_KEY, "flat"));
					if (DEBUG)
						Log.d(TAG, "Created roster with FragmentManager " + f.getFragmentManager());
					return f;

				} else {
					int idx = i - (!helper.isXLarge() ? 2 : 1);
					final ChatWrapper wrapper = getChatList().get(idx);
					if (wrapper.isChat()) {
						return ChatHistoryFragment.newInstance(
								wrapper.getChat().getSessionObject().getUserBareJid().toString(), wrapper.getChat().getId());
					} else {
						Room room = wrapper.getRoom();
						Fragment fr = MucRoomFragment.newInstance(room.getSessionObject().getUserBareJid().toString(),
								room.getId());
						return fr;
					}
				}
			}

			@Override
			public int getItemPosition(Object object) {
				if (object instanceof AccountsStatusFragment) {
					return 0;
				} else if (refreshRoster && object instanceof RosterFragment) {
					return POSITION_NONE;
				} else if (object instanceof RosterFragment) {
					return 1;
				} else if (object instanceof MucRoomFragment) {
					Log.v(TAG, "got chat history fragment");
					Room room = ((MucRoomFragment) object).getRoom();
					if (room != null) {
						Integer position = findRoom(room.getId());
						if (position != null) {
							if (helper.isXLarge())
								return 1 + position;
							return 2 + position;
						}
					}
					return POSITION_NONE;
				} else if (object instanceof ChatHistoryFragment) {
					Log.v(TAG, "got chat history fragment");
					Chat chat = ((ChatHistoryFragment) object).getChat();
					if (chat != null) {
						Integer position = findChat(chat.getId());
						if (position != null) {
							if (helper.isXLarge())
								return 1 + position;
							return 2 + position;
						}
					}
					return POSITION_NONE;
				} else {
					return POSITION_NONE;
				}
			}

			@Override
			protected String makeFragmentName(int viewId, int index) {
				if (index == 0) {
					return "accounts";
				} else if (!helper.isXLarge() && index == 1) {
					return "roster";
				} else {
					int pos = index - (helper.isXLarge() ? 1 : 2);
					if (pos < getChatList().size() && pos > -1) {
						return getChatList().get(pos).toString();
					} else {
						return null;
					}
				}
			}

		};

		viewPager.setAdapter(this.adapter);

		if (getIntent().getData() instanceof Uri) {
			Uri uri = getIntent().getData();
			if (DEBUG)
				Log.d(TAG, "onCreate(" + uri + ")");

			JID jid = JID.jidInstance(uri.getPath().substring(1));
			for (JaxmppCore jaxmpp : ((MessengerApplication) getApplicationContext()).getMultiJaxmpp().get()) {
				RosterItem ri = jaxmpp.getRoster().get(jid.getBareJid());
				if (ri != null) {
					openChatWith(ri, jid.getResource());
				}
			}
		}

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

	// @Override
	// protected void onActivityResult(int requestCode, int resultCode, Intent
	// data) {
	// super.onActivityResult(requestCode, resultCode, data);
	//
	// if(DEBUG)Log.i(TAG, "Sprawdzamy extrasy...");
	// if (getIntent() != null && getIntent().getExtras() != null) {
	// if(DEBUG)Log.i(TAG, "Mamy extrasy");
	// if (getIntent().getExtras().containsKey("chatId")) {
	// long chatId = getIntent().getLongExtra("chatId", -1);
	// getIntent().removeExtra("chatId");
	//
	// List<Chat> l = getChatList();
	// for (int i = 0; i < l.size(); i++) {
	// Chat chh = l.get(i);
	// if (chh.getId() == chatId) {
	// final int x = i + 1;
	// viewSwitcher.post(new Runnable() {
	//
	// @Override
	// public void run() {
	// if(DEBUG)Log.i(TAG, "Switch chats to " + x);
	//
	// viewSwitcher.setCurrentItem(x);
	// }
	// });
	//
	// }
	// }
	// }
	//
	// }
	// }

	@Override
	protected void onDestroy() {
		unregisterReceiver(mucErrorReceiver);

		this.mPreferences.unregisterOnSharedPreferenceChangeListener(prefChangeListener);
		super.onDestroy();
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (DEBUG)
			Log.d(TAG, "onDetachedFromWindow()");
	}

	protected void onMessageEvent(final AbstractMessageEvent be) {
		/*
		 * Runnable action = new Runnable() {
		 * 
		 * @Override public void run() {
		 */
		if (be.getType() == MessageModule.ChatCreated) {
			viewPager.getAdapter().notifyDataSetChanged();
		} else if (be.getType() == MessageModule.ChatClosed) {
			final Chat chat = ((MessageEvent) be).getChat();
			viewPager.getAdapter().notifyDataSetChanged();
			new Thread() {
				public void run() {
					try {
						chat.setLocalChatState(ChatState.gone);
					}
					catch (JaxmppException ex) {
						// we ignore this exception as sending gone state may be omitted
					}						
				}
			}.start();
		} else if (be.getType() == MucModule.RoomClosed) {
			viewPager.getAdapter().notifyDataSetChanged();
		} else if (be.getType() == MucModule.JoinRequested) {
			viewPager.getAdapter().notifyDataSetChanged();
		} else if (be.getType() == MessageModule.ChatStateChanged) {
			helper.updateActionBar(((MessageEvent) be).getChat().hashCode());
			return;
		}
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
		/*
		 * } };
		 * 
		 * viewPager.post(action);
		 */
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

					viewPager.post(new Runnable() {

						@Override
						public void run() {
							final Integer idx = findRoom(roomId);
							// this should be done on receiving RoomJoined event
							viewPager.getAdapter().notifyDataSetChanged();
							setVisiblePage(idx + (helper.isXLarge() ? 1 : 2));
						}
					});
					return null;
				}
			};
			newFragment.setAsyncTask(r);
			newFragment.show(getSupportFragmentManager(), "dialog");
			return true;
		} else if (id == android.R.id.home) {
			setVisiblePage(1);
			return true;
		} else if (id == R.id.showHideOffline) {
			boolean x = mPreferences.getBoolean(Preferences.SHOW_OFFLINE, Boolean.TRUE);
			Editor editor = mPreferences.edit();
			editor.putBoolean(Preferences.SHOW_OFFLINE, !x);
			editor.commit();
			Uri insertedItem = Uri.parse(RosterProvider.CONTENT_URI);
			getApplicationContext().getContentResolver().notifyChange(insertedItem, null);
			return true;
			// insertedItem =
			// Uri.parse("content://org.tigase.mobile.db.providers.RosterProvider");
			// getApplicationContext().getContentResolver().notifyChange(insertedItem,
			// null);
		} else if (id == R.id.contactAdd) {
			AccountManager accountManager = AccountManager.get(this);
			final Account[] accounts = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);

			if (accounts != null && accounts.length > 1) {
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				Fragment prev = getSupportFragmentManager().findFragmentByTag("account:select:dialog");
				if (prev != null) {
					ft.remove(prev);
				}
				ft.addToBackStack(null);
				AccountSelectorDialogFragment newFragment = AccountSelectorDialogFragment.newInstance();
				newFragment.show(ft, "account:select:dialog");
				return true;
			} else if (accounts != null && accounts.length == 1) {
				Intent intent = new Intent(this, ContactEditActivity.class);
				intent.putExtra("account", accounts[0].name);
				startActivityForResult(intent, 0);
				return true;
			}
		} else if (id == R.id.aboutButton) {
			showDialog(ABOUT_DIALOG);
			return true;
		} else if (id == R.id.propertiesButton) {
			Intent intent = new Intent().setClass(this, MessengerPreferenceActivity.class);
			this.startActivityForResult(intent, 0);
			return true;
		} else if (id == R.id.disconnectButton) {
			mPreferences.edit().putBoolean(Preferences.SERVICE_ACTIVATED, false).commit();
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
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if ((getCurrentPage() == 1 || helper.isXLarge()) && drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		return onOptionsItemSelected(item.getItemId());
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (getCurrentPage() == 0 || getCurrentPage() == 1 || helper.isXLarge()) {
			MenuInflater inflater = getMenuInflater();
			Log.v(TAG, "current page " + getCurrentPage());
			Log.v(TAG, "xlarge = " + helper.isXLarge());
			final boolean serviceActive = JaxmppService.isServiceActive();

			menu.clear();
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				inflater.inflate(R.menu.main_menu_old, menu);
			} else {
				inflater.inflate(R.menu.main_menu, menu);
			}

			MenuItem con = menu.findItem(R.id.connectButton);
			con.setVisible(!serviceActive);

			MenuItem dcon = menu.findItem(R.id.disconnectButton);
			dcon.setVisible(serviceActive);

			MenuItem showOffline = menu.findItem(R.id.showHideOffline);
			showOffline.setCheckable(true);
			showOffline.setChecked(mPreferences.getBoolean(Preferences.SHOW_OFFLINE, Boolean.TRUE));

			MenuItem add = menu.findItem(R.id.contactAdd);
			helper.setShowAsAction(add, MenuItem.SHOW_AS_ACTION_IF_ROOM);
			add.setVisible(serviceActive);

			MenuItem bookmarks = menu.findItem(R.id.bookmarksShow);
			if (bookmarks != null) {
				bookmarks.setVisible(serviceActive);
			}
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		setCurrentPage(savedInstanceState.getInt(STATE_CURRENT_PAGE, -1));
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (DEBUG)
			Log.d(TAG, "onResume()");

		registerReceiver(rosterClickReceiver, new IntentFilter(ROSTER_CLICK_MSG));

		viewPager.getAdapter().notifyDataSetChanged();

		final MultiJaxmpp multi = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();

		multi.addListener(this.chatListener);

		viewPager.post(new Runnable() {

			@Override
			public void run() {
				if (DEBUG)
					Log.d(TAG, "Focus on page " + getCurrentPage());
				setVisiblePage(getCurrentPage());
				notifyPageChange(getCurrentPage());
			}
		});

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_CURRENT_PAGE, getCurrentPage());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (DEBUG)
			Log.d(TAG, "onStop()");
	}

	protected void openChatWith(final RosterItem rosterItem, final String resource) {
		Runnable r = new Runnable() {

			@Override
			public void run() {

				try {
					Integer idx = findChat(rosterItem);

					if (DEBUG)
						Log.i(TAG, "Opening new chat with " + rosterItem + ". idx=" + idx);

					if (idx == null) {
						final Jaxmpp jaxmpp = ((MessengerApplication) TigaseMobileMessengerActivity.this.getApplicationContext()).getMultiJaxmpp().get(
								rosterItem.getSessionObject());
						jaxmpp.createChat(JID.jidInstance(rosterItem.getJid(), resource));
						final int i = getChatList().size() + (helper.isXLarge() ? 1 : 2);
						// this will be done when we receive ChatCreated event
						// viewPager.getAdapter().notifyDataSetChanged();
						setVisiblePage(i);

					} else {
						setVisiblePage(idx + (helper.isXLarge() ? 1 : 2));
					}

				} catch (JaxmppException e) {
					throw new RuntimeException(e);
				}

			}
		};
		viewPager.postDelayed(r, 750);
	}

	private void processingNotificationIntent(final Intent intent) {
		if (intent == null)
			return;

		if (DEBUG)
			Log.d(TAG, "processingNotificationIntent() action=" + intent.getAction());
		// this.currentPage = findChatPage(intent.getExtras());

		helper.updateActionBar();

		final Bundle bundle = intent.getExtras();
		viewPager.post(new Runnable() {
			@Override
			public void run() {
				if (DEBUG)
					Log.d(TAG, "processing posted new intent. action=" + intent.getAction());

				if (intent.getAction() != null && MUC_MESSAGE_ACTION.equals(intent.getAction())) {
					setCurrentPage(findChatPage(intent.getExtras()));
				} else if (intent.getAction() != null && NEW_CHAT_MESSAGE_ACTION.equals(intent.getAction())) {
					setCurrentPage(findChatPage(intent.getExtras()));
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

	private void setCurrentPage(int page) {
		this.XcurrentPage = page;
	}

	private void setVisiblePage(int i) {
		if (DEBUG)
			Log.d(TAG, "Set visible page to " + i);
		if (viewPager.getChildCount() != i)
			viewPager.getAdapter().notifyDataSetChanged();
		viewPager.setCurrentItem(i);
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
