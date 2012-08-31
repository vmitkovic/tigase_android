/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.sipdroid.media;

import java.io.IOException;
import java.net.SocketException;

import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;
import org.sipdroid.pjlib.Codec;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import com.beem.project.beem.jingle.JingleService;
import com.beem.project.beem.ui.Call;

/**
 * RtpStreamReceiver is a generic stream receiver. It receives packets from RTP
 * and writes them into an OutputStream.
 */
public class RtpStreamReceiver extends Thread {

	/** Whether working in debug mode. */
	public static boolean DEBUG = true;

	/** Payload type */
	int p_type;

	/** Size of the read buffer */
	public static final int BUFFER_SIZE = 1024;

	/**
	 * Maximum blocking time, spent waiting for reading new bytes [milliseconds]
	 */
	public static final int SO_TIMEOUT = 200;

	/** The RtpSocket */
	private RtpSocket rtp_socket = null;

	/** Whether it is running */
	private boolean running;
	private AudioManager am;
	public static int speakermode;
	private JingleService mJingle;

	private final StreamCallback callback;

	/**
	 * Constructs a RtpStreamReceiver.
	 * 
	 * @param output_stream
	 *            the stream sink
	 * @param socket
	 *            the local receiver SipdroidSocket
	 */
	public RtpStreamReceiver(StreamCallback callback, SipdroidSocket socket, int payload_type) {
		this.callback = callback;
		init(socket);
		p_type = payload_type;
	}

	/** Inits the RtpStreamReceiver */
	private void init(SipdroidSocket socket) {
		if (socket != null)
			rtp_socket = new RtpSocket(socket);
	}

	/** Whether is running */
	public boolean isRunning() {
		return running;
	}

	/** Stops running */
	public void halt() {
		running = false;
	}

	public int speaker(int mode) {
		int old = speakermode;

		if (Call.headset > 0 && mode == AudioManager.MODE_NORMAL)
			return old;
		saveVolume();
		setMode(speakermode = mode);
		restoreVolume();
		return old;
	}

	double smin = 200, s;
	public static int nearend;

	void calc(short[] lin, int off, int len) {
		int i, j;
		double sm = 30000, r;

		for (i = 0; i < len; i += 5) {
			j = lin[i + off];
			s = 0.03 * Math.abs(j) + 0.97 * s;
			if (s < sm)
				sm = s;
			if (s > smin)
				nearend = 3000 / 5;
			else if (nearend > 0)
				nearend--;
		}
		for (i = 0; i < len; i++) {
			j = lin[i + off];
			if (j > 6550)
				lin[i + off] = 6550 * 5;
			else if (j < -6550)
				lin[i + off] = -6550 * 5;
			else
				lin[i + off] = (short) (j * 5);
		}
		r = (double) len / 100000;
		smin = sm * r + smin * (1 - r);
	}

	static void setStreamVolume(final int stream, final int vol, final int flags) {
		(new Thread() {
			public void run() {
				AudioManager am = (AudioManager) Call.mContext.getSystemService(Context.AUDIO_SERVICE);
				am.setStreamVolume(stream, vol, flags);
				if (stream == AudioManager.STREAM_MUSIC)
					restored = true;
			}
		}).start();
	}

	static boolean restored;

	public static float getEarGain() {
		return 0.25f;
		// XXX try {
		// return
		// Float.valueOf(PreferenceManager.getDefaultSharedPreferences(Call.mContext).getString(
		// Call.headset > 0 ? "heargain" : "eargain", "0.25"));
		// } catch (NumberFormatException i) {
		// return (float) 0.25;
		// }
	}

	void restoreVolume() {
		switch (am.getMode()) {
		case AudioManager.MODE_IN_CALL:
			setStreamVolume(AudioManager.STREAM_RING, (int) (am.getStreamMaxVolume(AudioManager.STREAM_RING) * getEarGain()), 0);
			track.setStereoVolume(AudioTrack.getMaxVolume() * getEarGain(), AudioTrack.getMaxVolume() * getEarGain());
			break;
		case AudioManager.MODE_NORMAL:
			track.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
			break;
		}
		setStreamVolume(
				AudioManager.STREAM_MUSIC,
				PreferenceManager.getDefaultSharedPreferences(Call.mContext).getInt(
						"volume" + speakermode,
						am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * (speakermode == AudioManager.MODE_NORMAL ? 4 : 3)
								/ 4), 0);
	}

	void saveVolume() {
		if (restored) {
			Editor edit = PreferenceManager.getDefaultSharedPreferences(Call.mContext).edit();
			edit.putInt("volume" + speakermode, am.getStreamVolume(AudioManager.STREAM_MUSIC));
			edit.commit();
		}
	}

	void saveSettings() {
		if (!PreferenceManager.getDefaultSharedPreferences(Call.mContext).getBoolean("oldvalid", false)) {
			int oldvibrate = am.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
			int oldvibrate2 = am.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION);
			if (!PreferenceManager.getDefaultSharedPreferences(Call.mContext).contains("oldvibrate2"))
				oldvibrate2 = AudioManager.VIBRATE_SETTING_ON;
			Editor edit = PreferenceManager.getDefaultSharedPreferences(Call.mContext).edit();
			edit.putInt("oldvibrate", oldvibrate);
			edit.putInt("oldvibrate2", oldvibrate2);
			edit.putInt("oldring", am.getStreamVolume(AudioManager.STREAM_RING));
			edit.putBoolean("oldvalid", true);
			edit.commit();
		}
	}

	public static void setMode(int mode) {
		Editor edit = PreferenceManager.getDefaultSharedPreferences(Call.mContext).edit();
		edit.putBoolean("setmode", mode != AudioManager.MODE_NORMAL);
		edit.commit();
		AudioManager am = (AudioManager) Call.mContext.getSystemService(Context.AUDIO_SERVICE);
		am.setMode(mode);
	}

	public static void restoreMode() {
		if (PreferenceManager.getDefaultSharedPreferences(Call.mContext).getBoolean("setmode", true)) {
			setMode(AudioManager.MODE_NORMAL);
		}
	}

	public static void restoreSettings() {
		if (PreferenceManager.getDefaultSharedPreferences(Call.mContext).getBoolean("oldvalid", true)) {
			AudioManager am = (AudioManager) Call.mContext.getSystemService(Context.AUDIO_SERVICE);
			int oldvibrate = PreferenceManager.getDefaultSharedPreferences(Call.mContext).getInt("oldvibrate", 0);
			int oldvibrate2 = PreferenceManager.getDefaultSharedPreferences(Call.mContext).getInt("oldvibrate2", 0);
			am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, oldvibrate);
			am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, oldvibrate2);
			setStreamVolume(AudioManager.STREAM_RING,
					PreferenceManager.getDefaultSharedPreferences(Call.mContext).getInt("oldring", 0), 0);
			Editor edit = PreferenceManager.getDefaultSharedPreferences(Call.mContext).edit();
			edit.putBoolean("oldvalid", false);
			edit.commit();
		}
		restoreMode();
	}

	public static float good, late, lost, loss;
	public static int timeout;

	void empty() {
		try {
			rtp_socket.getDatagramSocket().setSoTimeout(1);
			for (;;)
				rtp_socket.receive(rtp_packet);
		} catch (SocketException e2) {
			e2.printStackTrace();
		} catch (IOException e) {
		}
		try {
			rtp_socket.getDatagramSocket().setSoTimeout(1000);
		} catch (SocketException e2) {
			e2.printStackTrace();
		}
	}

	RtpPacket rtp_packet;
	AudioTrack track;

	/** Runs it in a new Thread. */
	public void run() {
		boolean nodata = PreferenceManager.getDefaultSharedPreferences(Call.mContext).getBoolean("nodata", false);

		if (rtp_socket == null) {
			if (DEBUG)
				println("ERROR: RTP socket is null");
			return;
		}

		byte[] buffer = new byte[BUFFER_SIZE + 12];
		byte[] buffer_gsm = new byte[33 + 12];
		int i;
		rtp_packet = new RtpPacket(buffer, 0);

		if (DEBUG)
			println("Reading blocks of max " + buffer.length + " bytes");

		running = true;
		speakermode = Call.docked > 0 ? AudioManager.MODE_NORMAL : AudioManager.MODE_IN_CALL;
		restored = false;

		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
		am = (AudioManager) Call.mContext.getSystemService(Context.AUDIO_SERVICE);
		Call.mContext.getContentResolver();
		saveSettings();
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_OFF);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);
		int oldvol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		track = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE * 2 * 2, AudioTrack.MODE_STREAM);
		short lin[] = new short[BUFFER_SIZE];
		short lin2[] = new short[BUFFER_SIZE];
		int user, server, lserver, luser, cnt, todo, headroom, len = 0, seq = 0, cnt2 = 0, m = 1, expseq, getseq, vm = 1, gap, gseq;
		timeout = 1;
		boolean islate;
		user = 0;
		lserver = 0;
		luser = -8000;
		cnt = 0;
		switch (p_type) {
		case 3:
			Codec.init();
			break;
		case 0:
		case 8:
			G711.init();
			break;
		}
		ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, (int) (ToneGenerator.MAX_VOLUME * 2 * 0.95));
		track.play();
		if (Call.headset > 0 && Call.oRingtone != null) {
			ToneGenerator tg2 = new ToneGenerator(AudioManager.STREAM_RING, (int) (ToneGenerator.MAX_VOLUME * 2 * 0.95));
			tg2.startTone(ToneGenerator.TONE_SUP_RINGTONE);
			System.gc();
			tg2.stopTone();
		} else
			System.gc();
		while (running) {
			if (Call.call_state == Call.UA_STATE_HOLD) {
				tg.stopTone();
				track.pause();
				while (running && Call.call_state == Call.UA_STATE_HOLD) {
					try {
						sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				track.play();
				System.gc();
				timeout = 1;
				seq = 0;
			}
			try {
				rtp_socket.receive(rtp_packet);
				if (timeout != 0) {
					tg.stopTone();
					track.pause();
					user += track.write(lin2, 0, BUFFER_SIZE);
					user += track.write(lin2, 0, BUFFER_SIZE);
					track.play();
					cnt += 2 * BUFFER_SIZE;
					empty();
				}
				timeout = 0;
			} catch (IOException e) {
				if (timeout == 0 && nodata) {
					tg.startTone(ToneGenerator.TONE_SUP_RINGTONE);
				}
				rtp_socket.getDatagramSocket().disconnect();
				if (++timeout > 22) {
					try {
						mJingle.closeCall();
					} catch (RemoteException e1) {
						e1.printStackTrace();
					}
					break;
				}
			}
			if (running && timeout == 0) {
				gseq = rtp_packet.getSequenceNumber();
				if (seq == gseq) {
					m++;
					continue;
				}

				server = track.getPlaybackHeadPosition();
				headroom = user - server;

				if (headroom > 1500)
					cnt += len;
				else
					cnt = 0;

				if (lserver == server)
					cnt2++;
				else
					cnt2 = 0;

				if (cnt <= 500 || cnt2 >= 2 || headroom - 875 < len) {
					switch (rtp_packet.getPayloadType()) {
					case 0:
						len = rtp_packet.getPayloadLength();
						G711.ulaw2linear(buffer, lin, len);
						break;
					case 8:
						len = rtp_packet.getPayloadLength();
						G711.alaw2linear(buffer, lin, len);
						break;
					case 3:
						for (i = 12; i < 45; i++)
							buffer_gsm[i] = buffer[i];
						len = Codec.decode(buffer_gsm, lin, 0);
						break;
					}

					if (speakermode == AudioManager.MODE_NORMAL)
						calc(lin, 0, len);
				}

				if (headroom < 250) {
					todo = 875 - headroom;
					println("insert " + todo);
					islate = true;
					user += track.write(lin2, 0, todo);
				} else
					islate = false;

				if (cnt > 500 && cnt2 < 2) {
					todo = headroom - 875;
					println("cut " + todo);
					if (todo < len)
						user += track.write(lin, todo, len - todo);
				} else
					user += track.write(lin, 0, len);

				seq = gseq;

				if (user >= luser + 8000 && Call.call_state == Call.UA_STATE_INCALL) {
					if (luser == -8000 || am.getMode() != speakermode) {
						saveVolume();
						setMode(speakermode);
						restoreVolume();
					}
					luser = user;
				}
				lserver = server;
			}
		}
		track.stop();
		saveVolume();
		setStreamVolume(AudioManager.STREAM_MUSIC, oldvol, 0);
		restoreSettings();
		setStreamVolume(AudioManager.STREAM_MUSIC, oldvol, 0);
		tg.stopTone();
		tg = new ToneGenerator(AudioManager.STREAM_RING, ToneGenerator.MAX_VOLUME / 4 * 3);
		tg.startTone(ToneGenerator.TONE_PROP_PROMPT);
		try {
			sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		tg.stopTone();

		rtp_socket.close();
		rtp_socket = null;

		if (DEBUG)
			println("rtp receiver terminated");
	}

	/** Debug output */
	private static void println(String str) {
		System.out.println("RtpStreamReceiver: " + str);
	}

	public static int byte2int(byte b) { // return (b>=0)? b : -((b^0xFF)+1);
		// return (b>=0)? b : b+0x100;
		return (b + 0x100) % 0x100;
	}

	public static int byte2int(byte b1, byte b2) {
		return (((b1 + 0x100) % 0x100) << 8) + (b2 + 0x100) % 0x100;
	}
}
