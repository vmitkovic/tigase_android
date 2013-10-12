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
package org.tigase.mobile.roster;

import org.tigase.mobile.R;
import org.tigase.mobile.TigaseMobileMessengerActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class ContactActivity extends FragmentActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact_activity);


		if (savedInstanceState == null) {
			Bundle arguments = new Bundle();
			arguments.putString("jid", getIntent().getStringExtra("jid"));
			arguments.putString("account", getIntent().getStringExtra("account"));
			ContactFragment contactFragment = new ContactFragment();
			contactFragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction().add(R.id.main_detail_container, contactFragment).commit();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpTo(this, new Intent(this,
					TigaseMobileMessengerActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}	
	
}
