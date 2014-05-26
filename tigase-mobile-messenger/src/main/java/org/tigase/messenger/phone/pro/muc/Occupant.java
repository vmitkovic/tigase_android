package org.tigase.messenger.phone.pro.muc;

import org.tigase.messenger.phone.pro.roster.CPresence;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Affiliation;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Role;
import tigase.jaxmpp.core.client.xmpp.modules.muc.XMucUserElement;
import android.os.Parcel;
import android.os.Parcelable;

public class Occupant implements Parcelable {

	private final String nickname;
	private final Affiliation affiliation;
	private final Role role;
	private final CPresence presence;
	private final JID jid;
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(nickname);
		dest.writeString(affiliation.name());
		dest.writeString(role.name());
		dest.writeParcelable(presence, flags);
		dest.writeString(jid == null ? "" : jid.toString());
	}

	public static final Parcelable.Creator<Occupant> CREATOR = new Parcelable.Creator<Occupant>() {
		public Occupant createFromParcel(Parcel in) {
			return new Occupant(in);
		}

		public Occupant[] newArray(int size) {
			return new Occupant[size];
		}
	};
	
	private Occupant(Parcel in) {
		nickname = in.readString();
		affiliation = Affiliation.valueOf(in.readString());
		role = Role.valueOf(in.readString());
		presence = in.readParcelable(CPresence.class.getClassLoader());
		String jidStr = in.readString();
		if (jidStr.length() > 0)
			jid = JID.jidInstance(jidStr);
		else
			jid = null;
	}	
	
	public Occupant(tigase.jaxmpp.core.client.xmpp.modules.muc.Occupant o) throws JaxmppException {
		nickname = o.getNickname();
		affiliation = o.getAffiliation();
		role = o.getRole();
		presence = new CPresence(o.getPresence());
		XMucUserElement xMucUser = XMucUserElement.extract(o.getPresence());
		if (xMucUser != null) {
			jid = xMucUser.getJID();
		} else {
			jid = null;
		}
	}
	
	public String getNickname() {
		return nickname;
	}
	
	public Affiliation getAffiliation() {
		return affiliation;
	}
	
	public Role getRole() {
		return role;
	}
	
	public CPresence getPresence() {
		return presence;
	}
	
	public JID getJid() {
		return jid;
	}
}
