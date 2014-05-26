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
package org.tigase.messenger.phone.pro.muc;


import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.chat.ChatView;
import org.tigase.messenger.phone.pro.service.JaxmppService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;

public class OccupantsListActivity extends FragmentActivity {

	private IJaxmppService jaxmppService;
	
	private ServiceConnection jaxmppServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			jaxmppService = IJaxmppService.Stub.asInterface(service);
			fragment.setJaxmppService(jaxmppService);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			jaxmppService = null;
		}
		
	};	
	
	OccupantsListFragment fragment = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.muc_occupants_list_activity);
		
		if (savedInstanceState == null) {
//			Bundle arguments = new Bundle();
//			arguments.putLong("roomId", getIntent().getLongExtra("roomId",-1));
			Bundle arguments = getIntent().getExtras();
			fragment = new OccupantsListFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.muc_occupants_container, fragment).commit();
		}
		Intent intent = new Intent(this, JaxmppService.class);
		intent.putExtra("ID", "AIDL");
		bindService(intent, jaxmppServiceConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.unbindService(jaxmppServiceConnection);
	}

}
