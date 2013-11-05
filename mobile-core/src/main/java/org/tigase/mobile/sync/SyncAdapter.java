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
package org.tigase.mobile.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.tigase.mobile.Constants;
import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.db.RosterCacheTableMetaData;
import org.tigase.mobile.db.VCardsCacheTableMetaData;
import org.tigase.mobile.db.providers.MessengerDatabaseHelper;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.utils.EscapeUtils;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

	final private static class DataQuery {

		public static final int COLUMN_DATA1 = 3;

		public static final int COLUMN_DATA15 = 6;

		public static final int COLUMN_DATA2 = 4;
		public static final int COLUMN_DATA3 = 5;
		public static final int COLUMN_FAMILY_NAME = COLUMN_DATA3;
		public static final int COLUMN_FULL_NAME = COLUMN_DATA1;
		public static final int COLUMN_GIVEN_NAME = COLUMN_DATA2;
		public static final int COLUMN_ID = 0;
		public static final int COLUMN_MIMETYPE = 2;

		public static final int COLUMN_SERVER_ID = 1;

		public static final Uri CONTENT_URI = Data.CONTENT_URI;
		public static final String[] PROJECTION = new String[] { Data._ID, RawContacts.SOURCE_ID, Data.MIMETYPE, Data.DATA1,
				Data.DATA2, Data.DATA3, Data.DATA15 };
		public static final String SELECTION = Data.RAW_CONTACT_ID + "=?";

		private DataQuery() {
		}
	}

	private static class PresenceUpdater implements Runnable {

		private final Context context;

		private boolean scheduled = false;

		public PresenceUpdater(Context context) {
			this.context = context;
		}

		public boolean isScheduled() {
			return scheduled;
		}

		@Override
		public void run() {
			final MultiJaxmpp multiJaxmpp = ((MessengerApplication) context.getApplicationContext()).getMultiJaxmpp();
			final ContentResolver resolver = context.getContentResolver();
			BatchOperation batchOperation = new BatchOperation(context, resolver);
			PresenceEvent be = null;

			AccountManager accountManager = AccountManager.get(context);
			Account[] accounts = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
			Set<BareJID> syncEnabledAccounts = new HashSet<BareJID>();
			if (accounts != null) {
				for (Account acc : accounts) {
					if (ContentResolver.getSyncAutomatically(acc, "com.android.contacts")) {
						syncEnabledAccounts.add(BareJID.bareJIDInstance(acc.name));
					}
				}
			}
			
			// we should schedule next event from now on
			scheduled = false;

			synchronized (SyncAdapter.class) {
			while ((be = presenceEventQueue.poll()) != null) {
				try {
					BareJID buddyJid = be.getJid().getBareJid();
					if (!syncEnabledAccounts.contains(be.getSessionObject().getUserBareJid()))
						continue;
					
					JaxmppCore jaxmpp = multiJaxmpp.get(be.getSessionObject());
					if (jaxmpp == null) {
						if (DEBUG)
							Log.v(TAG, "not setting status for " + buddyJid.toString()
									+ ", reason = no jaxmpp for session object");
						continue;
					}
					
					RosterItem ri = jaxmpp.getRoster().get(buddyJid);
					if (ri == null) {
						if (DEBUG)
							Log.v(TAG, "not setting status for " + buddyJid.toString() + ", reason = no roster item");
						continue;
					}

					long rawContactId = lookupRawContact(resolver, ri.getId());
					if (rawContactId == 0) {
						if (buddyJid.equals(be.getSessionObject().getUserBareJid()))
							continue;
					
						if (DEBUG)
							Log.v(TAG, "not setting status for " + buddyJid.toString() + ", reason = contact not synchronized");
						forceContactsSync.add(jaxmpp.getSessionObject().getUserBareJid());					
						continue;
					}

					Presence p = jaxmpp.getPresence().getBestPresence(buddyJid);
					
					ContactOperations.syncStatus(context, be.getSessionObject().getUserBareJid().toString(), rawContactId,
							buddyJid, p, batchOperation);

					// counter++;
					// if (counter >= 1) {
					if (DEBUG)
						Log.v(TAG, "updating status for " + buddyJid.toString() + "");
					// batchOperation.execute();
					// counter = 0;
					// }
					if (batchOperation.size() >= 50) {
						int counter = batchOperation.size();
						batchOperation.execute();
						Log.d(TAG, "updated " + counter + " contacts at once");
					}
				} catch (XMLException e) {
					Log.e(TAG, "WTF??", e);
				}
			}

			int counter = batchOperation.size();
			batchOperation.execute();
			
			Log.d(TAG, "updated " + counter + " contacts at once");
			}
			
			if (!forceContactsSync.isEmpty()) {
				for (Account account : accounts) {
					BareJID accountJid = BareJID.bareJIDInstance(account.name);
					if (syncEnabledAccounts.contains(accountJid) && forceContactsSync.contains(accountJid)) {
						ContentResolver.requestSync(account, "com.android.contacts", new Bundle());
					}
				}
			}
		}

		public void setScheduled(boolean value) {
			this.scheduled = value;
		}

	}

	public static final boolean DEBUG = false;

	private static BlockingQueue<PresenceEvent> presenceEventQueue = null;

	private static PresenceUpdater presenceUpdater = null;

	private static ScheduledThreadPoolExecutor scheduledExecutor = null;
	private static final String SYNC_MARKER_KEY = "org.tigase.mobile.sync.marker";
	private static final String TAG = "SyncAdapter";

	private static Set<BareJID> forceContactsSync = Collections.synchronizedSet(new HashSet<BareJID>());
	
	public static long ensureGroupExists(Context context, String account, String group) {
		final ContentResolver resolver = context.getContentResolver();

		// Lookup the sample group
		long groupId = 0;
		final Cursor cursor = resolver.query(Groups.CONTENT_URI, new String[] { BaseColumns._ID }, Groups.ACCOUNT_NAME
				+ "=? AND " + Groups.ACCOUNT_TYPE + "=? AND " + Groups.TITLE + "=?", new String[] { account,
				Constants.ACCOUNT_TYPE, group }, null);
		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					groupId = cursor.getLong(0);
				}
			} finally {
				cursor.close();
			}
		}

		if (groupId == 0) {
			// Sample group doesn't exist yet, so create it
			final ContentValues contentValues = new ContentValues();
			contentValues.put(Groups.ACCOUNT_NAME, account);
			contentValues.put(Groups.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
			contentValues.put(Groups.TITLE, group);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				contentValues.put(Groups.GROUP_IS_READ_ONLY, true);
			}

			final Uri newGroupUri = resolver.insert(Groups.CONTENT_URI, contentValues);
			groupId = ContentUris.parseId(newGroupUri);
		}

		return groupId;
	}

	public static Uri getAvatarUriFromContacts(ContentResolver resolver, BareJID jid) {
		final Cursor c = resolver.query(DataQuery.CONTENT_URI, new String[] { ContactsContract.RawContacts.CONTACT_ID },
				Data.DATA1 + "=? AND ( " + Data.DATA5 + "= 7 OR " + Data.DATA5 + "= 5 )", new String[] { jid.toString() }, null);
		if (c.moveToNext()) {
			long id = c.getLong(c.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
			c.close();
			return ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
		} else {
			c.close();
			return null;
		}
	}

	private static long lookupRawContact(ContentResolver resolver, long userId) {
		long id = 0;
		final Cursor c = resolver.query(RawContacts.CONTENT_URI, new String[] { BaseColumns._ID }, RawContacts.ACCOUNT_TYPE
				+ "='" + Constants.ACCOUNT_TYPE + "' AND " + RawContacts.SOURCE_ID + "=?",
				new String[] { String.valueOf(userId) }, null);

		try {
			if (c.moveToFirst()) {
				id = c.getLong(0);
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return id;
	}

	public static void syncContactStatus(Context context, PresenceEvent pe) {
		synchronized (TAG) {
			if (scheduledExecutor == null) {
				scheduledExecutor = new ScheduledThreadPoolExecutor(1);
				presenceEventQueue = new LinkedBlockingQueue<PresenceEvent>();
				presenceUpdater = new PresenceUpdater(context);
			}
		}

		presenceEventQueue.offer(pe);

		synchronized (presenceUpdater) {
			if (!presenceUpdater.isScheduled()) {
				presenceUpdater.setScheduled(true);
				// if
				// (!scheduledExecutor.getQueue().contains(presenceUpdater))
				// {
				scheduledExecutor.schedule(presenceUpdater, 1, TimeUnit.SECONDS);
			}
		}
	}

	private static void updateContact(Context context, ContentResolver resolver, Account account, String jid, String fullName,
			byte[] avatar, String group, boolean inSync, long rawContactId, long userId, BatchOperation batchOperation) {

		boolean existingAvatar = false;
		boolean existingGroup = false;
		boolean existingProfile = false;

		final Cursor c = resolver.query(DataQuery.CONTENT_URI, DataQuery.PROJECTION, DataQuery.SELECTION,
				new String[] { String.valueOf(rawContactId) }, null);
		final ContactOperations contactOp = ContactOperations.updateExistingContact(context, rawContactId, inSync,
				batchOperation);

		try {
			// Iterate over the existing rows of data, and update
			// each one
			// with the information we received from the server.
			while (c.moveToNext()) {
				final long id = c.getLong(DataQuery.COLUMN_ID);
				final long serverId = c.getLong(DataQuery.COLUMN_SERVER_ID);
				final String mimeType = c.getString(DataQuery.COLUMN_MIMETYPE);
				final Uri uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);
				if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
					contactOp.updateName(uri, c.getString(DataQuery.COLUMN_GIVEN_NAME),
							c.getString(DataQuery.COLUMN_FAMILY_NAME), c.getString(DataQuery.COLUMN_FULL_NAME), null, null,
							fullName);
				} else if (mimeType.equals(Photo.CONTENT_ITEM_TYPE)) {
					existingAvatar = true;
					contactOp.updateAvatar(uri, avatar);
				} else if (mimeType.equals(GroupMembership.CONTENT_ITEM_TYPE) && group != null) {
					existingGroup = true;
					long groupId = ensureGroupExists(context, account.name, group);
					if (groupId != 0) {
						contactOp.updateGroupMembership(uri, groupId);
					}
				} else if (mimeType.equals(Constants.PROFILE_MIMETYPE)) {
					existingProfile = true;
					contactOp.updateProfile(uri, jid);
				}
			} // while
				// always true for now
			if (/* userId != serverId */true) {
				Uri rawUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
				contactOp.updateServerId(userId, rawUri);
			}
		} finally {
			c.close();
		}

		if (!existingAvatar) {
			contactOp.addAvatar(avatar);
		}
		if (!existingGroup && group != null) {
			long groupId = ensureGroupExists(context, account.name, group);
			if (groupId != 0) {
				contactOp.addGroupMembership(groupId);
			}
		}
		if (!existingProfile) {
			contactOp.addProfile(jid);
		}
	}

	private final AccountManager accountManager;

	private final Context context;

	private final MessengerDatabaseHelper dbHelper;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);

		this.context = context;
		this.dbHelper = new MessengerDatabaseHelper(getContext());
		this.accountManager = AccountManager.get(context);
	}

	private void deleteContacts(ContentResolver resolver, Account account, BatchOperation batchOperation) {

		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		final Cursor c = db.rawQuery("SELECT roster." + RosterCacheTableMetaData.FIELD_ID + " FROM "
				+ RosterCacheTableMetaData.TABLE_NAME + " roster" + " WHERE roster." + RosterCacheTableMetaData.FIELD_ACCOUNT
				+ "=?", new String[] { account.name });

		StringBuilder builder = new StringBuilder(1024);
		builder.append("(0");
		try {
			while (c.moveToNext()) {
				builder.append(",");
				builder.append(c.getLong(0));
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		builder.append(")");

		final Cursor c1 = resolver.query(RawContacts.CONTENT_URI, new String[] { BaseColumns._ID }, RawContacts.ACCOUNT_TYPE
				+ "='" + Constants.ACCOUNT_TYPE + "' AND " + RawContacts.ACCOUNT_NAME + " = '" + account.name + "' AND " + RawContacts.SOURCE_ID + " NOT IN " + builder.toString(), null,
				null);

		try {
			while (c1.moveToNext()) {
				batchOperation.add(ContactOperations.newDeleteCpo(
						ContentUris.withAppendedId(RawContacts.CONTENT_URI, c1.getLong(0)), true, true).build());
			}
		} finally {
			if (c1 != null) {
				c1.close();
			}
		}

	}

	private long getSyncMarker(Account account) {
		String markerString = accountManager.getUserData(account, SYNC_MARKER_KEY);
		if (!TextUtils.isEmpty(markerString)) {
			return Long.parseLong(markerString);
		}
		return 0;
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider,
			SyncResult syncResult) {
		Log.i(TAG, "starting contacts synchronization for account = " + account.name);

		long newMarker = new Date().getTime();
		long oldMarker = getSyncMarker(account);
		BareJID accountJid = BareJID.bareJIDInstance(account.name);
		if (forceContactsSync.remove(accountJid)) {
 			oldMarker = 0;
		}
		// final MultiJaxmpp multiJaxmpp = ((MessengerApplication)

		Log.d(TAG, "getting items in roster of account = " + account.name + " for old marker = " + oldMarker + " and new marker = " + newMarker);
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		final Cursor c = db.rawQuery("SELECT roster." + RosterCacheTableMetaData.FIELD_ID + ", roster."
				+ RosterCacheTableMetaData.FIELD_JID + "," + " roster." + RosterCacheTableMetaData.FIELD_NAME + ", roster."
				+ RosterCacheTableMetaData.FIELD_GROUP_NAME + "," + " vcard." + VCardsCacheTableMetaData.FIELD_DATA + " FROM "
				+ RosterCacheTableMetaData.TABLE_NAME + " roster" + " LEFT JOIN " + VCardsCacheTableMetaData.TABLE_NAME
				+ " vcard ON roster." + RosterCacheTableMetaData.FIELD_JID + " = vcard." + VCardsCacheTableMetaData.FIELD_JID
				+ " WHERE " + RosterCacheTableMetaData.FIELD_ACCOUNT + "=? AND ( roster."
				+ RosterCacheTableMetaData.FIELD_TIMESTAMP + ">?" + " OR vcard." + VCardsCacheTableMetaData.FIELD_TIMESTAMP
				+ ">?)", new String[] { account.name, String.valueOf(oldMarker), String.valueOf(oldMarker) });

		Log.d(TAG, "adding or updating in contacts based on roster of account = " + account.name);
		
		try {
			BatchOperation batchOperation = new BatchOperation(context, context.getContentResolver());
			List<BareJID> added = new ArrayList<BareJID>();
			int updated = 0;
			int total = 0;
			while (c.moveToNext()) {
				total++;
				long userId = c.getInt(c.getColumnIndex(RosterCacheTableMetaData.FIELD_ID));
				long id = lookupRawContact(context.getContentResolver(), userId);
				String group = null;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
					String groupsStr = c.getString(c.getColumnIndex(RosterCacheTableMetaData.FIELD_GROUP_NAME));
					if (groupsStr != null && !TextUtils.isEmpty(groupsStr)) {
						groupsStr = EscapeUtils.unescape(groupsStr);
						String[] groups = groupsStr.split(";");
						if (groups != null) {
							group = groups[0];
						}
					}
				}
				BareJID jid = BareJID.bareJIDInstance(c.getString(c.getColumnIndex(RosterCacheTableMetaData.FIELD_JID)));
				if (id == 0) {
					added.add(jid);
					final ContactOperations contactOps = ContactOperations.createNewContact(context, userId, account.name,
							true, batchOperation);
					contactOps.addName(
							EscapeUtils.unescape(c.getString(c.getColumnIndex(RosterCacheTableMetaData.FIELD_NAME))), null,
							null).addJID(jid).addAvatar(c.getBlob(c.getColumnIndex(VCardsCacheTableMetaData.FIELD_DATA)));
					contactOps.addProfile(jid.toString());
					Log.d(TAG, "adding contact for jid = " + jid.toString());
					if (group != null) {
						long groupId = ensureGroupExists(context, account.name, group);
						contactOps.addGroupMembership(groupId);
					}
				} else {
					updated++;
					Log.d(TAG, "updating contact for jid = " + jid.toString());
					updateContact(context, context.getContentResolver(), account, jid.toString(),
							EscapeUtils.unescape(c.getString(c.getColumnIndex(RosterCacheTableMetaData.FIELD_NAME))),
							c.getBlob(c.getColumnIndex(VCardsCacheTableMetaData.FIELD_DATA)), group, true, id, userId,
							batchOperation);
				}

				if (batchOperation.size() >= 50) {
					// added to split sync
					batchOperation.execute();
				}
			}
			batchOperation.execute();
			if (DEBUG) {
				Log.v(TAG, "total " + total + " contacts");
				Log.v(TAG, "added " + added.size() + " contacts");
				Log.v(TAG, "updated " + updated + " contacts");
			}
			if (!added.isEmpty()) {
				final MultiJaxmpp multiJaxmpp = ((MessengerApplication) context.getApplicationContext()).getMultiJaxmpp();
				final JaxmppCore jaxmpp = multiJaxmpp.get(BareJID.bareJIDInstance(account.name));
				if (jaxmpp != null) {
					synchronized(SyncAdapter.class) {
						for (BareJID jid : added) {
							RosterItem ri = jaxmpp.getRoster().get(jid);
							if (ri == null)
								continue;
							long id = lookupRawContact(context.getContentResolver(), ri.getId());							
							try {
								Presence p = jaxmpp.getPresence().getBestPresence(jid);
								ContactOperations.syncStatus(context, account.name, id, jid, p, batchOperation);
							} catch (XMLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}							
							if (batchOperation.size() > 50)
								batchOperation.execute();
						}
						batchOperation.execute();
					}
				}
			}
		} finally {
			c.close();
		}

		Log.d(TAG, "deleting in contacts removed from roster of account = " + account.name);
		BatchOperation batchOperation = new BatchOperation(context, context.getContentResolver());
		deleteContacts(context.getContentResolver(), account, batchOperation);
		if (DEBUG) {
			Log.v(TAG, "deleting " + batchOperation.size() + " contacts");
		}
		batchOperation.execute();
		setSyncMarker(account, newMarker);
		Log.i(TAG, "finished contacts synchronization for account = " + account.name);
	}

	private void setSyncMarker(Account account, long marker) {
		accountManager.setUserData(account, SYNC_MARKER_KEY, Long.toString(marker));
	}

}
