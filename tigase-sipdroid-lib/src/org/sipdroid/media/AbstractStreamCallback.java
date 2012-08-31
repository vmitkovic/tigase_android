package org.sipdroid.media;

import android.content.Context;
import android.media.AudioManager;
import android.telephony.TelephonyManager;

public abstract class AbstractStreamCallback implements StreamCallback {

	private final Context context;

	public AbstractStreamCallback(Context context) {
		this.context = context;
	}

	@Override
	public void setStreamVolume(int stream, int vol, int flags) {
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		am.setStreamVolume(stream, vol, flags);
	}

	@Override
	public Context getContext() {
		return context;
	}

	@Override
	public AudioManager getAudioManager() {
		return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	}
	
	@Override
	public TelephonyManager getTelephonyManager() {
		return (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	}

}
