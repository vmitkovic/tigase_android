package org.tigase.messenger.phone.pro;

import org.tigase.messenger.phone.pro.roster.CPresence;

interface IJaxmppService {

	boolean isConnected(String accountJid);

	CPresence getBestPresence(String accountJid, String jid);

	List<CPresence> getPresences(String accountJid, String jid);
	
	boolean openChat(String accountJid, String jid);
	
	boolean sendMessage(String accountJid, String jid, String thread, String message);
}