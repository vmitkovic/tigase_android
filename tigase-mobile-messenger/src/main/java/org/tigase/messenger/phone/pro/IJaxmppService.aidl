package org.tigase.messenger.phone.pro;

import org.tigase.messenger.phone.pro.roster.CPresence;
import org.tigase.messenger.phone.pro.roster.RosterUpdateCallback;

interface IJaxmppService {
	
	boolean connect(String accountJid);
	boolean disconnect(String accountJid);

	boolean isConnected(String accountJid);

	CPresence getBestPresence(String accountJid, String jid);

	List<CPresence> getPresences(String accountJid, String jid);
	
	boolean openChat(String accountJid, String jid);
	boolean sendMessage(String accountJid, String jid, String thread, String message);
	void closeChat(String accountJid, String jid, String thread);
	
	void updateRosterItem(String accountJid, String jid, String name, in List<String> groups, boolean requestAuth, RosterUpdateCallback callback);
}