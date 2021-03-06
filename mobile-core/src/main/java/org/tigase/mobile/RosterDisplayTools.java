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

import org.tigase.mobile.roster.CPresence;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceStore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem.Subscription;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import android.util.Log;

public class RosterDisplayTools {

	public static String getDisplayName(final RosterItem item) {
		if (item == null)
			return null;
		else if (item.getName() != null && item.getName().length() != 0) {
			return item.getName();
		} else {
			return item.getJid().toString();
		}
	}

	public static String getDisplayName(SessionObject sessionObject, final BareJID jid) {
		RosterStore roster = sessionObject.getRoster();
		tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem item = roster.get(jid);
		return getDisplayName(item);
	}

	public static CPresence getShowOf(final Presence p) throws XMLException {
		CPresence r = CPresence.offline;
		if (p != null) {
			if (p.getType() == StanzaType.error) {
				r = CPresence.error;
			} else if (p.getType() == StanzaType.unavailable)
				r = CPresence.offline;
			else if (p.getShow() == Show.online)
				r = CPresence.online;
			else if (p.getShow() == Show.away)
				r = CPresence.away;
			else if (p.getShow() == Show.chat)
				r = CPresence.chat;
			else if (p.getShow() == Show.dnd)
				r = CPresence.dnd;
			else if (p.getShow() == Show.xa)
				r = CPresence.xa;
		}
		return r;
	}

	public static CPresence getShowOf(SessionObject sessionObject, final BareJID jid) {
		RosterStore roster = sessionObject.getRoster();
		tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem item = roster.get(jid);
		return getShowOf(item);
	}

	public static CPresence getShowOf(SessionObject sessionObject, final JID jid) {
		RosterStore roster = sessionObject.getRoster();
		PresenceStore presence = sessionObject.getPresence();
		RosterItem ri = roster.get(jid.getBareJid());
		Presence p = presence.getPresence(jid);
		return getShowOf(ri, p);
	}

	public static CPresence getShowOf(final tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem item) {
		return getShowOf(item, null);
	}

	public static CPresence getShowOf(final tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem item, Presence p) {
		try {
			if (item == null)
				return CPresence.notinroster;
			final PresenceStore presence = item.getSessionObject().getPresence();
			if (item.isAsk())
				return CPresence.requested;
			if (item.getSubscription() == Subscription.none || item.getSubscription() == Subscription.from)
				return CPresence.offline_nonauth;
			p = p == null ? presence.getBestPresence(item.getJid()) : p;
			CPresence r = getShowOf(p);
			return r;
		} catch (Exception e) {
			Log.e("tigase", "Can't calculate presence", e);
			return CPresence.error;
		}
	}

	public static String getStatusMessageOf(final tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem item) {
		try {
			if (item == null)
				return null;
			final PresenceStore presence = item.getSessionObject().getPresence();
			Presence p = presence.getBestPresence(item.getJid());
			if (p != null) {
				return p.getStatus();
			} else
				return null;
		} catch (Exception e) {
			Log.e("tigase", "Can't calculate presence", e);
			return null;
		}
	}

}
