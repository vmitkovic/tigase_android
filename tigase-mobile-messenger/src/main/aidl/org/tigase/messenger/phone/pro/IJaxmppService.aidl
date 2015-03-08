package org.tigase.messenger.phone.pro;

import org.tigase.messenger.phone.pro.muc.Occupant;
import org.tigase.messenger.phone.pro.roster.CPresence;
import org.tigase.messenger.phone.pro.roster.RosterUpdateCallback;
import tigase.jaxmpp.android.xml.ParcelableElement;
import org.tigase.messenger.phone.pro.service.XmppCallback;

interface IJaxmppService {
	
	void updateConfiguration();
	void preferenceChanged(String key);
	List<String> getAccounts(boolean connectedOnly);
	
	boolean connect(String accountJid);
	boolean disconnect(String accountJid);

	boolean isStarted();
	boolean isConnected(String accountJid);
	boolean hasStreamFeature(String accountJid, String elemName, String streamFeatureXmlns);

	CPresence getBestPresence(String accountJid, String jid);

	List<CPresence> getPresences(String accountJid, String jid);
	
	// Chats
	boolean openChat(String accountJid, String jid);
	boolean sendMessage(String accountJid, String jid, String thread, String message);
	boolean sendMessageExt(String accountJid, String jid, String thread, String message, in List<ParcelableElement> elems);
	void closeChat(String accountJid, String jid, String thread);
	String getRecipientChatState(String accountJid, String jid, String thread);
	void setOwnChatState(String accountJid, String jid, String thread, String chatState);
	
	// MUC
	boolean joinRoom(String accountJid, String roomJid, String nickname, String password, String action);
	boolean leaveRoom(String accountJid, String roomJid);
	boolean sendRoomMessage(String accountJid, String roomJid, String message);
	Occupant[] getRoomOccupants(String accountJid, String roomJid);
	
	void updateRosterItem(String accountJid, String jid, String name, in List<String> groups, boolean requestAuth, RosterUpdateCallback callback);
	void publishVCard(String accountJid, in ParcelableElement vcardEl, in XmppCallback callback);
	void retrieveVCard(String accountJid, String jid, in XmppCallback callback);
	
	// FileTransfer
	boolean sendFile(String account, String jid, String fileUri, String mimetype);
	
	// Bookmarks
	void retrieveBookmarks(String accountJid, in XmppCallback callback);
	void publishBookmarks(String accountJid, in List<ParcelableElement> items, in XmppCallback callback);
}