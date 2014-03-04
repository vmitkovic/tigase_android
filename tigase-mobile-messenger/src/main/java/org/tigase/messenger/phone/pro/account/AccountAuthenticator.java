package org.tigase.messenger.phone.pro.account;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class AccountAuthenticator extends AbstractAccountAuthenticator {

	public static final String ACCOUNT_TYPE = "org.tigase.messenger";
	
	private final Context context;
	
	public AccountAuthenticator(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
	    final Intent intent = new Intent(context, AuthenticatorActivity.class);
	    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
	    final Bundle bundle = new Bundle();
	    bundle.putParcelable(AccountManager.KEY_INTENT, intent);
	    return bundle;
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse arg0,
			Account arg1, Bundle arg2) throws NetworkErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
			String authTokenType, Bundle options) throws NetworkErrorException {
		final AccountManager am = AccountManager.get(context);		
		final String password = am.getPassword(account);
		
		if (password != null) {
			final Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
			result.putString(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
			result.putString(AccountManager.KEY_AUTHTOKEN, password);
			return result;
		}
		
		final Intent intent = new Intent(context, AuthenticatorActivity.class);
		intent.putExtra("account", account);
		intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle result = new Bundle();
		result.putParcelable(AccountManager.KEY_INTENT, intent);
		return result;
	}

	@Override
	public String getAuthTokenLabel(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse arg0, Account arg1,
			String[] arg2) throws NetworkErrorException {
		// TODO Auto-generated method stub
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;	
    }

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response,
			Account account, String arg2, Bundle arg3)
			throws NetworkErrorException {
        final Intent intent = new Intent(context, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
	}

}
