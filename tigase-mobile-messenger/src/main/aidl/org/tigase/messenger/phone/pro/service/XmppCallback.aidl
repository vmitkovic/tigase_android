package org.tigase.messenger.phone.pro.service;

import tigase.jaxmpp.android.xml.ParcelableElement;

interface XmppCallback {

	void onSuccess(in ParcelableElement elem);
	void onError(in ParcelableElement elem, String type);
	void onTimeout();

}