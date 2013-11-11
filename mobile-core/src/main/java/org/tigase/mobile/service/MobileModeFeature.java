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

import java.util.TimerTask;

import org.tigase.mobile.Features;

import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.DefaultElement;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;

public class MobileModeFeature {

	public static final String MOBILE_OPTIMIZATIONS_ENABLED = Features.MOBILE_V1 + "#enabled";

	public static final String MOBILE_OPTIMIZATIONS_QUEUE_TIMEOUT = Features.MOBILE_V1 + "#presence_queue_timeout";

	private static final String TAG = "MobileModeFeature";

	public static void updateSettings(Account account, JaxmppCore jaxmpp, Context context) {
		AccountManager accountManager = AccountManager.get(context);
		String valueStr = accountManager.getUserData(account, MobileModeFeature.MOBILE_OPTIMIZATIONS_ENABLED);
		boolean mobileOptimizations = (valueStr == null || Boolean.parseBoolean(valueStr));
		SessionObject sessionObject = jaxmpp.getSessionObject();
		sessionObject.setUserProperty(MobileModeFeature.MOBILE_OPTIMIZATIONS_ENABLED, mobileOptimizations);
	}

	private final JaxmppService jaxmppService;

	private boolean mobileModeEnabled = false;

	private TimerTask setMobileModeTask;

	public MobileModeFeature(JaxmppService service) {
		jaxmppService = service;
	}

	public void accountConnected(JaxmppCore jaxmpp) throws JaxmppException {
		if (mobileModeEnabled) {
			setMobileMode(jaxmpp, mobileModeEnabled);
		}
	}

	protected void setMobileMode(final boolean enable) {
		if (setMobileModeTask != null) {
			setMobileModeTask.cancel();
			setMobileModeTask = null;
		}

		Log.v(TAG, "setting mobile mode to = " + enable);

		mobileModeEnabled = enable;

		if (enable) {
			setMobileModeTask = new TimerTask() {

				@Override
				public void run() {
					setMobileModeTask = null;
					try {
						for (JaxmppCore jaxmpp : jaxmppService.getMulti().get()) {
							setMobileMode(jaxmpp, enable);
						}
					} catch (Exception e) {
						Log.e(TAG, "Can't set mobile mode!", e);
					}
				}
			};
			jaxmppService.timer.schedule(setMobileModeTask, 1000 * 60);
		} else {
			(new Thread() {
				@Override
				public void run() {
					try {
						for (JaxmppCore jaxmpp : jaxmppService.getMulti().get()) {
							setMobileMode(jaxmpp, enable);
						}
					} catch (Exception e) {
						Log.e(TAG, "Can't set mobile mode!", e);
					}
				}
			}).start();
		}
	}

	protected void setMobileMode(JaxmppCore jaxmpp, boolean enable) throws JaxmppException {
		if (jaxmpp.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY) == Connector.State.connected) {
			final Element sf = jaxmpp.getSessionObject().getStreamFeatures();
			if (sf == null)
				return;

			String xmlns = null;
			Element m = sf.getChildrenNS("mobile", Features.MOBILE_V3);
			if (m != null) {
				xmlns = Features.MOBILE_V3;
			}
			else {
				m = sf.getChildrenNS("mobile", Features.MOBILE_V2);
				if (m != null) {
					xmlns = Features.MOBILE_V2;
				} else {
					m = sf.getChildrenNS("mobile", Features.MOBILE_V1);
					if (m != null) {
						xmlns = Features.MOBILE_V1;
					}
				}
			}
			if (xmlns == null || (enable && !((Boolean) jaxmpp.getSessionObject().getProperty(MOBILE_OPTIMIZATIONS_ENABLED))))
				return;

			IQ iq = IQ.create();
			iq.setType(StanzaType.set);
			Element mobile = new DefaultElement("mobile");
			mobile.setXMLNS(xmlns);
			mobile.setAttribute("enable", String.valueOf(enable));
			if (Features.MOBILE_V1.equals(xmlns)) {
				Integer timeout = jaxmpp.getSessionObject().getProperty(MOBILE_OPTIMIZATIONS_QUEUE_TIMEOUT);
				if (timeout != null) {
					timeout = timeout * 60 * 1000;
					mobile.setAttribute("timeout", String.valueOf(timeout));
				}
			}
			iq.addChild(mobile);
			jaxmpp.send(iq);
		}
	}

}
