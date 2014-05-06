package org.tigase.messenger.phone.pro.roster;

interface RosterUpdateCallback {

	void onSuccess(String msg);
	void onFailure(String msg);

}