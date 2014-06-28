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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.InputStream;

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
					ftManager.sendFile(peerJid, filename, size, is, null);
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
	}

	@Override
	public void onFileTransferSuccess(SessionObject sessionObject,
			FileTransfer fileTransfer) {
		jaxmppService.notificationHelper.notifyFileTransferProgress(fileTransfer, State.finished);
	}

	@Override
	public void onFileTransferRejected(SessionObject sessionObject,
			FileTransfer fileTransfer) {
		jaxmppService.notificationHelper.notifyFileTransferProgress(fileTransfer, State.error);
	}

	@Override
	public void onFileTransferFailure(SessionObject sessionObject,
			FileTransfer fileTransfer) {
		jaxmppService.notificationHelper.notifyFileTransferProgress(fileTransfer, State.error);
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
}
