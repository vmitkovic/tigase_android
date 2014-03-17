package org.tigase.messenger.phone.pro.db;

import tigase.jaxmpp.android.chat.OpenChatDbHelper;
import tigase.jaxmpp.android.roster.RosterDbHelper;
import tigase.jaxmpp.android.roster.RosterItemsCacheTableMetaData;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "mobile_messenger1.db";

	public static final Integer DATABASE_VERSION = 1;

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
        sql += ChatTableMetaData.FIELD_STATE + " INTEGER";
        sql += ");";
        db.execSQL(sql);

        sql = "CREATE INDEX IF NOT EXISTS ";
        sql += ChatTableMetaData.INDEX_JID;
        sql += " ON " + ChatTableMetaData.TABLE_NAME + " (";
        sql += ChatTableMetaData.FIELD_JID;
        sql += ")";
        db.execSQL(sql);		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		RosterDbHelper.onUpgrade(db, oldVersion, newVersion);
		OpenChatDbHelper.onUpgrade(db, oldVersion, newVersion);
	}

}
