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
package org.tigase.messenger.phone.pro.preferences;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.AccountAuthenticator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.SyncStateContract.Constants;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class AccountAdvancedPreferencesActivity extends FragmentActivity {

	private static final int PICK_ACCOUNT = 1;

	private static final String TAG = "AccountAdvancedPreferencesActivity";

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//---- something is wrong here! data=null? why?
		Log.v(TAG, "requestCode = " + requestCode + ", result = " + resultCode + ", data = " + data);
		if (requestCode == PICK_ACCOUNT && data != null) {
			String accName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			if (accName == null) {
				this.finish();
				return;
			}
			final AccountManager accountManager = AccountManager.get(this.getApplicationContext());
			Account account = null;
			for (Account acc : accountManager.getAccountsByType(Constants.ACCOUNT_TYPE)) {
				if (acc.name.equals(accName)) {
					account = acc;
					break;
				}
			}
			setAccount(account);
		}
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.account_advanced_preferences_activity);
		
		Account account = null;
		String jidStr = null;

		final AccountManager accountManager = AccountManager.get(this);
		if (getIntent().getParcelableExtra("account") != null) {
			Log.v(TAG, "setting account 1 = " + getIntent().getParcelableExtra("account"));
			account = (Account) getIntent().getParcelableExtra("account");
		} else {
			jidStr = getIntent().getStringExtra("account_jid");
			Log.v(TAG, "setting jidStr = " + jidStr);
			Account[] accounts = accountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
			Log.v(TAG, "found " + accounts.length + " for type");
			for (Account acc : accounts) {
				if (jidStr.equals(acc.name)) {
					Log.v(TAG, "setting account 1 = " + acc.name);
					account = acc;
					break;
				}
			}
		}

		if ((jidStr == null && Build.VERSION_CODES.JELLY_BEAN <= Build.VERSION.SDK_INT) || account == null) {
			Log.v(TAG, "choose account!");
			startChooseAccountIceCream(account);
		} else {
			Log.v(TAG, "account = " + account);
			setAccount(account);
		}

	}
	
	private void setAccount(Account account) {
		AccountAdvancedPreferencesFragment advFragment = 
				(AccountAdvancedPreferencesFragment) this.getSupportFragmentManager().findFragmentById(R.id.account_advanced_preferences_fragment);
		advFragment.setAccount(account);
	}

	@TargetApi(14)
	private void startChooseAccountIceCream(final Account account) {
		Intent intentChooser = AccountManager.newChooseAccountIntent(account, null, new String[] { Constants.ACCOUNT_TYPE },
				false, null, null, null, null);
		this.startActivityForResult(intentChooser, PICK_ACCOUNT);
	}

}
