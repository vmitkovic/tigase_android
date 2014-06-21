/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2014 "Tigase, Inc." <office@tigase.com>
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

import org.tigase.messenger.phone.pro.db.DatabaseHelper;
import org.tigase.messenger.phone.pro.db.GeolocationTableMetaData;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class GeolocationProvider extends ContentProvider {

	public static final String AUTHORITY = "org.tigase.messenger.phone.pro.db.providers.GeolocationProvider";
	public static final String CONTENT_URI = "content://" + AUTHORITY + "/geoloc";
	private DatabaseHelper dbHelper;

	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		return dbHelper.getReadableDatabase().query(true, GeolocationTableMetaData.TABLE_NAME, projection, selection, selectionArgs, null, null, null, null);
	}

	@Override
	public String getType(Uri uri) {
		return GeolocationTableMetaData.CONTENT_TYPE;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new RuntimeException("There is nothing to insert! uri=" + uri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new RuntimeException("There is nothing to delete! uri=" + uri);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		throw new RuntimeException("There is nothing to update! uri=" + uri);
	}

}
