package org.tigase.messenger.phone.pro.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLSocketFactory;

import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.MessengerApplication;
import org.tigase.messenger.phone.pro.Preferences;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.StanzaExecutor;
import org.tigase.messenger.phone.pro.IJaxmppService.Stub;
import org.tigase.messenger.phone.pro.R.drawable;
import org.tigase.messenger.phone.pro.R.string;
import org.tigase.messenger.phone.pro.account.AccountAuthenticator;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.db.ChatTableMetaData;
import org.tigase.messenger.phone.pro.db.DatabaseHelper;
import org.tigase.messenger.phone.pro.db.VCardsCacheTableMetaData;
import org.tigase.messenger.phone.pro.db.providers.ChatHistoryProvider;
import org.tigase.messenger.phone.pro.db.providers.RosterProviderExt;
import org.tigase.messenger.phone.pro.muc.Occupant;
import org.tigase.messenger.phone.pro.roster.CPresence;
import org.tigase.messenger.phone.pro.roster.RosterUpdateCallback;
import org.tigase.messenger.phone.pro.security.SecureTrustManagerFactory;
import org.tigase.messenger.phone.pro.share.FileTransferUtility;
import org.tigase.messenger.phone.pro.sync.SyncAdapter;
import org.tigase.messenger.phone.pro.ui.NotificationHelper;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;
import org.tigase.messenger.phone.pro.utils.ImageHelper;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.android.caps.CapabilitiesDBCache;
import tigase.jaxmpp.android.chat.AndroidChatManager;
import tigase.jaxmpp.android.chat.ChatProvider;
import tigase.jaxmpp.android.muc.AndroidRoomsManager;
import tigase.jaxmpp.android.roster.AndroidRosterStore;
import tigase.jaxmpp.android.roster.RosterProvider;
import tigase.jaxmpp.android.xml.ParcelableElement;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Base64;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.StanzaReceivedHandler;
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
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.SoftwareVersionModule;
import tigase.jaxmpp.core.client.xmpp.modules.StreamFeaturesModule;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.AbstractChatManager;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageCarbonsModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageCarbonsModule.CarbonEventType;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.xep0085.ChatState;
import tigase.jaxmpp.core.client.xmpp.modules.chat.xep0085.ChatStateExtension;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceStore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCard;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.ErrorElement;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import tigase.jaxmpp.core.client.xmpp.utils.delay.XmppDelay;
import tigase.jaxmpp.j2se.J2SEPresenceStore;
import tigase.jaxmpp.j2se.J2SESessionObject;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
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
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

public class JaxmppService extends Service implements ConnectedHandler, DisconnectedHandler {
	
	private class Stub extends IJaxmppService.Stub {

		@Override
		public void preferenceChanged(String key) {
			SharedPreferences prefs = Preferences.getDefaultSharedPreferences(JaxmppService.this);
			JaxmppService.this.prefChangeListener.onSharedPreferenceChanged(prefs, key);
		}
		
		@Override
		public void updateConfiguration() throws RemoteException {
			JaxmppService.this.updateJaxmppInstances();
		}
		
		@Override
		public boolean connect(String accountJidStr) throws RemoteException {
			if (accountJidStr == null || accountJidStr.length() == 0) {
				Log.v(TAG, "Connecting all accounts..");
				started = true;
				JaxmppService.this.startService(new Intent(JaxmppService.this, JaxmppService.class));
				connectAllJaxmpp(10L);
				return true;
			}
			BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
			Log.v(TAG, "Checking account " + accountJidStr);
			Jaxmpp jaxmpp = multiJaxmpp.get(accountJid);
			if (jaxmpp == null || jaxmpp.isConnected())
				return false;
			Log.v(TAG, "Connecting account " + accountJidStr);
			connectJaxmpp(jaxmpp, 10L);
			return true;
		}

		@Override
		public boolean disconnect(String accountJidStr) throws RemoteException {
			if (accountJidStr == null || accountJidStr.length() == 0) {
				Log.v(TAG, "Disconnecting all accounts..");
				started = false;
				disconnectAllJaxmpp(true);
				JaxmppService.this.stopService(new Intent(JaxmppService.this, JaxmppService.class));
				return true;
			}
			BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
			Log.v(TAG, "Checking account " + accountJidStr);
			Jaxmpp jaxmpp = multiJaxmpp.get(accountJid);
			if (jaxmpp == null || !jaxmpp.isConnected())
				return false;
			Log.v(TAG, "Disconnecting account " + accountJidStr);
			disconnectJaxmpp(jaxmpp, true);
			return true;
		}		
		
		@Override
		public boolean isStarted() {
			return started;
		}
		
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
			if (result == null) {
				result = new CPresence(null, null, CPresence.OFFLINE, null);
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
				if (jaxmpp == null)
					return false;
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
			return sendMessageExt(accountJidStr, jidStr, threadId, body, null);
		}
		
		@Override
		public boolean sendMessageExt(String accountJidStr, String jidStr, String threadId,
				String body, List<ParcelableElement> additionalElems) throws RemoteException {
			try {
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);		
				JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				MessageModule messageModule = jaxmpp.getModule(MessageModule.class);	
				JID jid = JID.jidInstance(jidStr);
				Chat chat = messageModule.getChatManager().getChat(jid, threadId);
				if (chat != null) {
					messageModule.sendMessage(chat, body, additionalElems);
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

		public void closeChat(String accountJidStr, String jidStr, String threadId)
					throws RemoteException {
			try {
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
				JID jid = JID.jidInstance(jidStr);			
				JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				MessageModule messageModule = jaxmpp.getModule(MessageModule.class);	
				Chat chat = messageModule.getChatManager().getChat(jid, threadId);
				if (chat != null)
					messageModule.close(chat);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "EXCEPTION", e);
			}		
		}

		@Override
		public void updateRosterItem(String accountJidStr, final String jidStr,
				final String name, final List<String> groups, final boolean requestAuth,
				final RosterUpdateCallback callback) throws RemoteException {
			try {
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
				final JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				final BareJID jid = BareJID.bareJIDInstance(jidStr);
				new Thread() { 
					public void run() {
						
						try {
				jaxmpp.getModule(RosterModule.class).getRosterStore().add(jid, name, groups, new AsyncCallback() {

					@Override
					public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
						//dialog.cancel();
//						if (error == null)
//							WarningDialog.showWarning(ContactEditActivity.this, R.string.contact_edit_wrn_unkown);
//						else
//							WarningDialog.showWarning(ContactEditActivity.this, error.name());
						try {
							callback.onFailure((error != null) ? error.name() : null);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					@Override
					public void onSuccess(Stanza responseStanza) throws JaxmppException {
						if (requestAuth) {
							jaxmpp.getModule(PresenceModule.class).subscribe(JID.jidInstance(jid));
						}
						try {
							callback.onSuccess(null);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					@Override
					public void onTimeout() throws JaxmppException {
						//dialog.cancel();
						//WarningDialog.showWarning(ContactEditActivity.this, R.string.contact_edit_wrn_timeout);
						//getActivity().onBackPressed();
						try {
							callback.onFailure("timeout");
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
						}
						catch (Exception e) {
							Log.e(TAG, "EXCEPTION", e);
							try {
								callback.onFailure(e.getMessage());
							} catch (RemoteException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}
				}.start();
			}
			catch (Exception e) {
				Log.e(TAG, "EXCEPTION", e);
				callback.onFailure(e.getMessage());
			}
		}

		@Override
		public boolean hasStreamFeature(String accountJidStr, String elemName, String streamFeatureXmlns)
				throws RemoteException {
			try {
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);		
				JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				final Element sf = StreamFeaturesModule.getStreamFeatures(jaxmpp.getSessionObject());
				if (sf == null) return false;
				Element m = sf.getChildrenNS(elemName, streamFeatureXmlns);
				if (m == null)
					return false;
				return true;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "EXCEPTION", e);
			}
			return false;
		}

		@Override
		public boolean joinRoom(String accountJidStr, String roomJidStr,
				String nickname, String password, String action)
				throws RemoteException {
			try {
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
				JID roomJid = JID.jidInstance(roomJidStr);			
				JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				MucModule mucModule = jaxmpp.getModule(MucModule.class);	
				Room room = mucModule.getRoom(roomJid.getBareJid());
				boolean chatExists = room != null;
				if (!chatExists) {
					room = mucModule.join(roomJid.getLocalpart(), roomJid.getDomain(), nickname, password);
					Intent intent = new Intent(action);
					intent.putExtra("account", accountJidStr);
					intent.putExtra("jid", roomJid.toString());
					// is this ok? should be ok!
					intent.putExtra("roomId", room.getId());
					JaxmppService.this.sendBroadcast(intent);
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
		public boolean leaveRoom(String accountJidStr, String roomJidStr)
				throws RemoteException {
			try {
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
				JID roomJid = JID.jidInstance(roomJidStr);			
				JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				MucModule mucModule = jaxmpp.getModule(MucModule.class);
				Room room = mucModule.getRoom(roomJid.getBareJid());
				boolean chatExists = room != null;
				if (chatExists) {
					mucModule.leave(room);
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
		public boolean sendRoomMessage(String accountJidStr, String roomJidStr, String msg) {
			try {
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
				JID roomJid = JID.jidInstance(roomJidStr);			
				JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				MucModule mucModule = jaxmpp.getModule(MucModule.class);
				Room room = mucModule.getRoom(roomJid.getBareJid());
				boolean chatExists = room != null;
				if (chatExists) {
					room.sendMessage(msg);
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
		public Occupant[] getRoomOccupants(String accountJidStr, String roomJidStr)
				throws RemoteException {
			try {
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
				JID roomJid = JID.jidInstance(roomJidStr);			
				JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				MucModule mucModule = jaxmpp.getModule(MucModule.class);
				Room room = mucModule.getRoom(roomJid.getBareJid());
				Collection<tigase.jaxmpp.core.client.xmpp.modules.muc.Occupant> roomOccupants = room.getPresences().values();
				Occupant[] occupants = new Occupant[roomOccupants.size()];
				int i=0;
				for (tigase.jaxmpp.core.client.xmpp.modules.muc.Occupant ro : roomOccupants) {
					occupants[i] = new Occupant(ro);
					i++;
				}
				return occupants;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "EXCEPTION", e);
			}				
			return new Occupant[0];
		}

		@Override
		public String getRecipientChatState(String accountJidStr, String jidStr, String threadId)
				throws RemoteException {
			try {
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
				JID jid = JID.jidInstance(jidStr);			
				JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				if (jaxmpp == null)
					return "";
				MessageModule messageModule = jaxmpp.getModule(MessageModule.class);	
				Chat chat = messageModule.getChatManager().getChat(jid, threadId);
				if (chat != null) {
					ChatStateExtension ext = messageModule.getExtensionChain().getExtension(ChatStateExtension.class);
					if (ext != null) {
						ChatState state = ext.getRecipientChatState(chat);
						if (state != null) 
							return state.name();
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "EXCEPTION", e);
			}		
			return "";
		}

		@Override
		public void setOwnChatState(String accountJidStr, String jidStr, String threadId, String chatStateStr)
				throws RemoteException {
			// TODO Auto-generated method stub
			try {
				if (chatStateStr == null)
					return;
				ChatState state = ChatState.valueOf(chatStateStr);
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
				JID jid = JID.jidInstance(jidStr);			
				JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				if (jaxmpp == null)
					return;
				MessageModule messageModule = jaxmpp.getModule(MessageModule.class);	
				Chat chat = messageModule.getChatManager().getChat(jid, threadId);
				if (chat != null) {
					ChatStateExtension ext = messageModule.getExtensionChain().getExtension(ChatStateExtension.class);
					if (ext != null) {
						ext.setOwnChatState(chat, state);
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "EXCEPTION", e);
			}			
		}

		@Override
		public void retrieveVCard(String accountJidStr, String jidStr, final XmppCallback callback) throws RemoteException {
			try {
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
				JID jid = JID.jidInstance(jidStr);			
				JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				VCardModule vcardModule = jaxmpp.getModule(VCardModule.class);
				vcardModule.retrieveVCard(jid, new XmppCallbackWrapper(callback));
			} catch (Exception e) {
				Log.e(TAG, "EXCEPTION", e);
			}
		}
		
		@Override
		public void publishVCard(String accountJidStr, final ParcelableElement vcardEl, final XmppCallback callback) throws RemoteException {
			try {
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);	
				JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				final IQ iq = IQ.create();
				iq.setType(StanzaType.set);
				iq.addChild(vcardEl);
				jaxmpp.send(iq, new XmppCallbackWrapper(callback));
			} catch (Exception e) {
				Log.e(TAG, "EXCEPTION", e);
			}			
		}
		
		@Override
		public boolean sendFile(String accountJidStr, String jidStr, String uriStr, String mimetype) {
			try {
				Log.v(TAG, "send file called for account = " + accountJidStr + " for jid " + jidStr);
				BareJID accountJid = BareJID.bareJIDInstance(accountJidStr);
				JID jid = JID.jidInstance(jidStr);			
				JaxmppCore jaxmpp = multiJaxmpp.get(accountJid);
				if (jid.getResource() == null) {
					JID fullJid = FileTransferUtility.getBestJidForFeatures(jaxmpp, jid.getBareJid(), FileTransferUtility.FEATURES);
					if (fullJid != null) {
						jid = fullJid;
					} else {
						Presence p = PresenceModule.getPresenceStore(jaxmpp.getSessionObject()).getBestPresence(jid.getBareJid());
						if (p != null) {
							jid = p.getFrom();
						}
					}
				}
				Uri uri = Uri.parse(uriStr);
				fileTransferFeature.startFileTransfer(JaxmppService.this, jaxmpp, jid, uri, mimetype);
				return true;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "EXCEPTION", e);
				return false;
			}		
		}
	}
	
	private class MessageHandler implements MessageModule.MessageReceivedHandler, MessageCarbonsModule.CarbonReceivedHandler, 
			ChatStateExtension.ChatStateChangedHandler {

		@Override
		public void onMessageReceived(SessionObject sessionObject, Chat chat,
				tigase.jaxmpp.core.client.xmpp.stanzas.Message msg) {
			try {
				storeMessage(sessionObject, chat, msg, true);
			} catch (Exception ex) {
				Log.e(TAG, "Exception handling received message", ex);
			}
		}

		@Override
		public void onCarbonReceived(
				SessionObject sessionObject,
				CarbonEventType carbonType,
				tigase.jaxmpp.core.client.xmpp.stanzas.Message msg,
				Chat chat) {
			try {
				storeMessage(sessionObject, chat, msg, false);
			} catch (Exception ex) {
				Log.e(TAG, "Exception handling received carbon message", ex);
			}			
		}

		@Override
		public void onChatStateChanged(SessionObject sessionObject, Chat chat,
				ChatState state) {
			try {
				Log.v(TAG, "received chat state chaged event for " + chat.getJid().toString() + ", new state = " + state);
				Uri uri = chat != null
						? Uri.parse(org.tigase.messenger.phone.pro.db.providers.OpenChatsProvider.OPEN_CHATS_URI + "/" + chat.getId())
						: Uri.parse(org.tigase.messenger.phone.pro.db.providers.OpenChatsProvider.OPEN_CHATS_URI);
				context.getContentResolver().notifyChange(uri, null);				
			} catch (Exception ex) {
				Log.e(TAG, "Exception handling received chat state change event", ex);
			}			
		}
		
	}
	
	private class MucHandler implements MucModule.MucMessageReceivedHandler, MucModule.YouJoinedHandler, 
			MucModule.MessageErrorHandler, MucModule.StateChangeHandler {

		@Override
		public void onMucMessageReceived(SessionObject sessionObject,
				tigase.jaxmpp.core.client.xmpp.stanzas.Message msg,
				Room room, String nickname, Date timestamp) {
			try {
				if (msg == null || msg.getBody() == null || room == null)
					return;
				String body = msg.getBody();
				Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + Uri.encode(room.getRoomJid().toString()));
				
				ContentValues values = new ContentValues();
				values.put(ChatTableMetaData.FIELD_JID, room.getRoomJid().toString());
				values.put(ChatTableMetaData.FIELD_AUTHOR_NICKNAME, nickname);
				values.put(ChatTableMetaData.FIELD_TIMESTAMP, timestamp.getTime());
				values.put(ChatTableMetaData.FIELD_BODY, body);
				values.put(ChatTableMetaData.FIELD_STATE, 0);
				values.put(ChatTableMetaData.FIELD_ACCOUNT, sessionObject.getUserBareJid().toString());
				
				getContentResolver().insert(uri, values);
				
				if (activeChatJid == null || !activeChatJid.getBareJid().equals(room.getRoomJid())) {
					if (body.toLowerCase().contains(room.getNickname().toLowerCase())) {
						notificationHelper.notifyNewMucMessage(sessionObject, msg);
					}
				}
			} catch (Exception ex) {
				Log.e(TAG, "Exception handling received MUC message", ex);
			}
			
		}

		@Override
		public void onMessageError(SessionObject sessionObject,
				tigase.jaxmpp.core.client.xmpp.stanzas.Message message,
				Room room, String nickname, Date timestamp) {
			try {
				Log.e(TAG, "Error from room " + room.getRoomJid() + ", error = " + message.getAsString());
			} catch (XMLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onYouJoined(SessionObject sessionObject, Room room,
				String asNickname) {
			// TODO Auto-generated method stub
			Log.v(TAG, "joined room " + room.getRoomJid() + " as " + asNickname);
		}

		@Override
		public void onStateChange(SessionObject sessionObject, Room room,
				tigase.jaxmpp.core.client.xmpp.modules.muc.Room.State oldState,
				tigase.jaxmpp.core.client.xmpp.modules.muc.Room.State newState) {
			Log.v(TAG, "room " + room.getRoomJid()  + " changed state from " + oldState + " to " + newState);
			int state = CPresence.OFFLINE;
			switch (newState) {
				case joined:
					state = CPresence.ONLINE;
					break;
				default:
					state = CPresence.OFFLINE;
			}
			chatProvider.updateRoomState(sessionObject, room.getRoomJid(), state);
		}
		
	}
	
	private class PresenceHandler implements PresenceModule.ContactAvailableHandler, 
		PresenceModule.ContactUnavailableHandler, PresenceModule.ContactChangedPresenceHandler,
		PresenceModule.BeforePresenceSendHandler {

		private final JaxmppService jaxmppService;
		
		public PresenceHandler(JaxmppService jaxmppService) {
			this.jaxmppService = jaxmppService;
		}
		
		@Override
		public void onContactChangedPresence(SessionObject sessionObject,
				Presence stanza, JID jid, Show show, String status,
				Integer priority) throws JaxmppException {
			updateRosterItem(sessionObject, stanza);
			rosterProvider.updateStatus(sessionObject, jid);
		}

		@Override
		public void onContactUnavailable(SessionObject sessionObject,
				Presence stanza, JID jid, String status) {
			try {
				updateRosterItem(sessionObject, stanza);
			}
			catch (JaxmppException ex) {
				Log.v(TAG, "Exception updating roster item presence", ex);
			}
			rosterProvider.updateStatus(sessionObject, jid);		
		}

		@Override
		public void onContactAvailable(SessionObject sessionObject,
				Presence stanza, JID jid, Show show, String status,
				Integer priority) throws JaxmppException {
			updateRosterItem(sessionObject, stanza);
			rosterProvider.updateStatus(sessionObject, jid);
		}

		@Override
		public void onBeforePresenceSend(SessionObject sessionObject,
				Presence presence) throws JaxmppException {
			presence.setStatus(userStatusMessage);
			if (focused) {
				presence.setShow(userStatusShow);
				presence.setPriority(prefs.getInt(Preferences.DEFAULT_PRIORITY_KEY, 5));
			} else {
				presence.setShow(Show.away);
				presence.setPriority(prefs.getInt(Preferences.AWAY_PRIORITY_KEY, 0));
			}
			activityFeature.beforePresenceSend(prefs, presence);
		}
		
	}
	
	private class StreamHandler implements DiscoveryModule.ServerFeaturesReceivedHandler {

		@Override
		public void onServerFeaturesReceived(final SessionObject sessionObject,
				IQ stanza, String[] featuresArr) {
			Set<String> features = new HashSet<String>(Arrays.asList(featuresArr));
			if (features.contains(MessageCarbonsModule.XMLNS_MC)) {
				MessageCarbonsModule mc = multiJaxmpp.get(sessionObject).getModule(MessageCarbonsModule.class);
				// if we decide to disable MessageCarbons for some account we may not create module 
				// instance at all, so better be prepared for null here
				if (mc != null) {
					try {
						mc.enable(new AsyncCallback() {
							@Override
							public void onError(Stanza responseStanza,
									ErrorCondition error)
									throws JaxmppException {
								Log.v(TAG,
										"MessageCarbons for account "
												+ sessionObject
														.getUserBareJid()
														.toString()
												+ " activation failed = "
												+ error.toString());
							}

							@Override
							public void onSuccess(Stanza responseStanza)
									throws JaxmppException {
								Log.v(TAG, "MessageCarbons for account "
										+ sessionObject.getUserBareJid()
												.toString()
										+ " activation succeeded");
							}

							@Override
							public void onTimeout() throws JaxmppException {
								Log.v(TAG, "MessageCarbons for account "
										+ sessionObject.getUserBareJid()
												.toString()
										+ " activation timeout");
							}

						});
					} catch (JaxmppException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
	}
	
	public static final int SEND_MESSAGE = 1;
	public static final String CLIENT_FOCUS = "org.tigase.messenger.phone.pro.CLIENT_FOCUS";

	public static final String ACTION_FILETRANSFER = "org.tigase.messenger.phone.pro.service.JaxmppService.FILE_TRANSFER";
	private static final String ACTION_KEEPALIVE = "org.tigase.messenger.phone.pro.service.JaxmppService.KEEP_ALIVE";
	private static final String TAG = "JaxmppService";
	private static final StanzaExecutor executor = new StanzaExecutor();
	
	protected static Show userStatusShow = Show.online;
	protected static String userStatusMessage = null;
	public static Context context = null;
	
	protected final Timer timer = new Timer();
	
	// do we need this any more?
	private Map<BareJID,Chat> chats = new HashMap<BareJID,Chat>();
	private MultiJaxmpp multiJaxmpp = new MultiJaxmpp();
	private ConnectivityManager connManager;
	
	private HashSet<SessionObject> locked = new HashSet<SessionObject>();
	private int usedNetworkType = -1;	
	
	private AccountModifyReceiver accountModifyReceiver = new AccountModifyReceiver();
	private ClientFocusReceiver clientFocusReceiver = new ClientFocusReceiver();
	private CapabilitiesDBCache capsCache = null;
	protected DatabaseHelper dbHelper = null;
	private boolean focused = true;
	private MessageHandler messageHandler = null;
	private MobileModeFeature mobileModeFeature = null;
	private MucHandler mucHandler = null;
	protected NotificationHelper notificationHelper = null;
	private PresenceHandler presenceHandler = null;
	private RosterProviderExt rosterProvider = null;
	private ChatProvider chatProvider = null;
	
	private boolean reconnect = true;
	private JID activeChatJid = null;
	
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
	
	private class ClientFocusReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String chatJid = intent.getStringExtra("chat");
			if (chatJid != null) {
				activeChatJid = chatJid.length() == 0 ? null : JID.jidInstance(chatJid);
			}
			if (intent.hasExtra("focus")) {
				focused = intent.getBooleanExtra("focus", false);
				if (focused) {
					sendAutoPresence(false);
				}
				else {
					sendAutoPresence(true);
				}
			}
		}
		
	}
	
	private class ConnReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			NetworkInfo netInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
			onNetworkChange(netInfo);
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
					final MessageModule messageModule = jaxmpp.getModule(MessageModule.class);
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
								messageModule.sendMessage(ch, body);
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
	
	final Messenger messenger = new Messenger(new IncomingHandler());

	private ActivityFeature activityFeature;
	private ConnReceiver connReceiver;
	private FileTransferFeature fileTransferFeature;
	private GeolocationFeature geolocationFeature;
	private OnSharedPreferenceChangeListener prefChangeListener;
	protected SharedPreferences prefs;
	private int keepaliveInterval;
	private ScreenStateReceiver screenStateReceiver;
	private boolean started = false;
	private TimerTask autoPresenceTask;
	private SSLSocketFactory sslSocketFactory;
	private StreamHandler streamHandler;

	public MultiJaxmpp getMulti() {
		return multiJaxmpp;
	}
	
	@Override
	public void onCreate() {
		context = this;
		
        // workaround for https://code.google.com/p/android/issues/detail?id=20915
        try {
            Class.forName("android.os.AsyncTask");
        } catch (ClassNotFoundException e) {
        }
		
        AvatarHelper.initilize(context);

    	if (geolocationFeature == null) {
    		geolocationFeature = new GeolocationFeature(this);
    		geolocationFeature.onStart();
    	}
    	
		// Android from API v8 contains optimized SSLSocketFactory
		// which reduces network usage for handshake
		SSLSessionCache sslSessionCache = new SSLSessionCache(this);
		sslSocketFactory = SSLCertificateSocketFactory.getDefault(0, sslSessionCache);
        
        mobileModeFeature = new MobileModeFeature(this);
        
		setUsedNetworkType(-1);
		this.prefChangeListener = new OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				// is this code called?
				Log.v(TAG, "key = " + key);
				if (Preferences.KEEPALIVE_TIME_KEY.equals(key)) {
					Log.v(TAG, "keepalive timout changed");
					keepaliveInterval = 1000 * 60 * sharedPreferences.getInt(
							key, 3);
					stopKeepAlive();
					keepAlive();
					startKeepAlive();
				}
				Log.v("STREAM", "got preference change = " + key);
				if (Preferences.ENABLE_CHAT_STATE_SUPPORT_KEY.equals(key)) {
					Log.v(TAG, "changed chat state support - calling update of jaxmpp states");
					updateJaxmppInstances();
				}
				if (activityFeature != null) {
					activityFeature.onSharedPreferenceChanged(sharedPreferences, key);
				}
			}
		};		
		//this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		this.prefs = Preferences.getDefaultSharedPreferences(this);
		this.prefs.registerOnSharedPreferenceChangeListener(prefChangeListener);		

        if (activityFeature == null) {
        	activityFeature = new ActivityFeature(this);
        	activityFeature.onStart();
        }		
        if (fileTransferFeature == null) {
        	fileTransferFeature = new FileTransferFeature(this);
        }
		
		keepaliveInterval = 1000 * 60 * this.prefs.getInt(Preferences.KEEPALIVE_TIME_KEY, 3);
		
		this.connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		this.connReceiver = new ConnReceiver();
		IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
		registerReceiver(connReceiver, filter);
		filter = new IntentFilter(CLIENT_FOCUS);
		registerReceiver(clientFocusReceiver, filter);
		filter = new IntentFilter(AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION);
		registerReceiver(accountModifyReceiver, filter);
		screenStateReceiver = new ScreenStateReceiver();
		filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenStateReceiver, filter);

		this.dbHelper = new DatabaseHelper(this);
		this.rosterProvider = new RosterProviderExt(this, dbHelper, new RosterProvider.Listener() {		
			@Override
			public void onChange(Long rosterItemId) {
				Uri uri = rosterItemId != null 
						? Uri.parse(org.tigase.messenger.phone.pro.db.providers.RosterProvider.CONTENT_URI + "/" + rosterItemId)
						: Uri.parse(org.tigase.messenger.phone.pro.db.providers.RosterProvider.CONTENT_URI);
				//Log.v(TAG, "Notifing about changed roster item with id = " + rosterItemId + " - " + uri.toString());
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
		chatProvider.resetRoomState(CPresence.OFFLINE);
		this.mucHandler = new MucHandler();
		this.streamHandler = new StreamHandler();
		this.capsCache = new CapabilitiesDBCache(dbHelper);
		
		notificationHelper = NotificationHelper.createInstance(context);
		
		multiJaxmpp.addHandler(JaxmppCore.ConnectedHandler.ConnectedEvent.class, this);
		multiJaxmpp.addHandler(JaxmppCore.DisconnectedHandler.DisconnectedEvent.class, this);
		multiJaxmpp.addHandler(DiscoveryModule.ServerFeaturesReceivedHandler.ServerFeaturesReceivedEvent.class, streamHandler);
		multiJaxmpp.addHandler(PresenceModule.ContactAvailableHandler.ContactAvailableEvent.class, presenceHandler);
		multiJaxmpp.addHandler(PresenceModule.ContactUnavailableHandler.ContactUnavailableEvent.class, presenceHandler);
		multiJaxmpp.addHandler(PresenceModule.ContactChangedPresenceHandler.ContactChangedPresenceEvent.class, presenceHandler);
		multiJaxmpp.addHandler(PresenceModule.BeforePresenceSendHandler.BeforePresenceSendEvent.class, presenceHandler);
		multiJaxmpp.addHandler(MessageModule.MessageReceivedHandler.MessageReceivedEvent.class, messageHandler);
		multiJaxmpp.addHandler(MessageCarbonsModule.CarbonReceivedHandler.CarbonReceivedEvent.class, messageHandler);
		multiJaxmpp.addHandler(ChatStateExtension.ChatStateChangedHandler.ChatStateChangedEvent.class, messageHandler);
		multiJaxmpp.addHandler(MucModule.MucMessageReceivedHandler.MucMessageReceivedEvent.class, mucHandler);
		multiJaxmpp.addHandler(MucModule.MessageErrorHandler.MessageErrorEvent.class, mucHandler);
		multiJaxmpp.addHandler(MucModule.YouJoinedHandler.YouJoinedEvent.class, mucHandler);
		multiJaxmpp.addHandler(MucModule.StateChangeHandler.StateChangeEvent.class, mucHandler);
		
		multiJaxmpp.addHandler(Connector.StanzaSendingHandler.StanzaSendingEvent.class, new Connector.StanzaSendingHandler() {
			@Override
			public void onStanzaSending(SessionObject sessionObject,
					Element stanza) throws JaxmppException {
				Log.v("STREAM", "sending stanza = " + stanza.getAsString());
			}			
		});
		multiJaxmpp.addHandler(StanzaReceivedHandler.StanzaReceivedEvent.class, new Connector.StanzaReceivedHandler() {
			@Override
			public void onStanzaReceived(SessionObject sessionObject,
					Element stanza) {
				try {
					Log.v("STREAM", "received stanza = " + stanza.getAsString());
				} catch (XMLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}			
		});
		
		updateJaxmppInstances();
		startKeepAlive();	
		updateServiceNotification();
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
		
		if (connReceiver != null)
			unregisterReceiver(connReceiver);
		if (accountModifyReceiver != null)
			unregisterReceiver(accountModifyReceiver);
		if (clientFocusReceiver != null)
			unregisterReceiver(clientFocusReceiver);
		
		disconnectAllJaxmpp(true);
		stopKeepAlive();
		setUsedNetworkType(-1);
		
		if (geolocationFeature != null) {
        	geolocationFeature.onStop();
        	geolocationFeature = null;			
		}
		
		if (activityFeature != null) {
			activityFeature.onStop();
			activityFeature = null;
		}
		
		super.onDestroy();
		mobileModeFeature = null;
		context = null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
//		if (intent != null && intent.getAction() != null) {
//			
//		}
//		else {
		if (intent != null && "connect-all".equals(intent.getAction())) {
			if (!started) {
				connectAllJaxmpp(null);
				started = true;
			}
		}
		else if (intent != null && ACTION_KEEPALIVE.equals(intent.getAction())) {
			keepAlive();
		}
		else if (intent != null && ACTION_FILETRANSFER.equals(intent.getAction())) {
			fileTransferFeature.processFileTransferAction(intent);
		}
		else if (activityFeature != null) {
			activityFeature.onHandleIntent(intent);
		}
//		}

		return Service.START_STICKY;
	}
	
	@Override
	public void onConnected(SessionObject sessionObject) {
		try {
			Log.v(TAG, "account " + sessionObject.getUserBareJid() + " connected");
			Jaxmpp jaxmpp = multiJaxmpp.get(sessionObject);
			mobileModeFeature.accountConnected(jaxmpp);
			geolocationFeature.accountConnected(jaxmpp);
		} catch (JaxmppException e) {
			Log.e(TAG, "Exception processing MobileModeFeature on connect for account " + sessionObject.getUserBareJid().toString());
		}
		
		try {
			Jaxmpp jaxmpp = multiJaxmpp.get(sessionObject);
			MucModule mucModule = jaxmpp.getModule(MucModule.class);
			for (Room room : mucModule.getRooms()) {
				if (room.getState() != Room.State.joined) {
					room.rejoin();
				}
			}
		} catch (JaxmppException e) {
			Log.e(TAG, "Exception while rejoining to rooms on connect for account " + sessionObject.getUserBareJid().toString());
		}
		
		updateServiceNotification();
	}
	
	@Override
	public void onDisconnected(SessionObject sessionObject) {
		Jaxmpp jaxmpp = multiJaxmpp.get(sessionObject);
		if (reconnect && getUsedNetworkType() != -1) {
			if (jaxmpp != null) {
				this.connectJaxmpp(jaxmpp, 5*1000L);
			}
		}
		geolocationFeature.accountDisconnected(jaxmpp);
		
		updateServiceNotification();
	}

	private void onNetworkChange(final NetworkInfo netInfo) {
		if (netInfo != null && netInfo.isConnected()) {
			setReconnect(true);
			connectAllJaxmpp(5000l);
		}
		else {
			setReconnect(false);
			disconnectAllJaxmpp(false);
		}
	}
	
	@Override
	public void onTrimMemory(int level) {
		ImageHelper.onTrimMemory(level);
	}
	
    protected final State getState(SessionObject object) {
        State state = multiJaxmpp.get(object).getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
        return state == null ? State.disconnected : state;
    }	
    
    private void setReconnect(boolean val) {
    	this.reconnect = val;
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
    			
    			sessionObject.setUserProperty(Connector.TRUST_MANAGERS_KEY, SecureTrustManagerFactory.getTrustManagers(context));
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
    			
    			sessionObject.setUserProperty(SocketConnector.SSL_SOCKET_FACTORY_KEY, sslSocketFactory);    			
    			
    			jaxmpp = new Jaxmpp(sessionObject);
    			jaxmpp.setExecutor(executor);
    			
    			RosterModule.setRosterStore(sessionObject, new AndroidRosterStore(this.rosterProvider));
    			jaxmpp.getModulesManager().register(new RosterModule(this.rosterProvider));
    			PresenceModule.setPresenceStore(sessionObject, new J2SEPresenceStore());
    			jaxmpp.getModulesManager().register(new PresenceModule());
    			AndroidChatManager chatManager = new AndroidChatManager(this.chatProvider);
    			MessageModule messageModule = new MessageModule(chatManager);
    			jaxmpp.getModulesManager().register(messageModule);
    			messageModule.addExtension(new ChatStateExtension(chatManager));
    			try {
    				jaxmpp.getModulesManager().register(new MessageCarbonsModule());
    			} catch (JaxmppException ex) {
    				Log.v(TAG, "Exception creating instance of MessageCarbonsModule", ex);
    			}
    			
    			jaxmpp.getModulesManager().register(new MucModule(new AndroidRoomsManager(this.chatProvider)));
    			jaxmpp.getModulesManager().register(new VCardModule());
    			CapabilitiesModule capsModule = new CapabilitiesModule();
    			capsModule.setCache(capsCache);
    			jaxmpp.getModulesManager().register(capsModule);
    			
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

    		MobileModeFeature.updateSettings(account, jaxmpp, context);
    		Boolean disabled = Boolean.valueOf(am.getUserData(account, "DISABLED"));
    		sessionObject.setUserProperty("CC:DISABLED", disabled);
    		
    		fileTransferFeature.updateSettings(jaxmpp, this);
    		
    		boolean needToSendPresence = false;
    		
    		// updating settings for Chat State Notification
    		final boolean value = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Preferences.ENABLE_CHAT_STATE_SUPPORT_KEY, true);
    		Log.v(TAG, "updating chat state support to =  " + value);
    		needToSendPresence |= (ChatStateExtension.isDisabled(sessionObject) != (value));
    		jaxmpp.getModule(MessageModule.class).getExtensionChain().getExtension(ChatStateExtension.class).setDisabled(!value);
    		needToSendPresence |= geolocationFeature.updateGeolocationSettings(account, jaxmpp, context, this.dbHelper);
   
    		// if we need to send presence (features changed), then go for it
    		// maybe we should clear CAPS module, or reset it?
    		if (needToSendPresence && jaxmpp.isConnected()) {
				jaxmpp.getSessionObject().setProperty(CapabilitiesModule.VERIFICATION_STRING_KEY, null);
    			final JaxmppCore tjaxmpp = jaxmpp;
    			new Thread() {
    				public void run() {
    					try {
							tjaxmpp.getModule(PresenceModule.class).setPresence(userStatusShow, userStatusMessage, null);
						} catch (JaxmppException e) {
							Log.e(TAG, e.getMessage());
						}
    				}
    			}.start();
    		}
    		
    		if (disabled != null && disabled) {
    			if (jaxmpp.isConnected()) {
    				this.disconnectJaxmpp(jaxmpp, true);
    			}
    		}
    		else {
    			if (!jaxmpp.isConnected()) {
    				this.connectJaxmpp(jaxmpp, 1L);
    			}
    		}
    		
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
                            // is this needed any more??
    						//JaxmppService.this.rosterProvider.resetStatus(jaxmpp.getSessionObject());
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
    	setUsedNetworkType(getActiveNetworkType());
    	//geolocationFeature.registerLocationListener();
    	
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
    
    private void disconnectJaxmpp(final Jaxmpp jaxmpp, final boolean cleaning) {
        (new Thread() {
            @Override
            public void run() {
                    try {
                            geolocationFeature.accountDisconnect(jaxmpp);
                            jaxmpp.disconnect(false);
                            // is this needed any more??
                            //JaxmppService.this.rosterProvider.resetStatus(jaxmpp.getSessionObject());
//                            app.clearPresences(j.getSessionObject(), !cleaning);
                    } catch (Exception e) {
                            Log.e(TAG, "cant; disconnect account " + jaxmpp.getSessionObject().getUserBareJid(), e);
                    }
            }
        }).start();    	
    }
    
    private void disconnectAllJaxmpp(final boolean cleaning) {
        setUsedNetworkType(-1);
//        if (geolocationFeature != null) {
//        	geolocationFeature.unregisterLocationListener();
//        }
        final MessengerApplication app = (MessengerApplication) getApplicationContext();
        for (final JaxmppCore j : multiJaxmpp.get()) {
        	disconnectJaxmpp((Jaxmpp) j, cleaning);
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
    
    public boolean isDisabled(SessionObject sessionObject) {
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
    
	protected synchronized void updateRosterItem(final SessionObject sessionObject, final Presence p)
			throws XMLException {
		if (p != null) {
			Element x = p.getChildrenNS("x", "vcard-temp:x:update");
			if (x != null) {
				for (Element c : x.getChildren()) {
					if (c.getName().equals("photo") && c.getValue() != null) {
						boolean retrieve = false;
						final String sha = c.getValue();
						if (sha == null)
							continue;
						retrieve = !rosterProvider.checkVCardHash(sessionObject, p.getFrom().getBareJid(), sha);

						if (retrieve)
							retrieveVCard(sessionObject, p.getFrom().getBareJid());
					}
				}
			}
		}

		// Synchronize contact status
		BareJID from = p.getFrom().getBareJid();
		PresenceStore store = PresenceModule.getPresenceStore(sessionObject);
		Presence bestPresence = store.getBestPresence(from);
		SyncAdapter.syncContactStatus(getApplicationContext(), sessionObject.getUserBareJid(), from, bestPresence);
	}

	private void keepAlive() {
		new Thread() {
			@Override
			public void run() {
				for (JaxmppCore jaxmpp : multiJaxmpp.get()) {
					try {
						if (jaxmpp.isConnected()) {
							jaxmpp.getConnector().keepalive();
							GeolocationFeature.sendQueuedGeolocation(jaxmpp, JaxmppService.this);
						}
					} catch (JaxmppException ex) {
						Log.e(TAG, "error sending keep alive for = "
								+ jaxmpp.getSessionObject().getUserBareJid()
										.toString(), ex);
					}
				}
			}
		}.start();
	}
	
	private void retrieveVCard(final SessionObject sessionObject,
			final BareJID jid) {
		try {
			JaxmppCore jaxmpp = multiJaxmpp.get(sessionObject);
			if (jaxmpp == null)
				return;
			// final RosterItem rosterItem = jaxmpp.getRoster().get(jid);
			jaxmpp.getModule(VCardModule.class).retrieveVCard(
					JID.jidInstance(jid), (long) 3 * 60 * 1000,
					new VCardModule.VCardAsyncCallback() {

						@Override
						public void onError(Stanza responseStanza,
								ErrorCondition error) throws JaxmppException {
						}

						@Override
						public void onTimeout() throws JaxmppException {
						}

						@Override
						protected void onVCardReceived(VCard vcard)
								throws XMLException {
							try {
								if (vcard.getPhotoVal() != null
										&& vcard.getPhotoVal().length() > 0) {
									byte[] buffer = Base64.decode(vcard
											.getPhotoVal());

									rosterProvider.updateVCardHash(sessionObject, jid, buffer);
									Intent intent = new Intent("org.tigase.messenger.phone.pro.AvatarUpdated");
									intent.putExtra("jid", jid.toString());
									JaxmppService.this.sendBroadcast(intent);
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
	
	protected void sendAutoPresence(final boolean delayed) {
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
							final PresenceModule presenceModule = jaxmpp
									.getModule(PresenceModule.class);
							if (jaxmpp.getSessionObject().getProperty(
									Connector.CONNECTOR_STAGE_KEY) == Connector.State.connected)
								presenceModule.setPresence(null, null, null);
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
							final PresenceModule presenceModule = jaxmpp
									.getModule(PresenceModule.class);
							if (jaxmpp.getSessionObject().getProperty(
									Connector.CONNECTOR_STAGE_KEY) == Connector.State.connected)
								presenceModule.setPresence(null, null, null);
						}
					} catch (Exception e) {
						Log.e(TAG, "Can't send auto presence!", e);
					}
				}
			}).start();
		}
	}	
	
	private void startKeepAlive() {
		Intent i = new Intent();
		i.setClass(this, JaxmppService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + keepaliveInterval, keepaliveInterval, pi);
	}
	
	private void stopKeepAlive() {
		Intent i = new Intent();
		i.setClass(this, JaxmppService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.cancel(pi);
	}
	
	private void storeMessage(SessionObject sessionObject, Chat chat, tigase.jaxmpp.core.client.xmpp.stanzas.Message msg, boolean showNotification) throws XMLException {
		// for now let's ignore messages without body element
		if (msg.getBody() == null && msg.getType() != StanzaType.error)
			return;
		BareJID authorJid = msg.getFrom() == null ? sessionObject.getUserBareJid() : msg.getFrom().getBareJid();
		String author = authorJid.toString();
		String jid = null;
		if (chat != null) {
			jid = chat.getJid().getBareJid().toString();
		}
		else {
			jid = (sessionObject.getUserBareJid().equals(authorJid) ? msg.getTo().getBareJid() : authorJid).toString();
		}
		
		Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + Uri.encode(jid));

		ContentValues values = new ContentValues();
		values.put(ChatTableMetaData.FIELD_AUTHOR_JID, author);
		values.put(ChatTableMetaData.FIELD_JID, jid);

		XmppDelay delay = XmppDelay.extract(msg);
		values.put(ChatTableMetaData.FIELD_TIMESTAMP, ((delay == null || delay.getStamp() == null) ? new Date() : delay.getStamp()).getTime());
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
		if (chat != null) {
			values.put(ChatTableMetaData.FIELD_THREAD_ID, chat.getThreadId());
		}
		values.put(ChatTableMetaData.FIELD_ACCOUNT, sessionObject.getUserBareJid().toString());
		
		int type = ChatTableMetaData.ITEM_TYPE_MESSAGE;
		Element geoloc = msg.getChildrenNS("geoloc", "http://jabber.org/protocol/geoloc");
		if (geoloc != null) {
			values.put(ChatTableMetaData.FIELD_DATA, geoloc.getAsString());
			type = ChatTableMetaData.ITEM_TYPE_LOCALITY;
		}
		values.put(ChatTableMetaData.FIELD_ITEM_TYPE, type);
		values.put(ChatTableMetaData.FIELD_STATE, sessionObject.getUserBareJid().equals(authorJid) 
				? ChatTableMetaData.STATE_OUT_SENT : ChatTableMetaData.STATE_INCOMING_UNREAD);	
		
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long id = db.insert(ChatTableMetaData.TABLE_NAME, null, values);
		Log.v(TAG, "inserted message - id = " + id);
		context.getContentResolver().notifyChange(uri, null);
		//context.getContentResolver().insert(uri, values);		
		
		if (!sessionObject.getUserBareJid().equals(authorJid) && showNotification 
				&& (this.activeChatJid == null || !this.activeChatJid.getBareJid().equals(authorJid))) {
			notificationHelper.notifyNewChatMessage(sessionObject, msg);
		}
	}
	
	private void updateServiceNotification() {
		int ico = R.drawable.ic_stat_disconnected;
		String notificationTitle = null;
		String expandedNotificationTitle = null;
		if (getUsedNetworkType() == -1) {
			notificationTitle = getResources().getString(R.string.service_disconnected_notification_title);
			expandedNotificationTitle = getResources().getString(R.string.service_no_network_notification_text);
		}
		else {
			int onlineCount = 0;
			int offlineCount = 0;
			int connectingCount = 0;
			for (JaxmppCore jaxmpp : getMulti().get()) {
				State state = jaxmpp.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
				boolean established = jaxmpp.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID) != null;
				if (!isDisabled(jaxmpp.getSessionObject())) {
					if (state == State.connected && established) {
						++onlineCount;
					} else if (state == null || state == State.disconnected) {
						++offlineCount;
					} else {
						++connectingCount;
					}	
				}
			}
			if (connectingCount > 0) {
				ico = R.drawable.ic_stat_connecting;
				notificationTitle = getResources().getString(R.string.service_connecting_notification_title);
				expandedNotificationTitle = getResources().getString(R.string.service_connecting_notification_text, connectingCount);
			} else if (onlineCount == 0) {
				notificationTitle = getResources().getString(R.string.service_disconnected_notification_title);
				expandedNotificationTitle = getResources().getString(R.string.service_no_active_accounts_notification_text);
			} else {
				ico = R.drawable.ic_stat_connected;
				notificationTitle = getResources().getString(R.string.service_connected_notification_title);
				expandedNotificationTitle = getResources().getString(R.string.service_online_notification_text);
			}
		}
		
		Notification notification = notificationHelper.getForegroundNotification(ico, notificationTitle, expandedNotificationTitle);
		startForeground(NotificationHelper.NOTIFICATION_ID, notification);
	}
	
}
