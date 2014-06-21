package org.tigase.messenger.phone.pro.preferences;

import org.tigase.messenger.phone.pro.Constants;
import org.tigase.messenger.phone.pro.Features;
import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.AccountAuthenticator;
import org.tigase.messenger.phone.pro.account.AuthenticatorService;
import org.tigase.messenger.phone.pro.service.GeolocationFeature;
import org.tigase.messenger.phone.pro.service.JaxmppService;
import org.tigase.messenger.phone.pro.service.MobileModeFeature;

import tigase.jaxmpp.core.client.BareJID;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
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

	public final boolean isMobileAvailable(BareJID accountJid, String feature) {
		try {
			return jaxmppService.hasStreamFeature(accountJid.toString(), "mobile", feature);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean isConnected(String accountJid) {
		try {
			return jaxmppService.isConnected(accountJid);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	private volatile Account account;
	private BareJID accountJid;
	private CompoundButton geolocationListen;
	private Spinner geolocationPrecision;

	private CompoundButton geolocationPublish;

	private View layout;

	private CompoundButton mobileOptimizations;

	private Spinner presenceQueueTimeout;

	private IJaxmppService jaxmppService = null;
	private ServiceConnection jaxmppServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			synchronized (AccountAdvancedPreferencesFragment.this) {
				jaxmppService = IJaxmppService.Stub.asInterface(service);
				if (account != null) {
					update();
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			jaxmppService = null;
		}
		
	};	
	
	private Context getApplicationContext() {
		return getActivity().getApplicationContext();
	}

//	private final MultiJaxmpp getMulti() {
//		return ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();
//	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (jaxmppService != null) {
			try {
				jaxmppService.updateConfiguration();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		getActivity().unbindService(jaxmppServiceConnection);		
	}

	@Override
	public void onResume() {
		Intent intent = new Intent(getActivity(), JaxmppService.class);
		intent.putExtra("ID", "AIDL");
		getActivity().bindService(intent, jaxmppServiceConnection, Context.BIND_AUTO_CREATE);		
		super.onResume();
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
				if (account != null)
					accountManager.setUserData(account, MobileModeFeature.MOBILE_OPTIMIZATIONS_ENABLED, String.valueOf(isChecked));
//				getMulti().get(accountJid).getSessionObject().setUserProperty(MobileModeFeature.MOBILE_OPTIMIZATIONS_ENABLED,
//						isChecked);
			}

		});

		presenceQueueTimeout.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
				int value = 3 * (position + 1);
				if (account != null)
					accountManager.setUserData(account, MobileModeFeature.MOBILE_OPTIMIZATIONS_QUEUE_TIMEOUT, String.valueOf(value));
//				getMulti().get(accountJid).getSessionObject().setUserProperty(
//						MobileModeFeature.MOBILE_OPTIMIZATIONS_QUEUE_TIMEOUT, value);
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
					if (account != null)
						accountManager.setUserData(account, GeolocationFeature.GEOLOCATION_LISTEN_ENABLED,
							String.valueOf(isChecked));
//					JaxmppCore jaxmpp = getMulti().get(accountJid);
//					GeolocationFeature.updateGeolocationSettings(account, jaxmpp, getApplicationContext());
				}
			});
			geolocationPublish.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (account != null)
						accountManager.setUserData(account, GeolocationFeature.GEOLOCATION_PUBLISH_ENABLED,
							String.valueOf(isChecked));
//					JaxmppCore jaxmpp = getMulti().get(accountJid);
//					GeolocationFeature.updateGeolocationSettings(account, jaxmpp, getApplicationContext());
					geolocationPrecision.setEnabled(isChecked);
				}
			});
			geolocationPrecision.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
					int precision = position;
					if (account != null)
						accountManager.setUserData(account, GeolocationFeature.GEOLOCATION_PUBLISH_PRECISION,
							String.valueOf(precision));
//					JaxmppCore jaxmpp = getMulti().get(accountJid);
//					GeolocationFeature.updateGeolocationSettings(account, jaxmpp, getApplicationContext());
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}
			});
		}

		return layout;
	}

	public void setAccount(final Account account) {
		synchronized (this) {
			//final AccountManager accountManager = AccountManager.get(this.getApplicationContext());
			Log.v(TAG, "setting account = " + account.name);
			accountJid = BareJID.bareJIDInstance(account.name);
			this.account = account;
			if (jaxmppService != null) {
				update();
			}
		}
	}
	
	private void update() {
		final AccountManager accountManager = AccountManager.get(this.getApplicationContext());
		Log.v(TAG, "setting account = " + account.name);
		accountJid = BareJID.bareJIDInstance(account.name);
		boolean available_v1 = isMobileAvailable(accountJid, Features.MOBILE_V1)
				&& Features.MOBILE_V1.equals(accountManager.getUserData(account, Constants.MOBILE_OPTIMIZATIONS_AVAILABLE_KEY));
		boolean available_v2 = isMobileAvailable(accountJid, Features.MOBILE_V2)
				&& Features.MOBILE_V2.equals(accountManager.getUserData(account, Constants.MOBILE_OPTIMIZATIONS_AVAILABLE_KEY));
		boolean available_v3 = isMobileAvailable(accountJid, Features.MOBILE_V3)
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

		if (!available_v1 && !available_v2 && isConnected(accountJid.toString())) {
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

		for (Account acc : accountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)) {
			if (jidStr.equals(acc.name)) {
				account = acc;
				break;
			}
		}

		setAccount(account);
	}

}
