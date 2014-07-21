package org.tigase.messenger.phone.pro.service;

import tigase.jaxmpp.android.xml.ParcelableElement;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import android.os.RemoteException;
import android.util.Log;

public class AsyncXmppCallback extends XmppCallback.Stub {
	
	private static final String TAG = "AsyncCallback";

	private final tigase.jaxmpp.core.client.AsyncCallback callback;
	
	public AsyncXmppCallback(tigase.jaxmpp.core.client.AsyncCallback callback) {
		this.callback = callback;
	}
	
	@Override
	public void onSuccess(ParcelableElement elem) throws RemoteException {
		try {
			Stanza stanza = Stanza.create(elem);
			this.callback.onSuccess(stanza);
		} catch (JaxmppException e) {
			try {
				Log.e(TAG, "exception processing stanza = " + elem.getAsString(), e);
			} catch (XMLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	@Override
	public void onError(ParcelableElement elem, String type)
			throws RemoteException {
		try {
			Stanza stanza = Stanza.create(elem);
			this.callback.onError(stanza, ErrorCondition.valueOf(type));
		} catch (JaxmppException e) {
			try {
				Log.e(TAG, "exception processing error stanza = " + elem.getAsString(), e);
			} catch (XMLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	@Override
	public void onTimeout() throws RemoteException {
		try {
			this.callback.onTimeout();
		} catch (JaxmppException e) {
			Log.e(TAG, "exception processing timeout", e);
		}
	}

}
