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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;

public class AccountAdvancedPreferencesActivity extends Activity {

	private static final int PICK_ACCOUNT = 1;

	private static final String TAG = "AccountAdvancedPreferencesActivity";

	public static final boolean isMobileAvailable(JaxmppCore jaxmpp, String feature) {
		if (jaxmpp == null)
			return false;
		if (jaxmpp.getSessionObject() == null)
			return false;
		final Element sf = jaxmpp.getSessionObject().getStreamFeatures();
		if (sf == null) {
			Log.v(TAG, "no stream features available for = " + jaxmpp.getSessionObject().getUserBareJid().toString());
			return false;
		}

		try {
			Element m = sf.getChildrenNS("mobile", feature);
			if (m == null)
				return false;
		} catch (XMLException e) {
			return false;
		}

		return true;
	}

	private CompoundButton geolocationListen;
	private Spinner geolocationPrecision;
	private CompoundButton geolocationPublish;
	private CompoundButton mobileOptimizations;

	private Spinner presenceQueueTimeout;

	private final MultiJaxmpp getMulti() {
		return ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();
	}

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

		setContentView(R.layout.account_advanced_preferences);

		Account account = null;
		String jidStr = null;

		final AccountManager accountManager = AccountManager.get(this.getApplicationContext());
		if (getIntent().getParcelableExtra("account") != null) {
			account = (Account) getIntent().getParcelableExtra("account");
		} else {
			jidStr = getIntent().getStringExtra("account_jid");
			for (Account acc : accountManager.getAccountsByType(Constants.ACCOUNT_TYPE)) {
				if (jidStr.equals(jidStr)) {
					account = acc;
					break;
				}
			}
		}

		if (jidStr == null && Build.VERSION_CODES.JELLY_BEAN <= Build.VERSION.SDK_INT) {
			startChooseAccountIceCream(account);
		} else {
			setAccount(account);
		}

	}

	public void setAccount(final Account account) {
		final AccountManager accountManager = AccountManager.get(this.getApplicationContext());
		final BareJID accountJid = BareJID.bareJIDInstance(account.name);

		mobileOptimizations = (CompoundButton) findViewById(R.id.mobile_optimizations);
		presenceQueueTimeout = (Spinner) findViewById(R.id.presence_queue_timeout);

		boolean available_v1 = isMobileAvailable(getMulti().get(accountJid), Features.MOBILE_V1)
				|| Features.MOBILE_V1.equals(accountManager.getUserData(account, Constants.MOBILE_OPTIMIZATIONS_AVAILABLE_KEY));
		boolean available_v2 = isMobileAvailable(getMulti().get(accountJid), Features.MOBILE_V2)
				|| Features.MOBILE_V2.equals(accountManager.getUserData(account, Constants.MOBILE_OPTIMIZATIONS_AVAILABLE_KEY));
		mobileOptimizations.setEnabled(available_v1 || available_v2);
		presenceQueueTimeout.setEnabled(available_v1 && !available_v2);

		String valueStr = accountManager.getUserData(account, MobileModeFeature.MOBILE_OPTIMIZATIONS_ENABLED);
		boolean enabled = valueStr == null || Boolean.valueOf(valueStr);
		mobileOptimizations.setChecked(enabled);
		mobileOptimizations.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				accountManager.setUserData(account, MobileModeFeature.MOBILE_OPTIMIZATIONS_ENABLED, String.valueOf(isChecked));
				getMulti().get(accountJid).getSessionObject().setUserProperty(MobileModeFeature.MOBILE_OPTIMIZATIONS_ENABLED,
						isChecked);
			}

		});

		valueStr = accountManager.getUserData(account, MobileModeFeature.MOBILE_OPTIMIZATIONS_QUEUE_TIMEOUT);
		int position = (valueStr == null) ? 1 : ((Integer.parseInt(valueStr) / 3) - 1);
		presenceQueueTimeout.setSelection(position);
		presenceQueueTimeout.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
				int value = 3 * (position + 1);
				accountManager.setUserData(account, MobileModeFeature.MOBILE_OPTIMIZATIONS_QUEUE_TIMEOUT, String.valueOf(value));
				getMulti().get(accountJid).getSessionObject().setUserProperty(
						MobileModeFeature.MOBILE_OPTIMIZATIONS_QUEUE_TIMEOUT, value);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		geolocationListen = (CompoundButton) findViewById(R.id.geolocation_listen);
		geolocationPublish = (CompoundButton) findViewById(R.id.geolocation_publish);
		geolocationPrecision = (Spinner) findViewById(R.id.geolocation_percision);
		if (geolocationPublish != null) {
			valueStr = accountManager.getUserData(account, GeolocationFeature.GEOLOCATION_LISTEN_ENABLED);
			geolocationListen.setChecked(valueStr != null && Boolean.parseBoolean(valueStr));
			geolocationListen.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					accountManager.setUserData(account, GeolocationFeature.GEOLOCATION_LISTEN_ENABLED,
							String.valueOf(isChecked));
					JaxmppCore jaxmpp = getMulti().get(accountJid);
					GeolocationFeature.updateGeolocationSettings(account, jaxmpp, getApplicationContext());
				}
			});
			valueStr = accountManager.getUserData(account, GeolocationFeature.GEOLOCATION_PUBLISH_ENABLED);
			geolocationPublish.setChecked(valueStr != null && Boolean.parseBoolean(valueStr));
			geolocationPublish.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					accountManager.setUserData(account, GeolocationFeature.GEOLOCATION_PUBLISH_ENABLED,
							String.valueOf(isChecked));
					JaxmppCore jaxmpp = getMulti().get(accountJid);
					GeolocationFeature.updateGeolocationSettings(account, jaxmpp, getApplicationContext());
					geolocationPrecision.setEnabled(isChecked);
				}
			});
			valueStr = accountManager.getUserData(account, GeolocationFeature.GEOLOCATION_PUBLISH_PRECISION);
			int precision = (valueStr != null) ? Integer.parseInt(valueStr) : 0;
			geolocationPrecision.setSelection(precision);
			geolocationPrecision.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
					int precision = position;
					accountManager.setUserData(account, GeolocationFeature.GEOLOCATION_PUBLISH_PRECISION,
							String.valueOf(precision));
					JaxmppCore jaxmpp = getMulti().get(accountJid);
					GeolocationFeature.updateGeolocationSettings(account, jaxmpp, getApplicationContext());
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}
			});
			geolocationPrecision.setEnabled(geolocationPublish.isChecked());
		}

		if (!available_v1 && !available_v2 && getMulti().get(accountJid).isConnected()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.mobile_optimizations_not_supported).setIcon(R.drawable.icon);
			builder.setTitle(R.string.mobile_optimizations).setCancelable(true);
			AlertDialog dlg = builder.create();
			dlg.show();
		}
	}

	@TargetApi(14)
	private void startChooseAccountIceCream(final Account account) {
		Intent intentChooser = AccountManager.newChooseAccountIntent(account, null, new String[] { Constants.ACCOUNT_TYPE },
				false, null, null, null, null);
		this.startActivityForResult(intentChooser, PICK_ACCOUNT);
	}

}
