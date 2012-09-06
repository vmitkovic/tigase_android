package org.tigase.mobile.jingle;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;

import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.jingle.JingleModule;
import tigase.jaxmpp.core.client.xmpp.modules.jingle.JingleModule.JingleSessionAcceptEvent;
import tigase.jaxmpp.core.client.xmpp.modules.jingle.JingleModule.JingleSessionInfoEvent;
import tigase.jaxmpp.core.client.xmpp.modules.jingle.JingleModule.JingleSessionInitiationEvent;
import tigase.jaxmpp.core.client.xmpp.modules.jingle.JingleModule.JingleSessionTerminateEvent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class JingleService extends Service {

	private final static String TAG = "JingleService";

	private final Listener<JingleSessionInitiationEvent> sessionInitiateListener = new Listener<JingleModule.JingleSessionInitiationEvent>() {

		@Override
		public void handleEvent(JingleSessionInitiationEvent be) throws JaxmppException {
			onSessionInitiateEvent(be);
		}
	};

	private final Listener<JingleSessionAcceptEvent> sesssionAcceptListener = new Listener<JingleSessionAcceptEvent>() {

		@Override
		public void handleEvent(JingleSessionAcceptEvent be) throws JaxmppException {
			onSessionAcceptListener(be);
		}
	};

	private final Listener<JingleSessionInfoEvent> sesssionInfoListener = new Listener<JingleSessionInfoEvent>() {

		@Override
		public void handleEvent(JingleSessionInfoEvent be) throws JaxmppException {
			onSessionInfoEvent(be);
		}
	};

	private final Listener<JingleSessionTerminateEvent> sesssionTerminateListener = new Listener<JingleSessionTerminateEvent>() {

		@Override
		public void handleEvent(JingleSessionTerminateEvent be) throws JaxmppException {
			onSessionTerminateListener(be);
		}
	};

	public JingleService() {
	}

	protected final MultiJaxmpp getMulti() {
		return ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		getMulti().addListener(JingleModule.JingleSessionInitiation, this.sessionInitiateListener);
		getMulti().addListener(JingleModule.JingleSessionAccept, this.sesssionAcceptListener);
		getMulti().addListener(JingleModule.JingleSessionInfo, this.sesssionInfoListener);
		getMulti().addListener(JingleModule.JingleSessionTerminate, this.sesssionTerminateListener);
	}

	@Override
	public void onDestroy() {
		getMulti().removeListener(JingleModule.JingleSessionInitiation, this.sessionInitiateListener);
		getMulti().removeListener(JingleModule.JingleSessionAccept, this.sesssionAcceptListener);
		getMulti().removeListener(JingleModule.JingleSessionInfo, this.sesssionInfoListener);
		getMulti().removeListener(JingleModule.JingleSessionTerminate, this.sesssionTerminateListener);

		super.onDestroy();
	}

	protected void onSessionAcceptListener(JingleSessionAcceptEvent be) {
		Log.i(TAG, "Session Accept Event");

	}

	protected void onSessionInfoEvent(JingleSessionInfoEvent be) {
		Log.i(TAG, "Session Info Event");

	}

	protected void onSessionInitiateEvent(JingleSessionInitiationEvent be) {
		Log.i(TAG, "Session Initiate Event");

	}

	protected void onSessionTerminateListener(JingleSessionTerminateEvent be) {
		Log.i(TAG, "Session Terminate Event");

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand()");
		return super.onStartCommand(intent, flags, startId);
	}

}
