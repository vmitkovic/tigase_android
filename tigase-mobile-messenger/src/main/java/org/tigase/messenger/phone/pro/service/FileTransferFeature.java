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
package org.tigase.messenger.phone.pro.service;

import java.io.File;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.j2se.filetransfer.FileTransfer;
import tigase.jaxmpp.j2se.filetransfer.FileTransferManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import java.io.InputStream;
import java.util.Date;

import org.tigase.messenger.phone.pro.db.ChatTableMetaData;
import org.tigase.messenger.phone.pro.db.providers.ChatHistoryProvider;
import org.tigase.messenger.phone.pro.share.FileTransferUtility;

public class FileTransferFeature implements FileTransferManager.FileTransferRequestHandler, 
		FileTransferManager.FileTransferProgressHandler, FileTransferManager.FileTransferFailureHandler,
		FileTransferManager.FileTransferRejectedHandler, FileTransferManager.FileTransferSuccessHandler {
	
	private static final String TAG = "FileTransferFeature";
	
	public static enum State {
		active,
		connecting,
		error,
		finished,
		negotiating
	}	
	
	private final JaxmppService jaxmppService;
	
	public FileTransferFeature(final JaxmppService jaxmppService) {
		this.jaxmppService = jaxmppService;
	}
	
	public void updateSettings(Jaxmpp jaxmpp, Context context) {

		FileTransferManager ftManager = jaxmpp.get(FileTransferManager.class);
		if (ftManager != null)
			return;

		FileTransferManager.initialize(jaxmpp, false);
		ftManager = jaxmpp.get(FileTransferManager.class);
		jaxmpp.getEventBus().addHandler(FileTransferManager.FileTransferProgressHandler.FileTransferProgressEvent.class, this);
		jaxmpp.getEventBus().addHandler(FileTransferManager.FileTransferRequestHandler.FileTransferRequestEvent.class, this);
		jaxmpp.getEventBus().addHandler(FileTransferManager.FileTransferSuccessHandler.FileTransferSuccessEvent.class, this);
		jaxmpp.getEventBus().addHandler(FileTransferManager.FileTransferFailureHandler.FileTransferFailureEvent.class, this);
		jaxmpp.getEventBus().addHandler(FileTransferManager.FileTransferRejectedHandler.FileTransferRejectedEvent.class, this);
	}
	
	public void startFileTransfer(final Context context, final JaxmppCore jaxmpp, final JID peerJid, final Uri uri,
			final String mimetype) {
		new Thread() {
			@Override
			public void run() {
				try {
					Log.v(TAG, "starting file transfer..");
					final ContentResolver cr = context.getContentResolver();
					final InputStream is = cr.openInputStream(uri);
					final long size = is.available();

					String filename = FileTransferUtility.resolveFilename(context, uri, mimetype);
					FileTransferManager ftManager = jaxmpp.get(FileTransferManager.class);
					FileTransfer ft = ftManager.sendFile(peerJid, filename, size, is, null);
					ft.setData("file-uri", uri);
					
					updateChat(ft, FileTransferFeature.State.connecting);
				} catch (Exception ex) {
					Log.e(TAG, "problem with starting filetransfer", ex);
				}
			}
		}.start();
	}

	@Override
	public void onFileTransferRequest(SessionObject sessionObject,
			FileTransfer fileTransfer) {
		jaxmppService.notificationHelper.notifyFileTransferRequest(fileTransfer);
		updateChat(fileTransfer, FileTransferFeature.State.negotiating);
	}

	@Override
	public void onFileTransferSuccess(SessionObject sessionObject,
			FileTransfer fileTransfer) {
		jaxmppService.notificationHelper.notifyFileTransferProgress(fileTransfer, State.finished);
		updateChat(fileTransfer, FileTransferFeature.State.finished);
	}

	@Override
	public void onFileTransferRejected(SessionObject sessionObject,
			FileTransfer fileTransfer) {
		jaxmppService.notificationHelper.notifyFileTransferProgress(fileTransfer, State.error);
		updateChat(fileTransfer, FileTransferFeature.State.error);
	}

	@Override
	public void onFileTransferFailure(SessionObject sessionObject,
			FileTransfer fileTransfer) {
		jaxmppService.notificationHelper.notifyFileTransferProgress(fileTransfer, State.error);
		updateChat(fileTransfer, FileTransferFeature.State.error);
	}

	@Override
	public void onFileTransferProgress(SessionObject sessionObject,
			FileTransfer fileTransfer) {
		jaxmppService.notificationHelper.notifyFileTransferProgress(fileTransfer, State.active);
	}

	protected void processFileTransferAction(Intent intent) {
		final JID peer = JID.jidInstance(intent.getStringExtra("peer"));
		final String sid = intent.getStringExtra("sid");
		final String tag = intent.getStringExtra("tag");

		final FileTransfer ft = (FileTransfer) FileTransferUtility.unregisterFileTransfer(peer, sid);

		final Jaxmpp jaxmpp = jaxmppService.getMulti().get(
				ft.getSessionObject());
		if ("reject".equals(intent.getStringExtra("filetransferAction"))) {
			Log.v(TAG, "incoming file rejected");
			jaxmppService.notificationHelper.cancelFileTransferRequestNotification(tag);
			new Thread() {
				@Override
				public void run() {
					try {
						jaxmpp.get(FileTransferManager.class).rejectFile(ft);
					} catch (JaxmppException e) {
						Log.e(TAG, "Could not send stream initiation reject", e);
					}
				}
			}.start();
		} else if ("accept".equals(intent.getStringExtra("filetransferAction"))) {
			jaxmppService.notificationHelper.cancelFileTransferRequestNotification(tag);
			String mimetype = ft.getFileMimeType();
			String filename = ft.getFilename();
			if (mimetype == null) {
				mimetype = FileTransferUtility.guessMimeType(filename);
				ft.setFileMimeType(mimetype);
			}

			String store = intent.getStringExtra("store");
			final File destination = FileTransferUtility.getPathToSave(filename, mimetype, store);
			ft.setFile(destination);
			ft.setData("file-uri", Uri.fromFile(destination));

			new Thread() {
				@Override
				public void run() {
					try {
						jaxmpp.get(FileTransferManager.class).acceptFile(ft);
					} catch (JaxmppException e) {
						Log.e(TAG, "Could not send stream initiation accept", e);
					}
				}
			}.start();

		}
	}
	
	protected void updateChat(FileTransfer ft, State state) {
		String jid = ft.getPeer().getBareJid().toString();
		Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/" + Uri.encode(jid));
		
		ContentValues values = new ContentValues();
		values.put(ChatTableMetaData.FIELD_ACCOUNT, ft.getSessionObject().getUserBareJid().toString());
		values.put(ChatTableMetaData.FIELD_AUTHOR_JID, ft.isIncoming() ? jid : ft.getSessionObject().getUserBareJid().toString());
		values.put(ChatTableMetaData.FIELD_JID, jid);
		values.put(ChatTableMetaData.FIELD_TIMESTAMP, new Date().getTime());
		if (ft.getData("file-uri") != null) {
			values.put(ChatTableMetaData.FIELD_DATA, ft.getData("file-uri").toString());
		}
		int stateInt = ft.isIncoming() 
				? ((state == State.finished || state == State.error) ? ChatTableMetaData.STATE_INCOMING : ChatTableMetaData.STATE_INCOMING_UNREAD)
				: ((state == State.finished || state == State.error) ? ChatTableMetaData.STATE_OUT_SENT : ChatTableMetaData.STATE_OUT_NOT_SENT);
		values.put(ChatTableMetaData.FIELD_STATE, ft.isIncoming() 
				? ChatTableMetaData.STATE_OUT_SENT : ChatTableMetaData.STATE_INCOMING_UNREAD);	

		if (ft.getData("db-id") == null) {
			values.put(ChatTableMetaData.FIELD_BODY, ft.getFilename());
			String mimeType = ft.getFileMimeType();
			if (mimeType == null) {
				mimeType = FileTransferUtility.guessMimeType(ft.getFilename());
			}
			int type = ChatTableMetaData.ITEM_TYPE_FILE;
			if (mimeType != null) {
				if (mimeType.startsWith("image/")) {
					type = ChatTableMetaData.ITEM_TYPE_IMAGE;
				}
				else if (mimeType.startsWith("video/")) {
					type = ChatTableMetaData.ITEM_TYPE_VIDEO;
				}
			}
			values.put(ChatTableMetaData.FIELD_ITEM_TYPE, type);
		}
		SQLiteDatabase db = jaxmppService.dbHelper.getWritableDatabase();
		if (ft.getData("db-id") == null) {
			long id = db.insert(ChatTableMetaData.TABLE_NAME, null, values);
			ft.setData("db-id", id);
			Log.v(TAG, "inserted message - id = " + id);
		} else {
			long id = (Long) ft.getData("db-id");
			db.update(ChatTableMetaData.TABLE_NAME, values, ChatTableMetaData.FIELD_ID + "=?", new String[] { String.valueOf(id) });
		}
		jaxmppService.getContentResolver().notifyChange(uri, null);
	}

}
