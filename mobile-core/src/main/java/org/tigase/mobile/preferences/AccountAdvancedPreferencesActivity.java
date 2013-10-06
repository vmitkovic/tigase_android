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
package org.tigase.mobile.preferences;

import org.tigase.mobile.Constants;
import org.tigase.mobile.Features;
import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.R;
import org.tigase.mobile.service.GeolocationFeature;
import org.tigase.mobile.service.MobileModeFeature;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;

public class AccountAdvancedPreferencesActivity extends FragmentActivity {

	private static final int PICK_ACCOUNT = 1;

	private static final String TAG = "AccountAdvancedPreferencesActivity";

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_ACCOUNT) {
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

		final AccountManager accountManager = AccountManager.get(this.getApplicationContext());
		if (getIntent().getParcelableExtra("account") != null) {
			account = (Account) getIntent().getParcelableExtra("account");
		} else {
			jidStr = getIntent().getStringExtra("account_jid");
			for (Account acc : accountManager.getAccountsByType(Constants.ACCOUNT_TYPE)) {
				if (jidStr.equals(acc.name)) {
					account = acc;
					break;
				}
			}
		}

		if ((jidStr == null && Build.VERSION_CODES.JELLY_BEAN <= Build.VERSION.SDK_INT) || account == null) {
			startChooseAccountIceCream(account);
		} else {
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
