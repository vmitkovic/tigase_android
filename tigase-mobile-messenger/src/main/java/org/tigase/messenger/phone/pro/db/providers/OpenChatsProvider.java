package org.tigase.messenger.phone.pro.db.providers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tigase.messenger.phone.pro.db.ChatTableMetaData;
import org.tigase.messenger.phone.pro.db.DatabaseHelper;
import org.tigase.messenger.phone.pro.db.RosterItemsCacheTableExtMetaData;

import tigase.jaxmpp.android.chat.OpenChatTableMetaData;
import tigase.jaxmpp.android.roster.RosterItemsCacheTableMetaData;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class OpenChatsProvider extends ContentProvider {

	public static final String AUTHORITY = "org.tigase.messenger.phone.pro.db.providers.OpenChatsProvider";

	public static final String OPEN_CHATS_URI = "content://" + AUTHORITY + "/open_chats";	
	
	public static final String FIELD_LAST_MESSAGE = "last_message";
	public static final String FIELD_NAME = "name";
	public static final String FIELD_STATE = "state";
	public static final String FIELD_UNREAD_COUNT = "unread";
	
	protected static final int OPEN_CHATS_URI_INDICATOR = 1;
	
	private final static Map<String, String> openChatsProjectionMap = new HashMap<String, String>() {

		private static final long serialVersionUID = 1L;
		{
			put(OpenChatTableMetaData.FIELD_ACCOUNT, "open_chats." + OpenChatTableMetaData.FIELD_ACCOUNT + " as " + OpenChatTableMetaData.FIELD_ACCOUNT);
			put(OpenChatTableMetaData.FIELD_ID, "open_chats." + OpenChatTableMetaData.FIELD_ID + " as " + OpenChatTableMetaData.FIELD_ID);
			put(OpenChatTableMetaData.FIELD_JID, "open_chats." + OpenChatTableMetaData.FIELD_JID + " as " + OpenChatTableMetaData.FIELD_JID);
			put(OpenChatsProvider.FIELD_NAME, "CASE WHEN recipient." + RosterItemsCacheTableMetaData.FIELD_NAME + " IS NULL THEN " 
					+ " open_chats." + OpenChatTableMetaData.FIELD_JID + " ELSE recipient." + RosterItemsCacheTableMetaData.FIELD_NAME + " END as " + OpenChatsProvider.FIELD_NAME);
			put(OpenChatsProvider.FIELD_UNREAD_COUNT, "(SELECT COUNT(" + ChatTableMetaData.TABLE_NAME + "." + ChatTableMetaData.FIELD_ID + ") from " + ChatTableMetaData.TABLE_NAME 
					+ " WHERE " + ChatTableMetaData.FIELD_ACCOUNT + " = open_chats." + OpenChatTableMetaData.FIELD_ACCOUNT 
					+ " AND " + ChatTableMetaData.FIELD_JID + " = open_chats." + OpenChatTableMetaData.FIELD_JID 
					+ " AND " + ChatTableMetaData.FIELD_STATE + " = " + ChatTableMetaData.STATE_INCOMING_UNREAD + ") as " + OpenChatsProvider.FIELD_UNREAD_COUNT);
			put(OpenChatTableMetaData.FIELD_TYPE, "open_chats." + OpenChatTableMetaData.FIELD_TYPE + " as " + OpenChatTableMetaData.FIELD_TYPE);
			put(OpenChatsProvider.FIELD_STATE, "CASE WHEN open_chats." + OpenChatTableMetaData.FIELD_TYPE + " = " + OpenChatTableMetaData.TYPE_MUC + " THEN " 
					+ " open_chats." + OpenChatTableMetaData.FIELD_ROOM_STATE + " ELSE recipient." + RosterItemsCacheTableExtMetaData.FIELD_STATUS + " END as " + OpenChatsProvider.FIELD_STATE);
			put(OpenChatsProvider.FIELD_LAST_MESSAGE, "(SELECT " + ChatTableMetaData.FIELD_BODY + " FROM " + ChatTableMetaData.TABLE_NAME
					+ " WHERE " + ChatTableMetaData.FIELD_ACCOUNT + " = open_chats." + OpenChatTableMetaData.FIELD_ACCOUNT 
					+ " AND " + ChatTableMetaData.FIELD_JID + " = open_chats." + OpenChatTableMetaData.FIELD_JID 
					+ " ORDER BY " + ChatTableMetaData.FIELD_TIMESTAMP + " DESC LIMIT 1) as " + OpenChatsProvider.FIELD_LAST_MESSAGE);
			put(OpenChatTableMetaData.FIELD_THREAD_ID, "open_chats." + OpenChatTableMetaData.FIELD_THREAD_ID + " as " + OpenChatTableMetaData.FIELD_THREAD_ID);
			put(OpenChatTableMetaData.FIELD_NICKNAME, "open_chats." + OpenChatTableMetaData.FIELD_NICKNAME + " as " + OpenChatTableMetaData.FIELD_NICKNAME);
		}
	};	
	private DatabaseHelper dbHelper;
	private static ContentObserver observer = null;
	
	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		if (observer == null) {
			final Context context = getContext();
			observer = new ContentObserver(null) {
				public void onChange(boolean selfChange) {
					Uri uri = Uri.parse(OPEN_CHATS_URI);
					context.getContentResolver().notifyChange(uri, null);
				}
			};
			context.getContentResolver().registerContentObserver(Uri.parse(ChatHistoryProvider.CHAT_URI), true, observer);
			context.getContentResolver().registerContentObserver(Uri.parse(RosterProvider.CONTENT_URI), true, observer);
		}
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		switch (match(uri)) {
			case OPEN_CHATS_URI_INDICATOR:
				qb.setProjectionMap(openChatsProjectionMap);
				qb.setTables(OpenChatTableMetaData.TABLE_NAME + " open_chats LEFT JOIN " + RosterItemsCacheTableMetaData.TABLE_NAME 
						+ " recipient ON recipient." + RosterItemsCacheTableMetaData.FIELD_ACCOUNT + " = open_chats." + OpenChatTableMetaData.FIELD_ACCOUNT 
						+ " AND recipient." + RosterItemsCacheTableMetaData.FIELD_JID + " = open_chats." + OpenChatTableMetaData.FIELD_JID);
				
				// may be removed later on production build - left to make tests easier
				qb.appendWhere("open_chats." + OpenChatTableMetaData.FIELD_TYPE + " IS NOT NULL");
				break;
			default:
				throw new IllegalArgumentException("Unknown URI '" + (uri != null ? uri.toString() : "null") + "'");		
		}
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch (match(uri)) {
		case OPEN_CHATS_URI_INDICATOR:
			return OPEN_CHATS_URI;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}	
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}

	private int match(Uri uri) {
		List<String> l = uri.getPathSegments();
		
		// unknown type - new types may be introduced in future
		//if (l != null && !l.isEmpty())
		//	return -1;
		
		return OPEN_CHATS_URI_INDICATOR;
	}
	
}
