package org.tigase.messenger.phone.pro;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLSocketFactory;

import org.tigase.messenger.phone.pro.account.AccountAuthenticator;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.db.ChatTableMetaData;
import org.tigase.messenger.phone.pro.db.DatabaseHelper;
import org.tigase.messenger.phone.pro.db.providers.ChatHistoryProvider;
import org.tigase.messenger.phone.pro.db.providers.RosterProviderExt;
import org.tigase.messenger.phone.pro.roster.CPresence;
import org.tigase.messenger.phone.pro.security.SecureTrustManagerFactory;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.android.chat.AndroidChatManager;
import tigase.jaxmpp.android.chat.ChatProvider;
import tigase.jaxmpp.android.roster.AndroidRosterStore;
import tigase.jaxmpp.android.roster.RosterProvider;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.JaxmppCore.ConnectedHandler;
import tigase.jaxmpp.core.client.JaxmppCore.DisconnectedHandler;
import tigase.jaxmpp.core.client.MultiJaxmpp;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.factory.UniversalFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.SoftwareVersionModule;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.AbstractChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceStore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.ErrorElement;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import tigase.jaxmpp.core.client.xmpp.utils.delay.XmppDelay;
import tigase.jaxmpp.j2se.J2SEPresenceStore;
import tigase.jaxmpp.j2se.J2SESessionObject;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.Service;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

public class JaxmppService extends Service implements ConnectedHandler, DisconnectedHandler {
	
	private class Stub extends IJaxmppService.Stub {

		@Override
		public boolean isConnected(String accountJidStr) throws RemoteException {
			BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
			JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
			return jaxmpp == null ? false : jaxmpp.isConnected();
		}

		@Override
		public List<CPresence> getPresences(String accountJidStr, String jidStr)
				throws RemoteException {
			BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
			JID jid = JID.jidInstance(jidStr);
			JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
			PresenceStore store = PresenceModule.getPresenceStore(jaxmpp.getSessionObject());
			List<CPresence> result = new ArrayList<CPresence>();
			if (jid.getResource() != null) {
				Presence p = store.getPresence(jid);
				if (p != null) {
					try {
						result.add(new CPresence(p));
					} catch (XMLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else {
				Map<String,Presence> presences = store.getPresences(jid.getBareJid());
				if (presences != null) {
					for (Presence p : presences.values()) {
						try {
							result.add(new CPresence(p));
						} catch (XMLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}						
					}
				}
			}
			
			return result;
		}

		@Override
		public CPresence getBestPresence(String accountJidStr, String jidStr)
				throws RemoteException {
			BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
			BareJID jid = BareJID.bareJIDInstance(jidStr);
			JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
			PresenceStore store = PresenceModule.getPresenceStore(jaxmpp.getSessionObject());
			CPresence result = null;
			try {
				Presence p = store.getBestPresence(jid);
				if (p != null) {
					result = new CPresence(p);
				}
			} catch (XMLException ex) {				
			}
			return result;
		}

		@Override
		public boolean openChat(String accountJidStr, String jidStr)
				throws RemoteException {
			try {
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
				JID jid = JID.jidInstance(jidStr);			
				JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				MessageModule messageModule = jaxmpp.getModule(MessageModule.class);		
				boolean chatExists = messageModule.getChatManager().isChatOpenFor(jid.getBareJid());
				if (!chatExists) {
					messageModule.createChat(jid);
					return true;
				}
				return false;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "EXCEPTION", e);
			}
			return false;
		}

		@Override
		public boolean sendMessage(String accountJidStr, String jidStr, String threadId,
				String body) throws RemoteException {
			try {
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);		
				JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				MessageModule messageModule = jaxmpp.getModule(MessageModule.class);	
				JID jid = JID.jidInstance(jidStr);
				Chat chat = messageModule.getChatManager().getChat(jid, threadId);
				if (chat != null) {
					chat.sendMessage(body);
					return true;
				}
				return false;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "EXCEPTION", e);
			}			
			return false;
		}
		
	}
	
	private class MessageHandler implements MessageModule.MessageReceivedHandler {

		@Override
		public void onMessageReceived(SessionObject sessionObject, Chat chat,
				tigase.jaxmpp.core.client.xmpp.stanzas.Message msg) {
			try {
				// for now let's ignore messages without body element
				if (msg.getBody() == null)
					return;
				
				Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + chat.getJid().getBareJid().toString());
	
				ContentValues values = new ContentValues();
				values.put(ChatTableMetaData.FIELD_AUTHOR_JID, chat.getJid().getBareJid().toString());
				values.put(ChatTableMetaData.FIELD_JID, chat.getJid().getBareJid().toString());

				XmppDelay delay = XmppDelay.extract(msg);
				values.put(ChatTableMetaData.FIELD_TIMESTAMP, (delay == null ? new Date() : delay.getStamp()).getTime());
				if (msg.getType() == StanzaType.error) {
					ErrorElement error = ErrorElement.extract(msg);
					String body = "Error: ";
					if (error != null) {
						if (error.getText() != null) {
							body += error.getText();
						} else {
							ErrorCondition errorCondition = error
									.getCondition();
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
				values.put(ChatTableMetaData.FIELD_BODY, msg.getBody());
				values.put(ChatTableMetaData.FIELD_THREAD_ID, chat.getThreadId());
				values.put(ChatTableMetaData.FIELD_ACCOUNT, sessionObject.getUserBareJid().toString());
				values.put(ChatTableMetaData.FIELD_STATE, ChatTableMetaData.STATE_INCOMING_UNREAD);			
				
				SQLiteDatabase db = dbHelper.getWritableDatabase();
				long id = db.insert(ChatTableMetaData.TABLE_NAME, null, values);
				Log.v(TAG, "inserted message - id = " + id);
				context.getContentResolver().notifyChange(uri, null);
				//context.getContentResolver().insert(uri, values);
			} catch (Exception ex) {
				Log.e(TAG, "Exception handling received message", ex);
			}
		}
		
	}
	
	private class PresenceHandler implements PresenceModule.ContactAvailableHandler, 
		PresenceModule.ContactUnavailableHandler, PresenceModule.ContactChangedPresenceHandler {

		private final JaxmppService jaxmppService;
		
		public PresenceHandler(JaxmppService jaxmppService) {
			this.jaxmppService = jaxmppService;
		}
		
		@Override
		public void onContactChangedPresence(SessionObject sessionObject,
				Presence stanza, JID jid, Show show, String status,
				Integer priority) throws JaxmppException {
			rosterProvider.updateStatus(sessionObject, jid);
		}

		@Override
		public void onContactUnavailable(SessionObject sessionObject,
				Presence stanza, JID jid, String status) {
			rosterProvider.updateStatus(sessionObject, jid);		
		}

		@Override
		public void onContactAvailable(SessionObject sessionObject,
				Presence stanza, JID jid, Show show, String status,
				Integer priority) throws JaxmppException {
			rosterProvider.updateStatus(sessionObject, jid);
		}
		
	}
	
	public static final int SEND_MESSAGE = 1;

	private static final String TAG = "JaxmppService";
	private static final StanzaExecutor executor = new StanzaExecutor();
	
	public static Context context = null;
	
	protected final Timer timer = new Timer();
	
	// do we need this any more?
	private Map<BareJID,Chat> chats = new HashMap<BareJID,Chat>();
	private MultiJaxmpp multiJaxmpp = new MultiJaxmpp();
	private ConnectivityManager connManager;
	
	private HashSet<SessionObject> locked = new HashSet<SessionObject>();
	private int usedNetworkType = -1;	
	
	private AccountModifyReceiver accountModifyReceiver = new AccountModifyReceiver();
	private DatabaseHelper dbHelper = null;
	private MessageHandler messageHandler = null;
	private PresenceHandler presenceHandler = null;
	private RosterProviderExt rosterProvider = null;
	private ChatProvider chatProvider = null;
	
	private class AccountModifyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
            updateJaxmppInstances();
            for (JaxmppCore j : multiJaxmpp.get()) {
                State st = getState(j.getSessionObject());
                if (st == State.disconnected || st == null) {
                    connectJaxmpp((Jaxmpp) j, (Long) null);
                }
            }			
		}
		
	}
	
	private class IncomingHandler extends Handler {
		
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case SEND_MESSAGE:					
					Bundle data = msg.getData();
					BareJID account = BareJID.bareJIDInstance(data.getString("account"));
					JID to = JID.jidInstance(data.getString("to"));
					final String body = data.getString("message");
					JaxmppCore jaxmpp = multiJaxmpp.get(account);
					Log.v(TAG, "for account " + account.toString() + " got " + (jaxmpp == null ? "null" : jaxmpp.toString()));
					MessageModule messageModule = jaxmpp.getModule(MessageModule.class);
					Chat chat = chats.get(to.getBareJid());
					if (chat == null) {
						try {
							chat = messageModule.createChat(to);
							chats.put(to.getBareJid(), chat);
						} catch (JaxmppException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
					final Chat ch = chat;
					new Thread() {
						@Override
						public void run() {
							try {
								ch.sendMessage(body);
							} catch (JaxmppException e) {
								e.printStackTrace();
							}							
						}
					}.start();

					break;
				default:
					super.handleMessage(msg);
			}
		}
		
	}
	
	final Messenger messenger = new Messenger(new IncomingHandler());

	@Override
	public void onCreate() {
		context = this;
		
		setUsedNetworkType(-1);
		
		this.connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		
		IntentFilter filter = new IntentFilter(AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION);
		registerReceiver(accountModifyReceiver, filter);

		this.dbHelper = new DatabaseHelper(this);
		this.rosterProvider = new RosterProviderExt(this, dbHelper, new RosterProvider.Listener() {		
			@Override
			public void onChange(Long rosterItemId) {
				Uri uri = rosterItemId != null 
						? Uri.parse(org.tigase.messenger.phone.pro.db.providers.RosterProvider.CONTENT_URI + "/" + rosterItemId)
						: Uri.parse(org.tigase.messenger.phone.pro.db.providers.RosterProvider.CONTENT_URI);
				context.getContentResolver().notifyChange(uri, null);
			}
		}, Preferences.ROSTER_VERSION_KEY);
		rosterProvider.resetStatus();

		this.presenceHandler = new PresenceHandler(this);
		this.messageHandler = new MessageHandler();
		this.chatProvider = new ChatProvider(this, dbHelper, new ChatProvider.Listener() {
			@Override
			public void onChange(Long chatId) {
				Uri uri = chatId != null
						? Uri.parse(org.tigase.messenger.phone.pro.db.providers.OpenChatsProvider.OPEN_CHATS_URI + "/" + chatId)
						: Uri.parse(org.tigase.messenger.phone.pro.db.providers.OpenChatsProvider.OPEN_CHATS_URI);
				context.getContentResolver().notifyChange(uri, null);
			}			
		});
		
		multiJaxmpp.addHandler(JaxmppCore.ConnectedHandler.ConnectedEvent.class, this);
		multiJaxmpp.addHandler(JaxmppCore.DisconnectedHandler.DisconnectedEvent.class, this);
		multiJaxmpp.addHandler(PresenceModule.ContactAvailableHandler.ContactAvailableEvent.class, presenceHandler);
		multiJaxmpp.addHandler(PresenceModule.ContactUnavailableHandler.ContactUnavailableEvent.class, presenceHandler);
		multiJaxmpp.addHandler(PresenceModule.ContactChangedPresenceHandler.ContactChangedPresenceEvent.class, presenceHandler);
		multiJaxmpp.addHandler(MessageModule.MessageReceivedHandler.MessageReceivedEvent.class, messageHandler);
		
		updateJaxmppInstances();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		if ("AIDL".equals(intent.getStringExtra("ID"))) {
			return new Stub();
		}
		return messenger.getBinder();
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
		timer.cancel();
		
		disconnectAllJaxmpp(true);
		setUsedNetworkType(-1);
		
		super.onDestroy();
		context = null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
//		if (intent != null && intent.getAction() != null) {
//			
//		}
//		else {
		if (intent != null && "connect-all".equals(intent.getAction())) {
			connectAllJaxmpp(null);
		}
//		}

		return Service.START_STICKY;
	}
	
	@Override
	public void onConnected(SessionObject sessionObject) {
		// TODO Auto-generated method stub
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setContentTitle("Connection").setContentText("" + sessionObject.getUserBareJid().toString() + " - Connected");
		NotificationManager mNotificationManager =
			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);	
		mNotificationManager.notify(10, builder.build());
	}
	
	@Override
	public void onDisconnected(SessionObject sessionObject) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setContentTitle("Connection").setContentText("" + sessionObject.getUserBareJid().toString() + " - Disconnected");
		NotificationManager mNotificationManager =
			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);	
		mNotificationManager.notify(10, builder.build());
	}

    protected final State getState(SessionObject object) {
        State state = multiJaxmpp.get(object).getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
        return state == null ? State.disconnected : state;
    }	
    
    private void updateJaxmppInstances() {
    	Resources resources = this.getResources();
    	final HashSet<BareJID> accountsJids = new HashSet<BareJID>();
    	for (JaxmppCore jaxmpp : multiJaxmpp.get()) {
    		accountsJids.add(jaxmpp.getSessionObject().getUserBareJid());
    	}
    	
    	AccountManager am = AccountManager.get(this);
    	for (Account account : am.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)) {
    		BareJID accountJid = BareJID.bareJIDInstance(account.name);
    		String password = am.getPassword(account);
    		String nickname = am.getUserData(account, AccountsConstants.FIELD_NICKNAME);
    		String hostname = am.getUserData(account, AccountsConstants.FIELD_HOSTNAME);
    		String resource = am.getUserData(account, AccountsConstants.FIELD_RESOURCE);
    		hostname = hostname == null ? null : hostname.trim();
    		
    		Jaxmpp jaxmpp = multiJaxmpp.get(accountJid);
    		if (jaxmpp == null) {
    			SessionObject sessionObject = new J2SESessionObject();
    			
    			sessionObject.setUserProperty(Connector.TRUST_MANAGERS_KEY, SecureTrustManagerFactory.getTrustManagers());
    			sessionObject.setUserProperty(SoftwareVersionModule.VERSION_KEY, resources.getString(R.string.app_version));
    			sessionObject.setUserProperty(SoftwareVersionModule.NAME_KEY, resources.getString(R.string.app_name));
    			sessionObject.setUserProperty(SoftwareVersionModule.OS_KEY, "Android " + android.os.Build.VERSION.RELEASE);

    			sessionObject.setUserProperty(DiscoveryModule.IDENTITY_CATEGORY_KEY, "client");
    			sessionObject.setUserProperty(DiscoveryModule.IDENTITY_TYPE_KEY, "phone");
    			sessionObject.setUserProperty(CapabilitiesModule.NODE_NAME_KEY, "http://tigase.org/messenger");
    			
    			sessionObject.setUserProperty("ID", (long) account.hashCode());
    			sessionObject.setUserProperty(SocketConnector.SERVER_PORT, 5222);
    			sessionObject.setUserProperty(tigase.jaxmpp.j2se.Jaxmpp.CONNECTOR_TYPE, "socket");
    			sessionObject.setUserProperty(Connector.EXTERNAL_KEEPALIVE_KEY, true);    		
    			
    			sessionObject.setUserProperty(SessionObject.USER_BARE_JID, accountJid);
    			
    			// Android from API v8 contains optimized SSLSocketFactory
    			// which reduces network usage for handshake
    			SSLSessionCache sslSessionCache = new SSLSessionCache(this);
    			SSLSocketFactory sslSocketFactory = SSLCertificateSocketFactory.getDefault(0, sslSessionCache);
    			sessionObject.setUserProperty(SocketConnector.SSL_SOCKET_FACTORY_KEY, sslSocketFactory);    			
    			
    			jaxmpp = new Jaxmpp(sessionObject);
    			jaxmpp.setExecutor(executor);

    			RosterModule.setRosterStore(sessionObject, new AndroidRosterStore(this.rosterProvider));
    			jaxmpp.getModulesManager().register(new RosterModule(this.rosterProvider));
    			PresenceModule.setPresenceStore(sessionObject, new J2SEPresenceStore());
    			jaxmpp.getModulesManager().register(new PresenceModule());
    			jaxmpp.getModulesManager().register(new MessageModule(new AndroidChatManager(this.chatProvider)));
    			
    			Log.v(TAG, "registering account " + accountJid.toString());
    			multiJaxmpp.add(jaxmpp);
    		}
    		
    		SessionObject sessionObject = jaxmpp.getSessionObject();
    		sessionObject.setUserProperty(SessionObject.PASSWORD, password);
    		sessionObject.setUserProperty(SessionObject.NICKNAME, nickname);
    		if (hostname != null && TextUtils.isEmpty(hostname)) hostname = null;
    		sessionObject.setUserProperty(SessionObject.DOMAIN_NAME, hostname);
    		if (TextUtils.isEmpty(resource)) resource = null;
    		sessionObject.setUserProperty(SessionObject.RESOURCE, resource);

    		accountsJids.remove(accountJid);
    	}
    	
    	for (BareJID accountJid : accountsJids) {
    		final Jaxmpp jaxmpp = multiJaxmpp.get(accountJid);
    		if (jaxmpp != null) {
    			multiJaxmpp.remove(jaxmpp);
    			(new Thread() {
    				@Override
    				public void run() {
    					try {
    						jaxmpp.disconnect();
    						// clear presences for account?
    						// app.clearPresences(jaxmpp.getSessionObject(), false);
    					}
    					catch (Exception ex) {
    						Log.e(TAG, "Can't disconnect", ex);
    					}
    				}
    			}).start();
    		}
    	}
    }
    
    private void connectAllJaxmpp(Long delay) {
    	for (final JaxmppCore jaxmpp : multiJaxmpp.get()) {
    		Log.v(TAG, "connecting account " + jaxmpp.getSessionObject().getUserBareJid());
    		connectJaxmpp((Jaxmpp) jaxmpp, delay);
    	}
    }
    
    private void connectJaxmpp(final Jaxmpp jaxmpp, final Date date) {
    	if (isLocked(jaxmpp.getSessionObject())) {
    		Log.v(TAG, "cancelling connect for " + jaxmpp.getSessionObject().getUserBareJid() + " because it is locked");
    		return;
    	}
    	
    	final Runnable r = new Runnable() {
    		@Override
    		public void run() {
    			if (isDisabled(jaxmpp.getSessionObject())) {
    				Log.v(TAG, "cancelling connect for " + jaxmpp.getSessionObject().getUserBareJid() + " because it is disabled");
    				return;
    			}
    			
    			lock(jaxmpp.getSessionObject(), false);
    			setUsedNetworkType(getActiveNetworkType());
    			if (getUsedNetworkType() != -1) {
    				final State state = jaxmpp.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
    				if (state == null || state == State.disconnected) {
    					(new Thread() {
    						@Override
    						public void run() {
    							try {
    								jaxmpp.getSessionObject().setProperty("messenger#error", null);
    								jaxmpp.login();
    							}
    							catch (Exception e) {
    								// incrementConnectionError(jaxmpp.getSessionObject().getUserBareJid());
    								Log.e(TAG, "Can't connect account " + jaxmpp.getSessionObject().getUserBareJid(), e);
    							}
    						}
    					}).start();
    				}
    			}
    		}
    	};
    	
    	if (date == null) {
    		r.run();
    	}
    	else {
    		timer.schedule(new TimerTask() {
    			@Override
    			public void run() {
    				r.run();
    			}
    		}, date);
    	}
    }
    
    private void connectJaxmpp(final Jaxmpp jaxmpp, final Long delay) {
    	connectJaxmpp(jaxmpp, delay == null ? null : new Date(delay + System.currentTimeMillis()));
    }
    
    private void disconnectAllJaxmpp(final boolean cleaning) {
        setUsedNetworkType(-1);
        final MessengerApplication app = (MessengerApplication) getApplicationContext();
        for (final JaxmppCore j : multiJaxmpp.get()) {
                (new Thread() {
                        @Override
                        public void run() {
                                try {
//                                        GeolocationFeature.updateLocation(j, null, null);
                                        ((Jaxmpp) j).disconnect(false);
//                                        app.clearPresences(j.getSessionObject(), !cleaning);
                                } catch (Exception e) {
                                        Log.e(TAG, "cant; disconnect account " + j.getSessionObject().getUserBareJid(), e);
                                }
                        }
                }).start();
        }
        
//        synchronized (connectionErrorsCounter) {
//                connectionErrorsCounter.clear();
//        }
    }
    
    private int getActiveNetworkType() {
    	NetworkInfo info = connManager.getActiveNetworkInfo();
    	if (info == null)
    		return -1;
    	if (!info.isConnected())
    		return -1;
    	return info.getType();
    }
    
    private boolean isDisabled(SessionObject sessionObject) {
    	Boolean x = sessionObject.getProperty("CC:DISABLED");
    	return x == null ? false : x;
    }
    
    private boolean isLocked(SessionObject sessionObject) {
    	synchronized (locked) {
    		return locked.contains(sessionObject);
    	}
    }
    
    private void lock(SessionObject sessionObject, boolean value) {
    	synchronized (locked) {
    		if (value) {
    			locked.add(sessionObject);
    		}
    		else {
    			locked.remove(sessionObject);
    		}
    	}
    }
    
    private void setUsedNetworkType(int type) {
    	this.usedNetworkType = type;
    }
    
    private int getUsedNetworkType() {
    	return this.usedNetworkType;
    }
    
}
