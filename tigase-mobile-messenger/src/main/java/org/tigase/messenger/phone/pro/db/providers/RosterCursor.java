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
package org.tigase.messenger.phone.pro.db.providers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.JaxmppService;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.Preferences;
import org.tigase.messenger.phone.pro.db.RosterItemsCacheTableExtMetaData;
import org.tigase.messenger.phone.pro.db.RosterTableMetaData;
import org.tigase.messenger.phone.pro.roster.CPresence;

import tigase.jaxmpp.android.roster.RosterItemsCacheTableMetaData;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore.Predicate;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class RosterCursor extends AbstractCursor {

	private final static boolean DEBUG = false;

	// is should not be used - use AvatarHelper class instead
	// private HashMap<RosterItem, byte[]> avatarCache = new HashMap<RosterItem,
	// byte[]>();

	private final String[] COLUMN_NAMES = { RosterTableMetaData.FIELD_ID, RosterTableMetaData.FIELD_JID,
			RosterTableMetaData.FIELD_NAME, RosterTableMetaData.FIELD_ASK, RosterTableMetaData.FIELD_SUBSCRIPTION,
			RosterTableMetaData.FIELD_DISPLAY_NAME, RosterTableMetaData.FIELD_PRESENCE,
			RosterTableMetaData.FIELD_STATUS_MESSAGE, /*
													 * RosterTableMetaData.
													 * FIELD_AVATAR,
													 */RosterTableMetaData.FIELD_ACCOUNT };

	private final String[] columns = { RosterItemsCacheTableMetaData.FIELD_ID, RosterItemsCacheTableMetaData.FIELD_JID,
			RosterItemsCacheTableMetaData.FIELD_NAME, RosterItemsCacheTableMetaData.FIELD_ASK, RosterItemsCacheTableMetaData.FIELD_SUBSCRIPTION,
			RosterItemsCacheTableExtMetaData.FIELD_STATUS, RosterItemsCacheTableMetaData.FIELD_ACCOUNT 
	};
	
	private boolean bound = false;
	
	private final Context context;

	private final SQLiteDatabase db;

	private final Predicate predicate;
	
	private boolean hideOffline;

	private List<Object[]> items = new ArrayList<Object[]>();
	
	private String item = null;
	
	private IJaxmppService jaxmppService;
	
	private ServiceConnection conn = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			jaxmppService = IJaxmppService.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			jaxmppService = null;
		}
		
	};
	
	public RosterCursor(Context ctx, SQLiteDatabase sqLiteDatabase, boolean hideOffline, RosterStore.Predicate predicate, String item) {
		this.context = ctx;
		this.predicate = predicate;
		this.db = sqLiteDatabase;
		this.hideOffline = hideOffline;
		this.item= item;
		bound = ctx.bindService(new Intent(ctx, JaxmppService.class), conn, Context.BIND_AUTO_CREATE);
		loadData();
	}

	private Object get(int column) {
		if (column < 0 || column >= COLUMN_NAMES.length) {
			throw new CursorIndexOutOfBoundsException("Requested column: " + column + ", # of columns: " + COLUMN_NAMES.length);
		}
		if (mPos < 0) {
			throw new CursorIndexOutOfBoundsException("Before first row.");
		}
		if (mPos >= items.size()) {
			throw new CursorIndexOutOfBoundsException("After last row.");
		}
		switch (column) {
		case 0:
			return items.get(mPos)[0];
		case 1:
			return items.get(mPos)[1];
		case 2:
			return items.get(mPos)[2];
		case 3:
			return items.get(mPos)[3];
		case 4:
			return items.get(mPos)[4];
		case 5: {
			return items.get(mPos)[2] == null ? items.get(mPos)[1] : items.get(mPos)[2];
		}
		case 6: {
			return items.get(mPos)[5];
		}
		case 7: {
			Object[] data = items.get(mPos);
			CPresence p = null;
			try {
				p = (jaxmppService == null) ? null : jaxmppService.getBestPresence((String) data[6], (String) data[1]);
			} catch (RemoteException ex) {}
			return (p == null) ? null : p.getDescription();
		}
		case 8: {
			return items.get(mPos)[6];
		}
		default:
			throw new CursorIndexOutOfBoundsException("Unknown column!");
		}
	}

	@Override
	public byte[] getBlob(int column) {
		Object value = get(column);
		return (byte[]) value;
	}

	@Override
	public String[] getColumnNames() {
		return COLUMN_NAMES;
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public double getDouble(int column) {
		Object value = get(column);
		return (value instanceof String) ? Double.valueOf((String) value) : ((Number) value).doubleValue();
	}

	@Override
	public float getFloat(int column) {
		Object value = get(column);
		return (value instanceof String) ? Float.valueOf((String) value) : ((Number) value).floatValue();
	}

	@Override
	public int getInt(int column) {
		Object value = get(column);
		return (value instanceof String) ? Integer.valueOf((String) value) : ((Number) value).intValue();
	}

	@Override
	public long getLong(int column) {
		Object value = get(column);
		return (value instanceof String) ? Long.valueOf((String) value) : ((Number) value).longValue();
	}

	@Override
	public short getShort(int column) {
		Object value = get(column);
		return (value instanceof String) ? Short.valueOf((String) value) : ((Number) value).shortValue();
	}

	@Override
	public String getString(int column) {
		Object s = get(column);
		return s == null ? null : String.valueOf(s);
	}

	@Override
	public boolean isNull(int column) {
		return get(column) == null;
	}

	private final void loadData() {
		hideOffline |= !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Preferences.SHOW_OFFLINE,
				Boolean.TRUE);

		items.clear();
		
		SQLiteQueryBuilder sql = new SQLiteQueryBuilder();
		Predicate pr = predicate;
		String query = null;
		String orderBy = null;
		if (hideOffline) {
			query = RosterItemsCacheTableExtMetaData.FIELD_STATUS + " > 0";
//			pr = new Predicate() {
//				@Override
//				public boolean match(RosterItem item) {
//					try {
//						if (predicate != null && !predicate.match(item))
//							return false;
//						SessionObject session = item.getSessionObject();
//						if (session == null)
//							return false;
//						return session.getPresence().isAvailable(item.getJid());
//					} catch (XMLException e) {
//						return false;
//					}
//				}
//			};
		}

//		ArrayList<RosterItem> r = new ArrayList<RosterItem>();
//		for (JaxmppCore jaxmpp : multi.get()) {
//			r.addAll(jaxmpp.getRoster().getAll(pr));
//		}

		String sorting = PreferenceManager.getDefaultSharedPreferences(context).getString(Preferences.ROSTER_SORTING_KEY,
				"status");

		if ("status".equals(sorting)) {
			orderBy = RosterItemsCacheTableExtMetaData.FIELD_STATUS + "*(-1), " 
						+ RosterItemsCacheTableMetaData.FIELD_NAME + " ASC";
//			MergeSort.sort(r, new Comparator<RosterItem>() {
//
//				@Override
//				public int compare(RosterItem object1, RosterItem object2) {
//					try {
//						CPresence s1 = RosterDisplayTools.getShowOf(object1);
//						CPresence s2 = RosterDisplayTools.getShowOf(object2);
//
//						int sc = s2.getId().compareTo(s1.getId());
//						if (sc != 0)
//							return sc;
//
//						String n1 = RosterDisplayTools.getDisplayName(object1);
//						String n2 = RosterDisplayTools.getDisplayName(object2);
//
//						return n1.compareTo(n2);
//					} catch (Exception e) {
//						return 0;
//					}
//				}
//			});
		} else if ("name".endsWith(sorting)) {
			orderBy = RosterItemsCacheTableMetaData.FIELD_NAME + " ASC";
		}
		
		String[] selectionArgs = null;
		if (item != null) {
			query = RosterItemsCacheTableMetaData.FIELD_ID + " = ? or " + RosterItemsCacheTableMetaData.FIELD_JID + " = ?";
			selectionArgs = new String[] { item, item };
		}
		
		Cursor c = db.query(RosterItemsCacheTableMetaData.TABLE_NAME, columns, query, selectionArgs, null, null, orderBy);
		try {
			while (c.moveToNext()) {
				Object[] data = new Object[columns.length];
				data[0] = c.getLong(0);
				data[1] = c.getString(1);
				data[2] = c.getString(2);
				data[3] = c.getInt(3);
				data[4] = c.getString(4);
				data[5] = c.getInt(5);
				data[6] = c.getString(6);

				items.add(data);
			}			
		}
		catch (Exception ex) {
		}
		finally {
			c.close();			
		}
	}

	// private byte[] readAvatar(RosterItem item) {
	// // if (item.getData("photo") == null)
	// // return null;
	// // if (avatarCache.containsKey(item)) {
	// // if (DEBUG)
	// // Log.d("tigase", "Getting from cache avatar of user " + item.getJid());
	// // return avatarCache.get(item);
	// // }
	// if (DEBUG)
	// Log.d("tigase", "Reading avatar of user " + item.getJid());
	// final Cursor c = db.rawQuery("SELECT * FROM " +
	// VCardsCacheTableMetaData.TABLE_NAME + " WHERE "
	// + VCardsCacheTableMetaData.FIELD_JID + "='" + item.getJid() + "'", null);
	// try {
	// while (c.moveToNext()) {
	// String sha =
	// c.getString(c.getColumnIndex(VCardsCacheTableMetaData.FIELD_HASH));
	// item.setData("photo", sha);
	// byte[] data =
	// c.getBlob(c.getColumnIndex(VCardsCacheTableMetaData.FIELD_DATA));
	// // avatarCache.put(item, data);
	// return data;
	// }
	// return null;
	// } finally {
	// c.close();
	// }
	//
	// }

	@Override
	public boolean requery() {
		if (DEBUG)
			Log.d("tigase", "Requery()");
		loadData();
		return super.requery();
	}
	
	@Override
	public void close() {
		super.close();
		if (bound) {
			context.unbindService(conn);
			bound = false;
		}
	}
}
