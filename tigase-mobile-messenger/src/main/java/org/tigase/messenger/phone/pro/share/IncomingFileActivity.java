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
package org.tigase.messenger.phone.pro.share;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.providers.RosterProvider;
import org.tigase.messenger.phone.pro.service.JaxmppService;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;

import tigase.jaxmpp.android.roster.RosterItemsCacheTableMetaData;
import tigase.jaxmpp.core.client.JID;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class IncomingFileActivity extends Activity {

	private static final String TAG = "IncomingFileActivity";

	private JID peerJid;
	private String store = "Download";
	private String tag;
	private String sid;
	private String mimetype;

	public void accept(View view) {
		Log.v(TAG, "incoming file accepted");

//		-- FileTransferUtility must not be used here!!
//		FileTransferUtility.registerFileTransfer(ft);

		Intent intent = new Intent(getApplicationContext(), JaxmppService.class);
		intent.putExtra("tag", tag);
		intent.putExtra("peer", peerJid.toString());
		intent.putExtra("sid", sid);
		intent.setAction(JaxmppService.ACTION_FILETRANSFER);
		intent.putExtra("filetransferAction", "accept");
		intent.putExtra("store", store);

		startService(intent);

		finish();
	}

	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// switch (item.getItemId()) {
	// case R.id.incoming_file_accept: {
	// accept(null);
	// break;
	// }
	// case R.id.incoming_file_reject: {
	// reject(null);
	// break;
	// }
	// default:
	// break;
	// }
	//
	// return true;
	// }

	// @Override
	// public boolean onPrepareOptionsMenu(Menu menu) {
	// MenuInflater inflater = getMenuInflater();
	// menu.clear();
	// final Jaxmpp jaxmpp = ((MessengerApplication)
	// getApplicationContext()).getMultiJaxmpp().get(account.getBareJid());
	// if (jaxmpp.isConnected()) {
	// inflater.inflate(R.menu.incoming_file_menu, menu);
	// }
	// return true;
	// }

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		setContentView(R.layout.incoming_file);

		Intent intent = getIntent();
		tag = intent.getStringExtra("tag");

		peerJid = JID.jidInstance(intent.getStringExtra("peer"));
		sid = intent.getStringExtra("sid");
//		-- FileTransferUtility must not be used here!!
//		ft = FileTransferUtility.unregisterFileTransfer(peerJid, sid);
//		FileTransferUtility.registerFileTransfer(ft);

		Long filesize = intent.getLongExtra("size", 0);//ft.getFileSize();
		String filesizeStr = null;
		if (filesize == 0) {
			filesize = null;
		} else {
			if (filesize > 1024 * 1024) {
				filesizeStr = String.valueOf(filesize / (1024 * 1024)) + "MB";
			} else if (filesize > 1024) {
				filesizeStr = String.valueOf(filesize / 1024) + "KB";
			} else {
				filesizeStr = String.valueOf(filesize) + "B";
			}
		}

		mimetype = intent.getStringExtra("mimetype");
		String filename = intent.getStringExtra("filename");
		if (mimetype == null) {
			mimetype = FileTransferUtility.guessMimeType(filename);
			//ft.setFileMimeType(mimetype);
		}
		Log.v(TAG, "got mimetype = " + mimetype);

		Uri uri = Uri.parse(RosterProvider.CONTENT_URI + "/" + Uri.encode(peerJid.getBareJid().toString()));
		Cursor c = getContentResolver().query(uri, null, null, null, null);
		String senderName = peerJid.toString();
		if (c.moveToNext()) {
			int nameIdx = c.getColumnIndex(RosterItemsCacheTableMetaData.FIELD_NAME);
			if (!c.isNull(nameIdx)) {
				senderName = c.getString(nameIdx);
			}
		}
		
		((TextView) findViewById(R.id.incoming_file_from_name)).setText(senderName);
		((TextView) findViewById(R.id.incoming_file_from_jid)).setText(senderName.equals(peerJid.getBareJid().toString()) ? ""
				: peerJid.getBareJid().toString());
		((TextView) findViewById(R.id.incoming_file_filename)).setText(filename);
		((TextView) findViewById(R.id.incoming_file_filesize)).setText(filesize == null ? "unknown" : filesizeStr);

		ImageView itemAvatar = ((ImageView) findViewById(R.id.incoming_file_from_avatar));
		AvatarHelper.setAvatarToImageView(peerJid.getBareJid(), itemAvatar, 200);

		ArrayAdapter storeAdapter = ArrayAdapter.createFromResource(this, R.array.incoming_file_stores,
				android.R.layout.simple_spinner_item);
		storeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner storeSpinner = (Spinner) findViewById(R.id.incoming_file_store);
		storeSpinner.setAdapter(storeAdapter);
		storeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				store = (String) parent.getItemAtPosition(pos);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				parent.setSelection(0);
			}
		});

		int pos = 0;
		if (mimetype != null) {
			if (mimetype.startsWith("video/")) {
				pos = storeAdapter.getPosition("Movies");
			} else if (mimetype.startsWith("audio/")) {
				pos = storeAdapter.getPosition("Music");
			} else if (mimetype.startsWith("image/")) {
				pos = storeAdapter.getPosition("Pictures");
			}
		}
		storeSpinner.setSelection(pos);
	}

	public void reject(View view) {
//		-- FileTransferUtility must not be used here!!		
//		FileTransferUtility.registerFileTransfer(ft);

		Intent intent = new Intent(getApplicationContext(), JaxmppService.class);
		intent.putExtra("tag", tag);
		intent.putExtra("peer", peerJid.toString());
		intent.putExtra("sid", sid);
		intent.setAction(JaxmppService.ACTION_FILETRANSFER);
		intent.putExtra("filetransferAction", "reject");

		startService(intent);

		finish();
	}
}
