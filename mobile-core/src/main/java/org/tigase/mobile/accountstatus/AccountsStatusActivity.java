package org.tigase.mobile.accountstatus;

import org.tigase.mobile.R;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class AccountsStatusActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accounts_status_activity);
		
		if (savedInstanceState == null) {
			Bundle arguments = new Bundle();
			AccountsStatusFragment fragment = new AccountsStatusFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.accounts_status, fragment).commit();
		}		
		
	}
	
}
