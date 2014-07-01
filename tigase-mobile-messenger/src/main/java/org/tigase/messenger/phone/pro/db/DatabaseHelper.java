package org.tigase.messenger.phone.pro.db;

import tigase.jaxmpp.android.caps.CapsDbHelper;
import tigase.jaxmpp.android.chat.OpenChatDbHelper;
import tigase.jaxmpp.android.roster.RosterDbHelper;
import tigase.jaxmpp.android.roster.RosterItemsCacheTableMetaData;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "mobile_messenger1.db";

	public static final Integer DATABASE_VERSION = 4;

	private static final String TAG = "tigase";
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}	
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		RosterDbHelper.onCreate(db);
		OpenChatDbHelper.onCreate(db);
		
        String sql;
        sql = "ALTER TABLE " + RosterItemsCacheTableMetaData.TABLE_NAME + " ADD COLUMN " + RosterItemsCacheTableExtMetaData.FIELD_STATUS + " INTEGER DEFAULT 0;";
        db.execSQL(sql);
        
        sql = "CREATE TABLE " + ChatTableMetaData.TABLE_NAME + " (";
        sql += ChatTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
        sql += ChatTableMetaData.FIELD_ACCOUNT + " TEXT, ";
        sql += ChatTableMetaData.FIELD_THREAD_ID + " TEXT, ";
        sql += ChatTableMetaData.FIELD_JID + " TEXT, ";
        sql += ChatTableMetaData.FIELD_AUTHOR_JID + " TEXT, ";
        sql += ChatTableMetaData.FIELD_AUTHOR_NICKNAME + " TEXT, ";
        sql += ChatTableMetaData.FIELD_TIMESTAMP + " DATETIME, ";
        sql += ChatTableMetaData.FIELD_BODY + " TEXT, ";
        sql += ChatTableMetaData.FIELD_ITEM_TYPE + " INTEGER, ";
        sql += ChatTableMetaData.FIELD_DATA + " TEXT, ";
        sql += ChatTableMetaData.FIELD_STATE + " INTEGER";
        sql += ");";
        db.execSQL(sql);

        sql = "CREATE INDEX IF NOT EXISTS ";
        sql += ChatTableMetaData.INDEX_JID;
        sql += " ON " + ChatTableMetaData.TABLE_NAME + " (";
        sql += ChatTableMetaData.FIELD_JID;
        sql += ")";
        db.execSQL(sql);		
        
		sql = "CREATE TABLE " + VCardsCacheTableMetaData.TABLE_NAME + " (";
		sql += VCardsCacheTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += VCardsCacheTableMetaData.FIELD_JID + " TEXT, ";
		sql += VCardsCacheTableMetaData.FIELD_HASH + " TEXT, ";
		sql += VCardsCacheTableMetaData.FIELD_DATA + " BLOB, ";
		sql += VCardsCacheTableMetaData.FIELD_TIMESTAMP + " DATETIME";
		sql += ");";
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS ";
		sql += VCardsCacheTableMetaData.INDEX_JID;
		sql += " ON " + VCardsCacheTableMetaData.TABLE_NAME + " (";
		sql += VCardsCacheTableMetaData.FIELD_JID;
		sql += ")";
		db.execSQL(sql);   
		
		CapsDbHelper.onCreate(db);
		
		sql = "CREATE TABLE " + GeolocationTableMetaData.TABLE_NAME + " (";
		sql += GeolocationTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += GeolocationTableMetaData.FIELD_JID + " TEXT, ";
		sql += GeolocationTableMetaData.FIELD_LON + " REAL, ";
		sql += GeolocationTableMetaData.FIELD_LAT + " REAL, ";
		sql += GeolocationTableMetaData.FIELD_ALT + " REAL, ";
		sql += GeolocationTableMetaData.FIELD_COUNTRY + " TEXT, ";
		sql += GeolocationTableMetaData.FIELD_LOCALITY + " TEXT, ";
		sql += GeolocationTableMetaData.FIELD_STREET + " TEXT ";
		sql += ");";
		db.execSQL(sql);
		
		sql = "CREATE INDEX IF NOT EXISTS ";
		sql += GeolocationTableMetaData.INDEX_JID;
		sql += " ON " + GeolocationTableMetaData.TABLE_NAME + " (";
		sql += GeolocationTableMetaData.FIELD_JID;
		sql += ")";
		db.execSQL(sql);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		RosterDbHelper.onUpgrade(db, oldVersion, newVersion);
		OpenChatDbHelper.onUpgrade(db, oldVersion, newVersion);
		
		if (oldVersion < 2) {
			String sql;
			sql = "CREATE TABLE " + VCardsCacheTableMetaData.TABLE_NAME + " (";
			sql += VCardsCacheTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
			sql += VCardsCacheTableMetaData.FIELD_JID + " TEXT, ";
			sql += VCardsCacheTableMetaData.FIELD_HASH + " TEXT, ";
			sql += VCardsCacheTableMetaData.FIELD_DATA + " BLOB, ";
			sql += VCardsCacheTableMetaData.FIELD_TIMESTAMP + " DATETIME";
			sql += ");";
			db.execSQL(sql);

			sql = "CREATE INDEX IF NOT EXISTS ";
			sql += VCardsCacheTableMetaData.INDEX_JID;
			sql += " ON " + VCardsCacheTableMetaData.TABLE_NAME + " (";
			sql += VCardsCacheTableMetaData.FIELD_JID;
			sql += ")";
			db.execSQL(sql);			
		}
		if (oldVersion < 3) {
			CapsDbHelper.onCreate(db);
			
			String sql;
			sql = "CREATE TABLE " + GeolocationTableMetaData.TABLE_NAME + " (";
			sql += GeolocationTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
			sql += GeolocationTableMetaData.FIELD_JID + " TEXT, ";
			sql += GeolocationTableMetaData.FIELD_LON + " REAL, ";
			sql += GeolocationTableMetaData.FIELD_LAT + " REAL, ";
			sql += GeolocationTableMetaData.FIELD_ALT + " REAL, ";
			sql += GeolocationTableMetaData.FIELD_COUNTRY + " TEXT, ";
			sql += GeolocationTableMetaData.FIELD_LOCALITY + " TEXT, ";
			sql += GeolocationTableMetaData.FIELD_STREET + " TEXT ";
			sql += ");";
			db.execSQL(sql);
			
			sql = "CREATE INDEX IF NOT EXISTS ";
			sql += GeolocationTableMetaData.INDEX_JID;
			sql += " ON " + GeolocationTableMetaData.TABLE_NAME + " (";
			sql += GeolocationTableMetaData.FIELD_JID;
			sql += ")";
			db.execSQL(sql);
		}
		if (oldVersion < 4) {
			String sql;
			sql = "ALTER TABLE " + ChatTableMetaData.TABLE_NAME + " ADD COLUMN " + ChatTableMetaData.FIELD_ITEM_TYPE + " INTEGER DEFAULT 0;";
			db.execSQL(sql);
			sql = "UPDATE " + ChatTableMetaData.TABLE_NAME + " SET " + ChatTableMetaData.FIELD_ITEM_TYPE + " = 0";
			db.execSQL(sql);
			sql = "ALTER TABLE " + ChatTableMetaData.TABLE_NAME + " ADD COLUMN " + ChatTableMetaData.FIELD_DATA + " TEXT;";
			db.execSQL(sql);
		}
	}

}
