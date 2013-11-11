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
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;

public class AccountAdvancedPreferencesFragment extends Fragment {

	private static final String TAG = "AccountAdvancedPreferencesFragment";

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

	private Account account;
	private BareJID accountJid;
	private CompoundButton geolocationListen;
	private Spinner geolocationPrecision;

	private CompoundButton geolocationPublish;

	private View layout;

	private CompoundButton mobileOptimizations;

	private Spinner presenceQueueTimeout;

	private Context getApplicationContext() {
		return getActivity().getApplicationContext();
	}

	private final MultiJaxmpp getMulti() {
		return ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		layout = inflater.inflate(R.layout.account_advanced_preferences, container, false);

		mobileOptimizations = (CompoundButton) layout.findViewById(R.id.mobile_optimizations);
		presenceQueueTimeout = (Spinner) layout.findViewById(R.id.presence_queue_timeout);

		final AccountManager accountManager = AccountManager.get(this.getApplicationContext());
		mobileOptimizations.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				accountManager.setUserData(account, MobileModeFeature.MOBILE_OPTIMIZATIONS_ENABLED, String.valueOf(isChecked));
				getMulti().get(accountJid).getSessionObject().setUserProperty(MobileModeFeature.MOBILE_OPTIMIZATIONS_ENABLED,
						isChecked);
			}

		});

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

		geolocationListen = (CompoundButton) layout.findViewById(R.id.geolocation_listen);
		geolocationPublish = (CompoundButton) layout.findViewById(R.id.geolocation_publish);
		geolocationPrecision = (Spinner) layout.findViewById(R.id.geolocation_percision);

		if (geolocationPublish != null) {
			geolocationListen.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					accountManager.setUserData(account, GeolocationFeature.GEOLOCATION_LISTEN_ENABLED,
							String.valueOf(isChecked));
					JaxmppCore jaxmpp = getMulti().get(accountJid);
					GeolocationFeature.updateGeolocationSettings(account, jaxmpp, getApplicationContext());
				}
			});
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
		}

		return layout;
	}

	public void setAccount(final Account account) {
		final AccountManager accountManager = AccountManager.get(this.getApplicationContext());
		accountJid = BareJID.bareJIDInstance(account.name);
		this.account = account;

		boolean available_v1 = isMobileAvailable(getMulti().get(accountJid), Features.MOBILE_V1)
				&& Features.MOBILE_V1.equals(accountManager.getUserData(account, Constants.MOBILE_OPTIMIZATIONS_AVAILABLE_KEY));
		boolean available_v2 = isMobileAvailable(getMulti().get(accountJid), Features.MOBILE_V2)
				&& Features.MOBILE_V2.equals(accountManager.getUserData(account, Constants.MOBILE_OPTIMIZATIONS_AVAILABLE_KEY));
		boolean available_v3 = isMobileAvailable(getMulti().get(accountJid), Features.MOBILE_V3)
				&& Features.MOBILE_V3.equals(accountManager.getUserData(account, Constants.MOBILE_OPTIMIZATIONS_AVAILABLE_KEY));
		mobileOptimizations.setEnabled(available_v1 || available_v2 || available_v3);
		presenceQueueTimeout.setEnabled(available_v1 && !available_v2 && !available_v3);

		String valueStr = accountManager.getUserData(account, MobileModeFeature.MOBILE_OPTIMIZATIONS_ENABLED);
		boolean enabled = valueStr == null || Boolean.valueOf(valueStr);
		mobileOptimizations.setChecked(enabled);

		valueStr = accountManager.getUserData(account, MobileModeFeature.MOBILE_OPTIMIZATIONS_QUEUE_TIMEOUT);
		int position = (valueStr == null) ? 1 : ((Integer.parseInt(valueStr) / 3) - 1);
		presenceQueueTimeout.setSelection(position);

		if (geolocationPublish != null) {
			valueStr = accountManager.getUserData(account, GeolocationFeature.GEOLOCATION_LISTEN_ENABLED);
			geolocationListen.setChecked(valueStr != null && Boolean.parseBoolean(valueStr));

			valueStr = accountManager.getUserData(account, GeolocationFeature.GEOLOCATION_PUBLISH_ENABLED);
			geolocationPublish.setChecked(valueStr != null && Boolean.parseBoolean(valueStr));

			valueStr = accountManager.getUserData(account, GeolocationFeature.GEOLOCATION_PUBLISH_PRECISION);
			int precision = (valueStr != null) ? Integer.parseInt(valueStr) : 0;
			geolocationPrecision.setSelection(precision);

			geolocationPrecision.setEnabled(geolocationPublish.isChecked());
		}

		if (!available_v1 && !available_v2 && getMulti().get(accountJid).isConnected()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
			builder.setMessage(R.string.mobile_optimizations_not_supported).setIcon(R.drawable.icon);
			builder.setTitle(R.string.mobile_optimizations).setCancelable(true);
			AlertDialog dlg = builder.create();
			dlg.show();
		}
	}

	public void setAccount(String jidStr) {
		Account account = null;

		final AccountManager accountManager = AccountManager.get(this.getApplicationContext());

		for (Account acc : accountManager.getAccountsByType(Constants.ACCOUNT_TYPE)) {
			if (jidStr.equals(acc.name)) {
				account = acc;
				break;
			}
		}

		setAccount(account);
	}

}
