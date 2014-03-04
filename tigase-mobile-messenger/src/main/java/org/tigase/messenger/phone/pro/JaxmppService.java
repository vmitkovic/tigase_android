package org.tigase.messenger.phone.pro;

import java.util.HashMap;
import java.util.Map;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.JaxmppCore.ConnectedHandler;
import tigase.jaxmpp.core.client.JaxmppCore.DisconnectedHandler;
import tigase.jaxmpp.core.client.MultiJaxmpp;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;

public class JaxmppService extends Service implements ConnectedHandler, DisconnectedHandler {

	public static final int SEND_MESSAGE = 1;
	
	private Map<BareJID,Chat> chats = new HashMap<BareJID,Chat>();
	
	class IncomingHandler extends Handler {
		
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case SEND_MESSAGE:					
					Bundle data = msg.getData();
					BareJID account = BareJID.bareJIDInstance(data.getString("account"));
					JID to = JID.jidInstance(data.getString("to"));
					final String body = data.getString("message");
					JaxmppCore jaxmpp = jaxmpps.get(account);
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
	
	private MultiJaxmpp jaxmpps;
	
	@Override
	public void onCreate() {
		jaxmpps = new MultiJaxmpp();
		
		jaxmpps.addHandler(JaxmppCore.ConnectedHandler.ConnectedEvent.class, this);
		jaxmpps.addHandler(JaxmppCore.DisconnectedHandler.DisconnectedEvent.class, this);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return messenger.getBinder();
	}

	@Override
	public void onDestroy() {
		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final Jaxmpp jaxmpp = new Jaxmpp();
		jaxmpp.getModulesManager().register(new MessageModule());
		
		jaxmpp.getConnectionConfiguration().setUserJID("test@test.com");
		jaxmpp.getConnectionConfiguration().setUserPassword("password");
		jaxmpps.add(jaxmpp);		
		new Thread() {
			public void run() {
//				try {
//					jaxmpp.login();
//				} catch (JaxmppException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}						
			}
		}.start();

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

}
