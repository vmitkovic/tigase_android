package org.tigase.messenger.phone.pro.receivers;

import org.tigase.messenger.phone.pro.utils.AvatarHelper;

import tigase.jaxmpp.core.client.BareJID;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AvatarUpdatedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String jidStr = intent.getStringExtra("jid");
		BareJID jid = BareJID.bareJIDInstance(jidStr);
		AvatarHelper.clearAvatar(jid);
	}

}
