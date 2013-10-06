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
package org.tigase.mobile.service;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocketFactory;

import org.tigase.mobile.Constants;
import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.MultiJaxmpp.ChatWrapper;
import org.tigase.mobile.Preferences;
import org.tigase.mobile.R;
import org.tigase.mobile.TigaseMobileMessengerActivity;
import org.tigase.mobile.authenticator.AuthenticatorActivity;
import org.tigase.mobile.db.AccountsTableMetaData;
import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.VCardsCacheTableMetaData;
import org.tigase.mobile.db.providers.CapabilitiesDBCache;
import org.tigase.mobile.db.providers.ChatHistoryProvider;
import org.tigase.mobile.db.providers.RosterProvider;
//import org.tigase.mobile.filetransfer.AndroidFileTransferUtility;
//import org.tigase.mobile.filetransfer.FileTransfer;
//import org.tigase.mobile.filetransfer.FileTransferModule;
//import org.tigase.mobile.filetransfer.FileTransferProgressEvent;
//import org.tigase.mobile.filetransfer.FileTransferRequestEvent;
//import org.tigase.mobile.filetransfer.StreamhostsEvent;
import org.tigase.mobile.security.SecureTrustManagerFactory;
import org.tigase.mobile.sync.SyncAdapter;
import org.tigase.mobile.ui.NotificationHelper;
import org.tigase.mobile.utils.AvatarHelper;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Base64;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.JaxmppCore.JaxmppEvent;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.SessionObject.Scope;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.connector.StreamError;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.SoftwareVersionModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule.AuthEvent;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule.SaslEvent;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageCarbonsModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageCarbonsModule.MessageCarbonEvent;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageCarbonsModule.MessageReceivedCarbonEvent;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageCarbonsModule.MessageSentCarbonEvent;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule.DiscoInfoEvent;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule.MucEvent;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule.RosterEvent;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCard;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule.VCardAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.stanzas.ErrorElement;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import tigase.jaxmpp.j2se.J2SESessionObject;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class JaxmppService extends Service {

	private class AccountModifyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			updateJaxmppInstances(getMulti(), getContentResolver(), getResources(), getApplicationContext());
			for (JaxmppCore j : getMulti().get()) {
				State st = getState(j.getSessionObject());
				if (st == State.disconnected || st == null) {
					connectJaxmpp((Jaxmpp) j, (Long) null);
				}
			}
		}
	}

	private class ClientFocusReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			final int page = intent.getIntExtra("page", -1);

			onPageChanged(page);

			final long chatId = intent.getLongExtra("chatId", -1);
			final long roomId = intent.getLongExtra("roomId", -1);

			if (chatId != -1) {
				currentChatIdFocus = chatId;
				currentRoomIdFocus = -1;
				notificationHelper.cancelChatNotification("chatId:" + chatId);
			} else if (roomId != -1) {
				currentChatIdFocus = -1;
				currentRoomIdFocus = roomId;
				notificationHelper.cancelChatNotification("roomId:" + roomId);
			} else {
				currentChatIdFocus = -1;
				currentRoomIdFocus = -1;
			}
		}
	}

	private class ConnReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// EXTRA_NETWORK_INFO - This constant is deprecated
			// NetworkInfo netInfo = (NetworkInfo)
			// intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

			NetworkInfo netInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
			onNetworkChanged(netInfo);
		}

	}

	private class ScreenStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Boolean screenOff = null;
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				screenOff = true;
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				screenOff = false;
			}

			if (screenOff != null) {
				mobileModeFeature.setMobileMode(screenOff);
			}
		}
	}

	public static final String ACTION_FILETRANSFER = "org.tigase.mobile.service.JaxmppService.ACTION_FILETRANSFER";

	private static final String ACTION_KEEPALIVE = "org.tigase.mobile.service.JaxmppService.KEEP_ALIVE";

	private static final boolean DEBUG = false;

	private static Executor executor = new StanzaExecutor();

	protected static boolean focused;

	private final static Set<SessionObject> locked = new HashSet<SessionObject>();

	public static final String MUC_ERROR_MSG = "org.tigase.mobile.MUC_ERROR_MSG";

	private static boolean serviceActive = false;

	private static final String TAG = "JaxmppService";

	protected static String userStatusMessage = null;

	protected static Show userStatusShow = Show.online;

	private static Date calculateNextRestart(final int delayInSecs, final int errorCounter) {
		long timeInSecs = delayInSecs;
		if (errorCounter > 20) {
			timeInSecs += 60 * 5;
		} else if (errorCounter > 10) {
			timeInSecs += 120;
		} else if (errorCounter > 5) {
			timeInSecs += 60;
		}

		Date d = new Date((new Date()).getTime() + 1000 * timeInSecs);
		return d;
	}

	public static void disable(SessionObject jaxmpp, boolean disabled) {
		if (DEBUG)
			Log.d(TAG, "Account " + jaxmpp.getUserBareJid() + " disabled=" + disabled);
		jaxmpp.setProperty("CC:DISABLED", disabled);
	}

	private static Throwable extractCauseException(Throwable ex) {
		Throwable th = ex.getCause();
		if (th == null)
			return ex;

		for (int i = 0; i < 4; i++) {
			if (!(th instanceof JaxmppException))
				return th;
			if (th.getCause() == null)
				return th;
			th = th.getCause();
		}
		return ex;
	}

	public static boolean isDisabled(SessionObject jaxmpp) {
		Boolean x = jaxmpp.getProperty("CC:DISABLED");
		return x == null ? false : x;
	}

	private static boolean isLocked(SessionObject jaxmpp) {
		synchronized (locked) {
			return locked.contains(jaxmpp);
		}
	}

	public static boolean isServiceActive() {
		return serviceActive;
	}

	private static void lock(SessionObject jaxmpp, boolean value) {
		synchronized (locked) {
			if (DEBUG)
				Log.d(TAG, "Account " + jaxmpp.getUserBareJid() + " locked=" + value);

			if (value)
				locked.add(jaxmpp);
			else
				locked.remove(jaxmpp);
			// jaxmpp.setProperty("CC:LOCKED", locked);
		}
	}

	// added to fix Eclipse error
	public static void updateJaxmppInstances(MultiJaxmpp multi, ContentResolver contentResolver, Resources resources,
			Context context) {
		final HashSet<BareJID> accountsJids = new HashSet<BareJID>();
		for (JaxmppCore jc : multi.get()) {
			accountsJids.add(jc.getSessionObject().getUserBareJid());
		}

		AccountManager accountManager = AccountManager.get(context);
		for (Account account : accountManager.getAccountsByType(Constants.ACCOUNT_TYPE)) {
			BareJID jid = BareJID.bareJIDInstance(account.name);
			String password = accountManager.getPassword(account);
			String nickname = accountManager.getUserData(account, AccountsTableMetaData.FIELD_NICKNAME);
			String hostname = accountManager.getUserData(account, AccountsTableMetaData.FIELD_HOSTNAME);
			String resource = accountManager.getUserData(account, AccountsTableMetaData.FIELD_RESOURCE);
			hostname = hostname == null ? null : hostname.trim();

			if (!accountsJids.contains(jid)) {
				SessionObject sessionObject = new J2SESessionObject();
				// sessionObject.setUserProperty(SocketConnector.COMPRESSION_DISABLED_KEY,
				// Boolean.TRUE);
				sessionObject.setUserProperty(Connector.TRUST_MANAGERS_KEY, SecureTrustManagerFactory.getTrustManagers());
				sessionObject.setUserProperty(SoftwareVersionModule.VERSION_KEY, resources.getString(R.string.app_version));
				sessionObject.setUserProperty(SoftwareVersionModule.NAME_KEY, resources.getString(R.string.app_name));
				sessionObject.setUserProperty(SoftwareVersionModule.OS_KEY, "Android " + android.os.Build.VERSION.RELEASE);

				sessionObject.setUserProperty(DiscoInfoModule.IDENTITY_CATEGORY_KEY, "client");
				sessionObject.setUserProperty(DiscoInfoModule.IDENTITY_TYPE_KEY, "phone");
				sessionObject.setUserProperty(CapabilitiesModule.NODE_NAME_KEY, "http://tigase.org/messenger");

				sessionObject.setUserProperty("ID", (long) account.hashCode());
				sessionObject.setUserProperty(SocketConnector.SERVER_PORT, 5222);
				sessionObject.setUserProperty(tigase.jaxmpp.j2se.Jaxmpp.CONNECTOR_TYPE, "socket");
				sessionObject.setUserProperty(Connector.EXTERNAL_KEEPALIVE_KEY, true);
				// sessionObject.setUserProperty(Connector.DISABLE_SOCKET_TIMEOUT_KEY,
				// true);

				// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
				// Android from API v8 contains optimized SSLSocketFactory
				// which reduces network usage for handshake
				SSLSessionCache sslSessionCache = new SSLSessionCache(context);
				SSLSocketFactory sslSocketFactory = SSLCertificateSocketFactory.getDefault(0, sslSessionCache);
				sessionObject.setUserProperty(SocketConnector.SSL_SOCKET_FACTORY_KEY, sslSocketFactory);
				// }

				sessionObject.setUserProperty(SessionObject.USER_BARE_JID, jid);
				sessionObject.setUserProperty(SessionObject.PASSWORD, password);
				sessionObject.setUserProperty(SessionObject.NICKNAME, nickname);
				if (hostname != null && hostname.trim().length() > 0)
					sessionObject.setUserProperty(SocketConnector.SERVER_HOST, hostname);
				else
					sessionObject.setUserProperty(SocketConnector.SERVER_HOST, null);

				if (!TextUtils.isEmpty(resource))
					sessionObject.setUserProperty(SessionObject.RESOURCE, resource);
				else
					sessionObject.setUserProperty(SessionObject.RESOURCE, null);

				sessionObject.setUserProperty(JaxmppCore.AUTOADD_STANZA_ID_KEY, Boolean.TRUE);

				final Jaxmpp jaxmpp = new Jaxmpp(sessionObject); /*
																 * {
																 * 
																 * @Override
																 * public void
																 * modulesInit()
																 * {
																 * super.modulesInit
																 * ();
																 * getModulesManager
																 * (
																 * ).register(new
																 * FileTransferModule
																 * (observable,
																 * sessionObject
																 * , writer)); }
																 * };
																 */
				jaxmpp.setExecutor(executor);
				CapabilitiesModule capabilitiesModule = jaxmpp.getModule(CapabilitiesModule.class);
				if (capabilitiesModule != null) {
					capabilitiesModule.setCache(new CapabilitiesDBCache(context));
				}

				multi.add(jaxmpp);
			} else {
				SessionObject sessionObject = multi.get(jid).getSessionObject();

				sessionObject.setUserProperty(SessionObject.PASSWORD, password);
				sessionObject.setUserProperty(SessionObject.NICKNAME, nickname);
				if (hostname != null && hostname.trim().length() > 0)
					sessionObject.setUserProperty(SocketConnector.SERVER_HOST, hostname);
				else
					sessionObject.setUserProperty(SocketConnector.SERVER_HOST, null);

				if (!TextUtils.isEmpty(resource))
					sessionObject.setUserProperty(SessionObject.RESOURCE, resource);
				else
					sessionObject.setUserProperty(SessionObject.RESOURCE, null);

				// sessionObject.setUserProperty(JaxmppService.GEOLOCATION_LISTEN_ENABLED,
				// geolocationListen);
				// sessionObject.setUserProperty(JaxmppService.GEOLOCATION_PUBLISH_ENABLED,
				// geolocationPublish);
				// sessionObject.setUserProperty(JaxmppService.GEOLOCATION_PUBLISH_PRECISION,
				// geolocationPrecision);
			}

			Jaxmpp jaxmpp = multi.get(jid);
			if (jaxmpp != null) {
				FileTransferFeature.enableFileTransfer(jaxmpp, context);
				MobileModeFeature.updateSettings(account, jaxmpp, context);
				GeolocationFeature.updateGeolocationSettings(account, jaxmpp, context);
			}

			accountsJids.remove(jid);
		}

		final MessengerApplication app = (MessengerApplication) context.getApplicationContext();
		for (BareJID jid : accountsJids) {
			final JaxmppCore jaxmpp = multi.get(jid);
			if (jaxmpp != null) {
				multi.remove(jaxmpp);

				(new Thread() {
					@Override
					public void run() {
						try {
							jaxmpp.disconnect();
							app.clearPresences(jaxmpp.getSessionObject(), false);
						} catch (Exception e) {
							Log.e(TAG, "Can't disconnect", e);
						}
					}
				}).start();
			}
		}
	}

	private AccountModifyReceiver accountModifyReceiver;

	private TimerTask autoPresenceTask;

	private final HashMap<BareJID, Integer> connectionErrorsCounter = new HashMap<BareJID, Integer>();

	private Listener<ConnectorEvent> connectorListener;

	private ConnectivityManager connManager;

	public long currentChatIdFocus = -1;

	private long currentRoomIdFocus = -1;

	/*
	 * private final Listener<FileTransferProgressEvent>
	 * fileTransferProgressListener;
	 * 
	 * private final Listener<FileTransferRequestEvent>
	 * fileTransferRequestListener;
	 * 
	 * private final Listener<StreamhostsEvent> fileTransferStreamhostsListener;
	 */

	private FileTransferFeature fileTransferFeature;

	private ClientFocusReceiver focusChangeReceiver;

	private final Listener<MessageCarbonEvent> forwardedMessageListener;

	private final GeolocationFeature geolocationFeature;

	private Listener<AuthEvent> invalidAuthListener;

	private final Listener<JaxmppEvent> jaxmppConnected;

	private long keepaliveInterval = 3 * 60 * 1000;

	private final Listener<MessageModule.MessageEvent> messageListener;

	// private NotificationManager notificationManager;

	private final MobileModeFeature mobileModeFeature;

	private Listener<MucEvent> mucListener;

	private ConnReceiver myConnReceiver;

	private ScreenStateReceiver myScreenStateReceiver;

	protected NotificationHelper notificationHelper;

	private NotificationManager notificationManager;

	private OnSharedPreferenceChangeListener prefChangeListener;

	private SharedPreferences prefs;

	private final Listener<PresenceModule.PresenceEvent> presenceListener;

	private final Listener<PresenceEvent> presenceSendListener;

	private boolean reconnect = true;

	private final Listener<RosterModule.RosterEvent> rosterListener;

	private final Listener<DiscoInfoEvent> serverFeaturesListener;

	private final Listener<Connector.ConnectorEvent> stateChangeListener;

	private Listener<PresenceEvent> subscribeRequestListener;

	protected final Timer timer = new Timer();

	private int usedNetworkType = -1;

	public JaxmppService() {
		super();
		Logger logger = Logger.getLogger("tigase.jaxmpp");
		// // create a ConsoleHandler
		// Handler handler = new ConsoleHandler();
		// handler.setLevel(Level.ALL);
		// logger.addHandler(handler);
		logger.setLevel(Level.INFO);

		if (DEBUG)
			Log.i(TAG, "creating");

		this.serverFeaturesListener = new Listener<DiscoInfoModule.DiscoInfoEvent>() {

			@Override
			public void handleEvent(DiscoInfoEvent be) throws JaxmppException {
				final Set<String> sfeatures = be.getSessionObject().getProperty(DiscoInfoModule.SERVER_FEATURES_KEY);
				if (sfeatures != null && sfeatures.contains(MessageCarbonsModule.XMLNS_MC)) {
					enableMessageCarbons(be.getSessionObject());
				}

			}
		};

		this.presenceSendListener = new Listener<PresenceModule.PresenceEvent>() {

			@Override
			public void handleEvent(PresenceEvent be) throws JaxmppException {
				be.setStatus(userStatusMessage);
				if (focused) {
					be.setShow(userStatusShow);
					be.setPriority(prefs.getInt(Preferences.DEFAULT_PRIORITY_KEY, 5));
				} else {
					be.setShow(Show.away);
					be.setStatus("Auto away");
					be.setPriority(prefs.getInt(Preferences.AWAY_PRIORITY_KEY, 0));
				}
			}
		};

		this.forwardedMessageListener = new Listener<MessageCarbonsModule.MessageCarbonEvent>() {

			@Override
			public void handleEvent(final MessageCarbonEvent be) throws JaxmppException {
				if (be instanceof MessageReceivedCarbonEvent) {
					storeIncomingMessage(be, false);
				} else if (be instanceof MessageSentCarbonEvent) {
					storeOutgoingMessage(be);
				}
			}
		};
		this.messageListener = new Listener<MessageModule.MessageEvent>() {

			@Override
			public void handleEvent(MessageEvent be) throws JaxmppException {
				storeIncomingMessage(be, true);
			}
		};
		this.mucListener = new Listener<MucModule.MucEvent>() {

			@Override
			public void handleEvent(MucModule.MucEvent be) throws JaxmppException {
				if (be.getType() == MucModule.OccupantLeaved) {
					onMucOccupantLeave(be);
				} else if (be.getType() == MucModule.PresenceError) {
					onMucPresenceError(be);
				} else if (be.getType() == MucModule.MucMessageReceived && be.getRoom() != null && be.getMessage() != null
						&& be.getMessage().getBody() != null) {

					String msg = be.getMessage().getBody();

					Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + be.getRoom().getRoomJid().toString());

					ContentValues values = new ContentValues();
					values.put(ChatTableMetaData.FIELD_JID, be.getRoom().getRoomJid().toString());
					values.put(ChatTableMetaData.FIELD_AUTHOR_NICKNAME, be.getNickname());
					values.put(ChatTableMetaData.FIELD_TIMESTAMP, be.getDate().getTime());
					values.put(ChatTableMetaData.FIELD_BODY, msg);
					values.put(ChatTableMetaData.FIELD_STATE, 0);
					values.put(ChatTableMetaData.FIELD_ACCOUNT, be.getSessionObject().getUserBareJid().toString());

					getContentResolver().insert(uri, values);

					showMucMessageNotification(be);
				}
			}
		};
		this.presenceListener = new Listener<PresenceModule.PresenceEvent>() {

			@Override
			public void handleEvent(PresenceEvent be) throws JaxmppException {
				updateRosterItem(be);
			}
		};
		this.subscribeRequestListener = new Listener<PresenceModule.PresenceEvent>() {

			@Override
			public void handleEvent(PresenceEvent be) throws JaxmppException {
				onSubscribeRequest(be);
			}
		};

		this.rosterListener = new Listener<RosterModule.RosterEvent>() {

			@Override
			public synchronized void handleEvent(final RosterEvent be) throws JaxmppException {
				if (be.getType() == RosterModule.ItemAdded)
					changeRosterItem(be);
				else if (be.getType() == RosterModule.ItemUpdated)
					changeRosterItem(be);
				else if (be.getType() == RosterModule.ItemRemoved)
					changeRosterItem(be);
			}
		};

		this.invalidAuthListener = new Listener<AuthModule.AuthEvent>() {

			@Override
			public void handleEvent(AuthEvent be) throws JaxmppException {

				String msg;
				if (be instanceof SaslEvent && ((SaslEvent) be).getError() != null) {
					msg = getResources().getString(R.string.service_invalid_jid_or_password_info, ((SaslEvent) be).getError());
				} else {
					msg = getResources().getString(R.string.service_invalid_jid_or_password);

				}

				notificationUpdateFail(be.getSessionObject(), msg,
						getResources().getString(R.string.service_invalid_password, be.getSessionObject().getUserBareJid()),
						null);
				disable(be.getSessionObject(), true);
			}
		};

		this.stateChangeListener = new Listener<Connector.ConnectorEvent>() {

			@Override
			public void handleEvent(final Connector.ConnectorEvent be) throws JaxmppException {
				State st = getState(be.getSessionObject());
				if (DEBUG)
					Log.d(TAG, "New connection state for " + be.getSessionObject().getUserBareJid() + ": " + st);

				if (st == State.connected)
					setConnectionError(be.getSessionObject().getUserBareJid(), 0);
				if (st == State.disconnected)
					reconnectIfAvailable(be.getSessionObject());
				notificationUpdate();
			}
		};

		this.connectorListener = new Listener<Connector.ConnectorEvent>() {

			@Override
			public void handleEvent(ConnectorEvent be) throws JaxmppException {
				if (be.getType() == Connector.Error) {
					if (DEBUG)
						Log.d(TAG, "Connection error (" + be.getSessionObject().getUserBareJid() + ") " + be.getCaught() + "  "
								+ be.getStreamError());

					onConnectorError(be);
				} else if (be.getType() == Connector.StreamTerminated) {
					if (DEBUG)
						Log.d(TAG, "Stream terminated (" + be.getSessionObject().getUserBareJid() + ") " + be.getStreamError());
				}
			}
		};

		this.jaxmppConnected = new Listener<JaxmppEvent>() {

			@Override
			public void handleEvent(JaxmppEvent be) throws JaxmppException {
				sendUnsentMessages();
				JaxmppCore jaxmpp = getMulti().get(be.getSessionObject());

				// is it good place to change availability of server features?
				AccountManager accountManager = AccountManager.get(getApplicationContext());
				String jidStr = jaxmpp.getSessionObject().getUserBareJid().toString();
				for (Account acc : accountManager.getAccountsByType(Constants.ACCOUNT_TYPE)) {
					if (jidStr.equals(acc.name)) {
						Account account = acc;
						Map<String, String> data = new HashMap<String, String>();
						AuthenticatorActivity.processJaxmppForFeatures(jaxmpp, data);
						for (String key : data.keySet()) {
							String value = data.get(key);
							accountManager.setUserData(account, key, value);
						}
						break;
					}
				}

				mobileModeFeature.accountConnected(jaxmpp);

				GeolocationFeature.sendCurrentLocation(jaxmpp, JaxmppService.this);
				notificationUpdate();
				rejoinToRooms(be.getSessionObject());
			}
		};

		/*
		 * this.fileTransferProgressListener = new
		 * Listener<FileTransferProgressEvent>() {
		 * 
		 * @Override public void handleEvent(FileTransferProgressEvent be)
		 * throws JaxmppException {
		 * 
		 * FileTransfer ft = be.getFileTransfer(); if (ft != null) {
		 * notificationHelper.notifyFileTransferProgress(ft); } }
		 * 
		 * };
		 */

		/*
		 * this.fileTransferRequestListener = new
		 * Listener<FileTransferRequestEvent>() {
		 * 
		 * @Override public void handleEvent(FileTransferRequestEvent be) throws
		 * JaxmppException { // if there is no stream-method supported by us we
		 * return error if (be.getStreamMethods() == null ||
		 * !be.getStreamMethods().contains(Features.BYTESTREAMS)) {
		 * FileTransferModule ftModule =
		 * getMulti().get(be.getSessionObject()).getModule
		 * (FileTransferModule.class); ftModule.sendNoValidStreams(be); return;
		 * }
		 * 
		 * notificationHelper.notifyFileTransferRequest(be); }
		 * 
		 * };
		 */

		/*
		 * this.fileTransferStreamhostsListener = new
		 * Listener<StreamhostsEvent>() {
		 * 
		 * @Override public void handleEvent(StreamhostsEvent be) throws
		 * JaxmppException { Jaxmpp jaxmpp =
		 * getMulti().get(be.getSessionObject());
		 * AndroidFileTransferUtility.fileTransferHostsEventReceived(jaxmpp,
		 * be); }
		 * 
		 * };
		 */

		this.mobileModeFeature = new MobileModeFeature(this);
		this.geolocationFeature = new GeolocationFeature(this);
		this.fileTransferFeature = new FileTransferFeature(this);

	}

	protected synchronized void changeRosterItem(RosterEvent be) {
		Uri insertedItem = ContentUris.withAppendedId(Uri.parse(RosterProvider.CONTENT_URI), be.getItem().getId());
		getApplicationContext().getContentResolver().notifyChange(insertedItem, null);

		if (be.getChangedGroups() != null && !be.getChangedGroups().isEmpty()) {
			for (String gr : be.getChangedGroups()) {

				Uri x = ContentUris.withAppendedId(Uri.parse(RosterProvider.GROUP_URI), gr.hashCode());
				if (DEBUG)
					Log.d(TAG, "Group changed: " + gr + ". Sending notification for " + x);
				getApplicationContext().getContentResolver().notifyChange(x, null, true);
			}
		}

	}

	private void clearLocalJaxmppProperties() {
		for (JaxmppCore jaxmpp : getMulti().get()) {
			lock(jaxmpp.getSessionObject(), false);
			disable(jaxmpp.getSessionObject(), false);
			try {
				jaxmpp.getSessionObject().clear(Scope.stream, Scope.session);
			} catch (JaxmppException e) {
			}
		}
	}

	private void connectAllJaxmpp(Long delay) {
		if (DEBUG)
			Log.d(TAG, "Starting all JAXMPPs");

		for (final JaxmppCore j : getMulti().get()) {
			connectJaxmpp((Jaxmpp) j, delay);
		}

	}

	private void connectJaxmpp(final Jaxmpp jaxmpp, final Date delay) {
		if (DEBUG)
			Log.d(TAG, "Preparing to start account " + jaxmpp.getSessionObject().getUserBareJid());

		if (isLocked(jaxmpp.getSessionObject())) {
			if (DEBUG)
				Log.d(TAG, "Skip connection for account " + jaxmpp.getSessionObject().getUserBareJid() + ". Locked.");
			return;
		}

		final Runnable r = new Runnable() {

			@Override
			public void run() {
				if (isDisabled(jaxmpp.getSessionObject())) {
					if (DEBUG)
						Log.d(TAG, "Account" + jaxmpp.getSessionObject().getUserBareJid() + " disabled. Connection skipped.");
					return;
				}
				if (DEBUG)
					Log.d(TAG, "Start connection for account " + jaxmpp.getSessionObject().getUserBareJid());
				lock(jaxmpp.getSessionObject(), false);
				setUsedNetworkType(getActiveNetworkConnectionType());
				if (getUsedNetworkType() != -1) {
					final State state = jaxmpp.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
					if (state == null || state == State.disconnected)
						(new Thread() {
							@Override
							public void run() {
								try {
									jaxmpp.getSessionObject().setProperty("messenger#error", null);
									jaxmpp.login(false);
								} catch (Exception e) {
									incrementConnectionError(jaxmpp.getSessionObject().getUserBareJid());
									Log.e(TAG, "Can't connect account " + jaxmpp.getSessionObject().getUserBareJid(), e);
								}
							}
						}).start();
				}
			}
		};

		lock(jaxmpp.getSessionObject(), true);
		if (delay == null)
			r.run();
		else {
			if (DEBUG)
				Log.d(TAG, "Shedule (time=" + delay + ") connection for account " + jaxmpp.getSessionObject().getUserBareJid());
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					r.run();
				}
			}, delay);
		}
	}

	private void connectJaxmpp(final Jaxmpp jaxmpp, final Long delay) {
		connectJaxmpp(jaxmpp, delay == null ? null : new Date(delay + System.currentTimeMillis()));
	}

	private void disconnectAllJaxmpp(final boolean cleaning) {
		setUsedNetworkType(-1);
		final MessengerApplication app = (MessengerApplication) getApplicationContext();
		for (final JaxmppCore j : getMulti().get()) {
			(new Thread() {
				@Override
				public void run() {
					try {
						GeolocationFeature.updateLocation(j, null, (Context) null);
						((Jaxmpp) j).disconnect(false);
						app.clearPresences(j.getSessionObject(), !cleaning);
					} catch (Exception e) {
						Log.e(TAG, "cant; disconnect account " + j.getSessionObject().getUserBareJid(), e);
					}
				}
			}).start();
		}
		synchronized (connectionErrorsCounter) {
			connectionErrorsCounter.clear();
		}
	}

	protected void enableMessageCarbons(SessionObject sessionObject) throws JaxmppException {
		JaxmppCore jaxmpp = getMulti().get(sessionObject);
		MessageCarbonsModule mc = jaxmpp.getModule(MessageCarbonsModule.class);
		mc.enable(new AsyncCallback() {

			@Override
			public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
			}

			@Override
			public void onSuccess(Stanza responseStanza) throws JaxmppException {
			}

			@Override
			public void onTimeout() throws JaxmppException {
			}
		});
	}

	private int getActiveNetworkConnectionType() {
		NetworkInfo info = connManager.getActiveNetworkInfo();
		if (info == null)
			return -1;
		if (!info.isConnected())
			return -1;
		return info.getType();
	}

	private int getConnectionError(final BareJID jid) {
		synchronized (connectionErrorsCounter) {
			Integer x = connectionErrorsCounter.get(jid);
			return x == null ? 0 : x.intValue();
		}
	}

	protected final MultiJaxmpp getMulti() {
		return ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();
	}

	protected final State getState(SessionObject object) {
		State state = getMulti().get(object).getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
		return state == null ? State.disconnected : state;
	}

	private int getUsedNetworkType() {
		return usedNetworkType;
	}

	private int incrementConnectionError(final BareJID jid) {
		synchronized (connectionErrorsCounter) {
			Integer x = connectionErrorsCounter.get(jid);
			int z = x == null ? 0 : x.intValue();
			++z;
			connectionErrorsCounter.put(jid, z);

			if (DEBUG)
				Log.d(TAG, "Error counter for " + jid + " is now " + z);

			return z;
		}
	}

	private boolean isReconnect() {
		return reconnect;
	}

	private void keepAlive() {
		new Thread() {
			@Override
			public void run() {
				for (JaxmppCore jaxmpp : getMulti().get()) {
					try {
						if (jaxmpp.isConnected()) {
							jaxmpp.getConnector().keepalive();
							GeolocationFeature.sendQueuedGeolocation(jaxmpp, JaxmppService.this);
						}
					} catch (JaxmppException ex) {
						Log.e(TAG, "error sending keep alive for = " + jaxmpp.getSessionObject().getUserBareJid().toString(),
								ex);
					}
				}
			}
		}.start();
	}

	private void notificationUpdate() {
		int ico = R.drawable.ic_stat_disconnected;
		String notiticationTitle = null;
		String expandedNotificationText = null;

		if (getUsedNetworkType() == -1) {
			ico = R.drawable.ic_stat_disconnected;
			notiticationTitle = getResources().getString(R.string.service_disconnected_notification_title);
			expandedNotificationText = getResources().getString(R.string.service_no_network_notification_text);
		} else {
			int onlineCount = 0;
			int offlineCount = 0;
			int connectingCount = 0;
			int disabledCount = 0;
			for (JaxmppCore jaxmpp : getMulti().get()) {
				State state = jaxmpp.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
				boolean established = jaxmpp.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID) != null;
				if (isDisabled(jaxmpp.getSessionObject()))
					++disabledCount;
				else if (state == State.connected && established)
					++onlineCount;
				else if (state == null || state == State.disconnected)
					++offlineCount;
				else
					++connectingCount;
			}

			if (connectingCount > 0) {
				ico = R.drawable.ic_stat_connecting;
				notiticationTitle = getResources().getString(R.string.service_connecting_notification_title);
				expandedNotificationText = getResources().getString(R.string.service_connecting_notification_text,
						connectingCount);
			} else if (onlineCount == 0) {
				ico = R.drawable.ic_stat_disconnected;
				notiticationTitle = getResources().getString(R.string.service_disconnected_notification_title);
				expandedNotificationText = getResources().getString(R.string.service_no_active_accounts_notification_text);
			} else {
				ico = R.drawable.ic_stat_connected;
				notiticationTitle = getResources().getString(R.string.service_connected_notification_title);
				expandedNotificationText = getResources().getString(R.string.service_online_notification_text);
			}

		}

		final Notification notification = notificationHelper.getForegroundNotification(ico, notiticationTitle,
				expandedNotificationText);

		startForeground(NotificationHelper.NOTIFICATION_ID, notification);
	}

	private void notificationUpdateFail(SessionObject account, String message, String notificationMessage, Throwable cause) {
		notificationHelper.notificationUpdateFail(account, message, notificationMessage, cause);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	protected void onConnectorError(final ConnectorEvent be) {
		be.getSessionObject().setProperty("messenger#error", be.getStreamError() != null ? be.getStreamError().name() : null);

		if (be.getStreamError() == StreamError.host_unknown) {
			notificationUpdateFail(
					be.getSessionObject(),
					getResources().getString(R.string.service_unkown_host_error,
							be.getSessionObject().getUserBareJid().getDomain()), null, null);
			disable(be.getSessionObject(), true);
		} else if (be.getCaught() != null) {
			Throwable throwable = extractCauseException(be.getCaught());
			be.getSessionObject().setProperty("messenger#error", throwable.getMessage());

			if (throwable instanceof SecureTrustManagerFactory.DataCertificateException) {
				notificationUpdateFail(be.getSessionObject(), "Server certificate not trusted", null, throwable);
				disable(be.getSessionObject(), true);
			} else if (throwable instanceof UnknownHostException) {
				Log.w(TAG, "Skipped UnknownHostException exception", throwable);
				// notificationUpdateFail(be.getSessionObject(),
				// "Connection error: unknown host " + throwable.getMessage(),
				// null,
				// null);
				// disable(be.getSessionObject(), true);
			} else if (throwable instanceof SocketException) {
				Log.w(TAG, "Skipped SocketException exception", throwable);
			} else {
				Log.w(TAG, "Skipped exception", throwable);
				// Log.e(TAG, "Connection error!", throwable);
				// notificationUpdateFail(be.getSessionObject(), null, null,
				// throwable);
				// disable(be.getSessionObject(), true);
			}
		} else {
			try {
				Log.w(TAG, "Ignored ConnectorError: " + (be.getStanza() == null ? "???" : be.getStanza().getAsString()));
			} catch (XMLException e) {
				Log.e(TAG, "Can't display exception", e);
			}
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		if (DEBUG)
			Log.i(TAG, "onCreate()");
		setUsedNetworkType(-1);
		setRecconnect(true);
		clearLocalJaxmppProperties();
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		this.prefs.registerOnSharedPreferenceChangeListener(prefChangeListener);
		this.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		this.prefChangeListener = new OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				Log.v(TAG, "key = " + key);
				if (Preferences.KEEPALIVE_TIME_KEY.equals(key)) {
					Log.v(TAG, "keepalive timout changed");
					keepaliveInterval = 1000 * 60 * sharedPreferences.getInt(key, 3);
					stopKeepAlive();
					keepAlive();
					startKeepAlive();
				}
			}
		};

		keepaliveInterval = 1000 * 60 * this.prefs.getInt(Preferences.KEEPALIVE_TIME_KEY, 3);

		this.connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		this.myConnReceiver = new ConnReceiver();
		IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
		registerReceiver(myConnReceiver, filter);
		this.focusChangeReceiver = new ClientFocusReceiver();
		filter = new IntentFilter(TigaseMobileMessengerActivity.CLIENT_FOCUS_MSG);
		registerReceiver(focusChangeReceiver, filter);
		this.accountModifyReceiver = new AccountModifyReceiver();
		filter = new IntentFilter(AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION);
		registerReceiver(accountModifyReceiver, filter);
		this.myScreenStateReceiver = new ScreenStateReceiver();
		filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(this.myScreenStateReceiver, filter);

		getMulti().addListener(DiscoInfoModule.ServerFeaturesReceived, this.serverFeaturesListener);

		getMulti().addListener(JaxmppCore.Connected, this.jaxmppConnected);

		getMulti().addListener(RosterModule.ItemAdded, this.rosterListener);
		getMulti().addListener(RosterModule.ItemRemoved, this.rosterListener);
		getMulti().addListener(RosterModule.ItemUpdated, this.rosterListener);

		getMulti().addListener(PresenceModule.ContactAvailable, this.presenceListener);
		getMulti().addListener(PresenceModule.ContactUnavailable, this.presenceListener);
		getMulti().addListener(PresenceModule.ContactChangedPresence, this.presenceListener);
		getMulti().addListener(PresenceModule.SubscribeRequest, this.subscribeRequestListener);

		getMulti().addListener(AuthModule.AuthFailed, this.invalidAuthListener);
		getMulti().addListener(Connector.StateChanged, this.stateChangeListener);

		getMulti().addListener(MessageCarbonsModule.Carbon, this.forwardedMessageListener);

		getMulti().addListener(MessageModule.MessageReceived, this.messageListener);
		getMulti().addListener(MucModule.MucMessageReceived, this.mucListener);
		getMulti().addListener(MucModule.PresenceError, this.mucListener);
		getMulti().addListener(MucModule.OccupantLeaved, this.mucListener);

		/*
		 * getMulti().addListener(FileTransferModule.ProgressEventType,
		 * this.fileTransferProgressListener);
		 * getMulti().addListener(FileTransferModule.RequestEventType,
		 * this.fileTransferRequestListener);
		 * getMulti().addListener(FileTransferModule.StreamhostsEventType,
		 * this.fileTransferStreamhostsListener);
		 */

		getMulti().addListener(PresenceModule.BeforeInitialPresence, this.presenceSendListener);

		getMulti().addListener(Connector.Error, this.connectorListener);
		getMulti().addListener(Connector.StreamTerminated, this.connectorListener);

		startKeepAlive();

		updateJaxmppInstances(getMulti(), getContentResolver(), getResources(), getApplicationContext());

		// this.notificationManager = (NotificationManager)
		// getSystemService(Context.NOTIFICATION_SERVICE);
		notificationHelper = NotificationHelper.createIntstance(this);

		final Notification notification = notificationHelper.getForegroundNotification(R.drawable.icon, "Tigase Messenger",
				"Start");
		startForeground(NotificationHelper.NOTIFICATION_ID, notification);

		notificationUpdate();
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
		serviceActive = false;
		timer.cancel();

		geolocationFeature.unregisterLocationListener();

		clearLocalJaxmppProperties();
		this.prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener);

		if (myConnReceiver != null)
			unregisterReceiver(myConnReceiver);

		if (focusChangeReceiver != null)
			unregisterReceiver(focusChangeReceiver);

		unregisterReceiver(accountModifyReceiver);
		unregisterReceiver(this.myScreenStateReceiver);

		Log.i(TAG, "Stopping service");
		setRecconnect(false);
		disconnectAllJaxmpp(true);
		stopKeepAlive();
		setUsedNetworkType(-1);

		getMulti().removeListener(JaxmppCore.Connected, this.jaxmppConnected);

		getMulti().removeListener(DiscoInfoModule.ServerFeaturesReceived, this.serverFeaturesListener);

		getMulti().removeListener(PresenceModule.BeforeInitialPresence, this.presenceSendListener);
		getMulti().removeListener(RosterModule.ItemAdded, this.rosterListener);
		getMulti().removeListener(RosterModule.ItemRemoved, this.rosterListener);
		getMulti().removeListener(RosterModule.ItemUpdated, this.rosterListener);

		getMulti().removeListener(PresenceModule.ContactAvailable, this.presenceListener);
		getMulti().removeListener(PresenceModule.ContactUnavailable, this.presenceListener);
		getMulti().removeListener(PresenceModule.ContactChangedPresence, this.presenceListener);
		getMulti().removeListener(PresenceModule.SubscribeRequest, this.subscribeRequestListener);

		getMulti().removeListener(AuthModule.AuthFailed, this.invalidAuthListener);
		getMulti().removeListener(Connector.StateChanged, this.stateChangeListener);

		getMulti().removeListener(MessageCarbonsModule.Carbon, this.forwardedMessageListener);

		getMulti().removeListener(MessageModule.MessageReceived, this.messageListener);
		getMulti().removeListener(MucModule.MucMessageReceived, this.mucListener);
		getMulti().removeListener(MucModule.PresenceError, this.mucListener);
		getMulti().removeListener(MucModule.OccupantLeaved, this.mucListener);

		/*
		 * getMulti().removeListener(FileTransferModule.ProgressEventType,
		 * this.fileTransferProgressListener);
		 * getMulti().removeListener(FileTransferModule.RequestEventType,
		 * this.fileTransferRequestListener);
		 * getMulti().removeListener(FileTransferModule.StreamhostsEventType,
		 * this.fileTransferStreamhostsListener);
		 */

		getMulti().removeListener(Connector.Error, this.connectorListener);
		getMulti().removeListener(Connector.StreamTerminated, this.connectorListener);

		notificationHelper.cancelNotification();

		super.onDestroy();
	}

	protected void onMucOccupantLeave(final MucEvent be) throws XMLException {
		final State state = be.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
		if (be.getNickname().equals(be.getRoom().getNickname()) && state == State.connected) {

			Intent intent = new Intent();

			// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			// intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

			intent.setAction(TigaseMobileMessengerActivity.MUC_ERROR_ACTION);
			intent.putExtra("account", be.getSessionObject().getUserBareJid().toString());
			intent.putExtra("roomId", be.getRoom().getId());
			intent.putExtra("roomJid", be.getRoom().getRoomJid().toString());

			String c = "Removed from room";

			if (be.getxMucUserElement() != null && be.getxMucUserElement().getStatuses().contains(307)) {
				c = "Kicked from room";
			} else if (be.getxMucUserElement() != null && be.getxMucUserElement().getStatuses().contains(301)) {
				c = "Ban!";
			}
			intent.putExtra("errorMessage", c);

			if (focused) {
				intent.setAction(MUC_ERROR_MSG);
				sendBroadcast(intent);
			} else {
				intent.setClass(getApplicationContext(), TigaseMobileMessengerActivity.class);
				notificationHelper.showMucError(be.getRoom().getRoomJid().toString(), intent);
			}

		}
	}

	protected void onMucPresenceError(MucEvent be) throws XMLException {
		Intent intent = new Intent();

		// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		intent.setAction(TigaseMobileMessengerActivity.MUC_ERROR_ACTION);
		intent.putExtra("account", be.getSessionObject().getUserBareJid().toString());
		intent.putExtra("roomId", be.getRoom().getId());
		intent.putExtra("roomJid", be.getRoom().getRoomJid().toString());

		ErrorCondition c = be.getPresence().getErrorCondition();
		if (c != null) {
			intent.putExtra("errorCondition", c.name());
			intent.putExtra("errorMessage", c.name());
		} else {
			intent.putExtra("errorCondition", "-");
			intent.putExtra("errorMessage", "-");
		}

		if (focused) {
			intent.setAction(MUC_ERROR_MSG);
			sendBroadcast(intent);
		} else {
			intent.setClass(getApplicationContext(), TigaseMobileMessengerActivity.class);
			notificationHelper.showMucError(be.getRoom().getRoomJid().toString(), intent);
		}
	}

	public void onNetworkChanged(final NetworkInfo netInfo) {
		if (DEBUG) {
			Log.d(TAG,
					"Network " + (netInfo == null ? null : netInfo.getTypeName()) + " ("
							+ (netInfo == null ? null : netInfo.getType()) + ") state changed! Currently used="
							+ getUsedNetworkType() + " detailed state = "
							+ (netInfo != null ? netInfo.getDetailedState() : null));
		}

		if (netInfo != null && netInfo.isConnected()) {
			if (DEBUG)
				Log.d(TAG, "Network became available");
			setRecconnect(true);
			synchronized (connectionErrorsCounter) {
				connectionErrorsCounter.clear();
			}
			connectAllJaxmpp(5000l);
		} else {
			if (DEBUG)
				Log.d(TAG, "No internet connection");
			setRecconnect(false);
			disconnectAllJaxmpp(false);
		}
	}

	protected void onPageChanged(int pageIndex) {
		if (!focused && pageIndex >= 0) {
			if (DEBUG)
				Log.d(TAG, "Focused. Sending online presence.");
			focused = true;
			int pr = prefs.getInt(Preferences.DEFAULT_PRIORITY_KEY, 5);

			// setMobileMode(false);
			sendAutoPresence(userStatusShow, userStatusMessage, pr, false);
		} else if (focused && pageIndex == -1) {
			if (DEBUG)
				Log.d(TAG, "Sending auto-away presence");
			focused = false;
			int pr = prefs.getInt(Preferences.AWAY_PRIORITY_KEY, 0);
			// setMobileMode(true);
			sendAutoPresence(Show.away, "Auto away", pr, true);
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (DEBUG)
			Log.i(TAG, "onStart()");
	}

	// added to fix Eclipse error
	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		if (DEBUG)
			Log.i(TAG, "onStartCommand()");

		if (intent != null && intent.getAction() != null) {
			if (intent.getAction().equals(ACTION_KEEPALIVE)) {
				keepAlive();
			} else if (intent.getAction().equals(ACTION_FILETRANSFER)) {
				fileTransferFeature.processFileTransferAction(intent);
			}

		} else {

			if (intent != null) {
				// Log.i(TAG, intent.getExtras().toString());
				if (DEBUG)
					Log.i(TAG, "Found intent! focused=" + intent.getBooleanExtra("focused", false));
				JaxmppService.focused = intent.getBooleanExtra("focused", false);
			}

			serviceActive = true;

			notificationUpdate();

			connectAllJaxmpp(null);

			geolocationFeature.unregisterLocationListener();
		}
		return START_STICKY;
	}

	protected void onSubscribeRequest(PresenceEvent be) {
		notificationHelper.notifySubscribeRequest(be);
	}

	protected void reconnectIfAvailable(final SessionObject sessionObject) {
		if (!isReconnect()) {
			if (DEBUG)
				Log.d(TAG, "Reconnect disabled for: " + sessionObject.getUserBareJid());
			return;
		}

		if (DEBUG)
			Log.d(TAG, "Preparing for reconnect " + sessionObject.getUserBareJid());

		final Jaxmpp j = getMulti().get(sessionObject);

		int connectionErrors = getConnectionError(j.getSessionObject().getUserBareJid());

		if (connectionErrors > 50) {
			disable(sessionObject, true);
			notificationUpdateFail(sessionObject, getResources().getString(R.string.service_too_many_errors_disabled), null,
					null);
		} else
			connectJaxmpp(j, calculateNextRestart(5, connectionErrors));

	}

	private void rejoinToRooms(final SessionObject sessionObject) {
		try {
			for (ChatWrapper x : getMulti().getChats()) {
				if (x.isRoom() && x.getRoom().getSessionObject() == sessionObject
						&& x.getRoom().getState() != tigase.jaxmpp.core.client.xmpp.modules.muc.Room.State.joined) {
					x.getRoom().rejoin();
				}
			}
		} catch (JaxmppException e) {
			Log.e(TAG, "Problem on rejoining", e);
		}
	}

	private void retrieveVCard(final SessionObject sessionObject, final BareJID jid) {
		try {
			JaxmppCore jaxmpp = getMulti().get(sessionObject);
			if (jaxmpp == null)
				return;
			final RosterItem rosterItem = jaxmpp.getRoster().get(jid);
			jaxmpp.getModule(VCardModule.class).retrieveVCard(JID.jidInstance(jid), (long) 3 * 60 * 1000,
					new VCardAsyncCallback() {

						@Override
						public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
						}

						@Override
						public void onTimeout() throws JaxmppException {
						}

						@Override
						protected void onVCardReceived(VCard vcard) throws XMLException {
							try {
								if (vcard.getPhotoVal() != null && vcard.getPhotoVal().length() > 0) {
									ContentValues values = new ContentValues();
									byte[] buffer = Base64.decode(vcard.getPhotoVal());

									values.put(VCardsCacheTableMetaData.FIELD_DATA, buffer);
									getContentResolver().insert(
											Uri.parse(RosterProvider.VCARD_URI + "/" + Uri.encode(jid.toString())), values);

									if (rosterItem != null) {
										Uri insertedItem = ContentUris.withAppendedId(Uri.parse(RosterProvider.CONTENT_URI),
												rosterItem.getId());
										getApplicationContext().getContentResolver().notifyChange(insertedItem, null);
									}

								}
							} catch (Exception e) {
								Log.e("tigase", "WTF?", e);
							}
						}
					});
		} catch (Exception e) {
			Log.e("tigase", "WTF?", e);
		}
	}

	private void sendAutoPresence(final Show show, final String status, final int priority, final boolean delayed) {
		if (autoPresenceTask != null) {
			autoPresenceTask.cancel();
			autoPresenceTask = null;
		}

		if (delayed) {
			autoPresenceTask = new TimerTask() {

				@Override
				public void run() {
					autoPresenceTask = null;
					try {
						for (JaxmppCore jaxmpp : getMulti().get()) {
							final PresenceModule presenceModule = jaxmpp.getModule(PresenceModule.class);
							if (jaxmpp.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY) == Connector.State.connected)
								presenceModule.setPresence(show, status, priority);
						}
					} catch (Exception e) {
						Log.e(TAG, "Can't send auto presence!", e);
					}
				}
			};
			timer.schedule(autoPresenceTask, 1000 * 60);
		} else {
			(new Thread() {
				@Override
				public void run() {
					try {
						for (JaxmppCore jaxmpp : getMulti().get()) {
							final PresenceModule presenceModule = jaxmpp.getModule(PresenceModule.class);
							if (jaxmpp.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY) == Connector.State.connected)
								presenceModule.setPresence(show, status, priority);
						}
					} catch (Exception e) {
						Log.e(TAG, "Can't send auto presence!", e);
					}
				}
			}).start();
		}

	}

	protected void sendUnsentMessages() {
		final Cursor c = getApplication().getContentResolver().query(Uri.parse(ChatHistoryProvider.UNSENT_MESSAGES_URI), null,
				null, null, null);
		try {
			c.moveToFirst();
			if (c.isAfterLast())
				return;
			do {
				long id = c.getLong(c.getColumnIndex(ChatTableMetaData.FIELD_ID));
				String jid = c.getString(c.getColumnIndex(ChatTableMetaData.FIELD_JID));
				String body = c.getString(c.getColumnIndex(ChatTableMetaData.FIELD_BODY));
				String threadId = c.getString(c.getColumnIndex(ChatTableMetaData.FIELD_THREAD_ID));
				BareJID account = BareJID.bareJIDInstance(c.getString(c.getColumnIndex(ChatTableMetaData.FIELD_ACCOUNT)));

				final JID ownJid = getMulti().get(account).getSessionObject().getProperty(
						ResourceBinderModule.BINDED_RESOURCE_JID);
				final String nickname = ownJid.getLocalpart();

				Message msg = Message.create();
				msg.setType(StanzaType.chat);
				msg.setTo(JID.jidInstance(jid));
				msg.setBody(body);
				if (threadId != null && threadId.length() > 0)
					msg.setThread(threadId);
				if (DEBUG)
					Log.i(TAG, "Found unsetn message: " + jid + " :: " + body);

				try {
					getMulti().get(account).send(msg);

					ContentValues values = new ContentValues();
					values.put(ChatTableMetaData.FIELD_ID, id);
					values.put(ChatTableMetaData.FIELD_AUTHOR_JID, ownJid.getBareJid().toString());
					values.put(ChatTableMetaData.FIELD_AUTHOR_NICKNAME, nickname);
					values.put(ChatTableMetaData.FIELD_STATE, ChatTableMetaData.STATE_OUT_SENT);

					getContentResolver().update(Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + jid + "/" + id), values, null,
							null);
				} catch (JaxmppException e) {
					if (DEBUG)
						Log.d(TAG, "Can't send message");
				}

				c.moveToNext();
			} while (!c.isAfterLast());
		} catch (XMLException e) {
			Log.e(TAG, "WTF??", e);
		} finally {
			c.close();
		}
	}

	private void setConnectionError(final BareJID jid, final int count) {
		if (DEBUG)
			Log.d(TAG, "Error counter for " + jid + " is now " + count);
		synchronized (connectionErrorsCounter) {
			if (count == 0)
				connectionErrorsCounter.remove(jid);
			else
				connectionErrorsCounter.put(jid, count);
		}
	}

	private void setRecconnect(boolean reconnectAvailable) {
		if (DEBUG)
			Log.d(TAG, "Reconnect is now set to " + reconnectAvailable, new Exception("TRACE"));
		this.reconnect = reconnectAvailable;
	}

	private void setUsedNetworkType(int type) {
		if (DEBUG)
			Log.d(TAG, "Used NetworkType is now " + type, new Exception("TRACE"));
		usedNetworkType = type;
	}

	protected void showChatNotification(final MessageEvent event) throws XMLException {
		if (!focused || currentChatIdFocus != event.getChat().getId()) {
			notificationHelper.notifyNewChatMessage(event);
		}
	}

	protected void showMucMessageNotification(final MucEvent be) throws XMLException {
		String nick = be.getRoom().getNickname();
		String msg = be.getMessage().getBody();

		if (msg.toLowerCase().contains(nick.toLowerCase())) {
			if (!focused || currentRoomIdFocus != be.getRoom().getId()) {
				notificationHelper.notifyNewMucMessage(be);
			}
		}
	}

	private void startKeepAlive() {
		Intent i = new Intent();
		i.setClass(this, JaxmppService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + keepaliveInterval,
				keepaliveInterval, pi);
	}

	private void stopKeepAlive() {
		Intent i = new Intent();
		i.setClass(this, JaxmppService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.cancel(pi);
	}

	protected void storeIncomingMessage(MessageEvent be, boolean showNotification) throws XMLException {
		if (be.getChat() != null && (be.getMessage().getBody() != null || be.getMessage().getType() == StanzaType.error)) {

			Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + be.getChat().getJid().getBareJid().toString());

			ContentValues values = new ContentValues();
			values.put(ChatTableMetaData.FIELD_THREAD_ID, be.getChat().getThreadId());
			values.put(ChatTableMetaData.FIELD_JID, be.getChat().getJid().getBareJid().toString());
			values.put(ChatTableMetaData.FIELD_AUTHOR_JID, be.getChat().getJid().getBareJid().toString());
			values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
			Message msg = be.getMessage();
			if (msg.getType() == StanzaType.error) {
				ErrorElement error = ErrorElement.extract(msg);
				String body = "Error: ";
				if (error != null) {
					if (error.getText() != null) {
						body += error.getText();
					} else {
						ErrorCondition errorCondition = error.getCondition();
						if (errorCondition != null) {
							body += errorCondition.getElementName();
						}
					}
				}
				if (msg.getBody() != null) {
					body += " ------ ";
					body += msg.getBody();
				}
				values.put(ChatTableMetaData.FIELD_BODY, body);
			} else {
				values.put(ChatTableMetaData.FIELD_BODY, msg.getBody());
			}
			values.put(ChatTableMetaData.FIELD_STATE,
					currentChatIdFocus != be.getChat().getId() ? ChatTableMetaData.STATE_INCOMING_UNREAD
							: ChatTableMetaData.STATE_INCOMING);
			values.put(ChatTableMetaData.FIELD_ACCOUNT, be.getSessionObject().getUserBareJid().toString());

			getContentResolver().insert(uri, values);

			if (showNotification)
				showChatNotification(be);
		}

	}

	protected void storeOutgoingMessage(MessageEvent be) throws XMLException {

		if (be.getChat() != null && (be.getMessage().getBody() != null || be.getMessage().getType() == StanzaType.error)) {
			final Chat chat = be.getChat();
			final String t = be.getMessage().getBody();

			Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + chat.getJid().getBareJid().toString());

			ContentValues values = new ContentValues();
			values.put(ChatTableMetaData.FIELD_AUTHOR_JID, chat.getSessionObject().getUserBareJid().toString());
			values.put(ChatTableMetaData.FIELD_JID, chat.getJid().getBareJid().toString());
			values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
			values.put(ChatTableMetaData.FIELD_BODY, t);
			values.put(ChatTableMetaData.FIELD_THREAD_ID, chat.getThreadId());
			values.put(ChatTableMetaData.FIELD_ACCOUNT, chat.getSessionObject().getUserBareJid().toString());
			values.put(ChatTableMetaData.FIELD_STATE, ChatTableMetaData.STATE_OUT_SENT);

			getContentResolver().insert(uri, values);

		}
	}
	
	@Override
	public void onTrimMemory(int level) {
		AvatarHelper.onTrimMemory(level);
	}

	protected synchronized void updateRosterItem(final PresenceEvent be) throws XMLException {
		RosterItem it = getMulti().get(be.getSessionObject()).getRoster().get(be.getJid().getBareJid());
		if (it != null) {
			// Log.i(TAG, "Item " + it.getJid() + " has changed presence");

			Element x = be != null && be.getPresence() != null ? be.getPresence().getChildrenNS("x", "vcard-temp:x:update")
					: null;
			if (x != null) {
				for (Element c : x.getChildren()) {
					if (c.getName().equals("photo") && c.getValue() != null) {
						boolean retrieve = false;
						final String sha = c.getValue();
						if (sha == null)
							continue;
						final Cursor cursor = getContentResolver().query(
								Uri.parse(RosterProvider.VCARD_URI + "/" + Uri.encode(it.getJid().toString())), null, null,
								null, null);
						try {
							boolean isInCahe = cursor.moveToNext();

							if (isInCahe) {
								String hash = cursor.getString(cursor.getColumnIndex(VCardsCacheTableMetaData.FIELD_HASH));
								retrieve = !hash.equalsIgnoreCase(sha);
							} else
								retrieve = true;

						} finally {
							cursor.close();
						}

						if (retrieve)
							retrieveVCard(be.getSessionObject(), it.getJid());

					}
				}
			}
		}

		// Synchronize contact status
		SyncAdapter.syncContactStatus(getApplicationContext(), be);
	}

}
