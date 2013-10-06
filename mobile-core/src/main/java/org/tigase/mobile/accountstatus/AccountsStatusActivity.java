package org.tigase.mobile.accountstatus;

import org.tigase.mobile.R;
import org.tigase.mobile.TigaseMobileMessengerActivity;
import org.tigase.mobile.preferences.AccountAdvancedPreferencesFragment;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class AccountsStatusActivity extends FragmentActivity {

	AccountAdvancedPreferencesFragment detailFragment = null;
	AccountsStatusFragment fragment = null;

	@TargetApi(11)
	public void enableHomeButton() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accounts_status_activity);

		if (savedInstanceState == null) {
			Bundle arguments = new Bundle();
			fragment = new AccountsStatusFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction().add(R.id.accounts_status, fragment, "master").commit();

			if (findViewById(R.id.account_detail_container) != null) {
				detailFragment = new AccountAdvancedPreferencesFragment();
				getSupportFragmentManager().beginTransaction().add(R.id.account_detail_container, detailFragment, "details").commit();
			}
		}
		this.getSupportFragmentManager().executePendingTransactions();
		fragment = (AccountsStatusFragment) this.getSupportFragmentManager().findFragmentByTag("master");
		detailFragment = (AccountAdvancedPreferencesFragment) this.getSupportFragmentManager().findFragmentByTag("details");

		if (detailFragment != null) {
			fragment.setAccountSelectedListener(new AccountsStatusFragment.AccountSelectionListener() {

				@Override
				public void accountSelected(String jid) {
					detailFragment.setAccount(jid);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						forceOptionsMenuRefresh();
					}
				}

			});
		}
		super.onStart();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			enableHomeButton();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpTo(this, new Intent(this, TigaseMobileMessengerActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void forceOptionsMenuRefresh() {
		this.invalidateOptionsMenu();
	}
}
