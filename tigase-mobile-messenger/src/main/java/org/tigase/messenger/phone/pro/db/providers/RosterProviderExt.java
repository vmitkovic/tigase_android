package org.tigase.messenger.phone.pro.db.providers;

import org.tigase.messenger.phone.pro.db.RosterItemsCacheTableExtMetaData;
import org.tigase.messenger.phone.pro.roster.CPresence;

import tigase.jaxmpp.android.roster.RosterItemsCacheTableMetaData;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class RosterProviderExt extends tigase.jaxmpp.android.roster.RosterProvider {
	
	public RosterProviderExt(Context context, SQLiteOpenHelper dbHelper, Listener listener,
			String versionKeyPrefix) {
		super(context, dbHelper, listener, versionKeyPrefix);
	}
	
	public void updateStatus(SessionObject sessionObject, JID jid) {
		long id = createId(sessionObject, jid.getBareJid());
		int status = 0;
		try {
			Presence p = PresenceModule.getPresenceStore(sessionObject).getBestPresence(jid.getBareJid());
			if (p != null) {
				status = CPresence.getStatusFromPresence(p);
			}
		} catch (XMLException ex) {				
		}
		
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(RosterItemsCacheTableExtMetaData.FIELD_STATUS, status);
		db.update(RosterItemsCacheTableMetaData.TABLE_NAME, values, RosterItemsCacheTableMetaData.FIELD_ID + " = ?", 
				new String[] { String.valueOf(id) });
		if (listener != null) {
			listener.onChange(id);
		}
	}
	
	public void resetStatus() {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(RosterItemsCacheTableExtMetaData.FIELD_STATUS, CPresence.OFFLINE);
		db.update(RosterItemsCacheTableMetaData.TABLE_NAME, values, null, null);		
		if (listener != null) {
			listener.onChange(null);
		}
	}
	
	private long createId(SessionObject sessionObject, BareJID jid) {
		return (sessionObject.getUserBareJid() + "::" + jid).hashCode();
	}	
}
