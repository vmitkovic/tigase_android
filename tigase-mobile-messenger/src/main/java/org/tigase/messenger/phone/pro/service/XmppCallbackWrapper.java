package org.tigase.messenger.phone.pro.service;

import android.os.RemoteException;
import tigase.jaxmpp.android.xml.ParcelableElement;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

public class XmppCallbackWrapper implements tigase.jaxmpp.core.client.AsyncCallback {

	private final XmppCallback callback;
	
	public XmppCallbackWrapper(XmppCallback callback) {
		this.callback = callback;
	}

	@Override
	public void onError(Stanza responseStanza, ErrorCondition error)
			throws JaxmppException {
		try {
			callback.onError(ParcelableElement.fromElement(responseStanza), error.name());
		} catch (RemoteException ex) {
			throw new JaxmppException(ex);
		}
	}

	@Override
	public void onSuccess(Stanza responseStanza) throws JaxmppException {
		try {
			callback.onSuccess(ParcelableElement.fromElement(responseStanza));
		} catch (RemoteException ex) {
			throw new JaxmppException(ex);
		}
	}

	@Override
	public void onTimeout() throws JaxmppException {
		try {
			callback.onTimeout();
		} catch (RemoteException ex) {
			throw new JaxmppException(ex);
		}
	}

}
