package org.tigase.messenger.phone.pro.db;

import tigase.jaxmpp.android.chat.OpenChatDbHelper;
import tigase.jaxmpp.android.roster.RosterDbHelper;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "mobile_messenger.db";

	public static final Integer DATABASE_VERSION = 1;

	private static final String TAG = "tigase";
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}	
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		RosterDbHelper.onCreate(db);
		OpenChatDbHelper.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		RosterDbHelper.onUpgrade(db, oldVersion, newVersion);
		OpenChatDbHelper.onUpgrade(db, oldVersion, newVersion);
	}

}
