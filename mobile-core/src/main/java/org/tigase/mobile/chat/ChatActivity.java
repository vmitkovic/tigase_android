package org.tigase.mobile.chat;

import org.tigase.mobile.R;
import org.tigase.mobile.TigaseMobileMessengerActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class ChatActivity extends FragmentActivity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_activity);


		if (savedInstanceState == null) {
			Bundle arguments = new Bundle();
			arguments.putLong("chatId", getIntent().getLongExtra("chatId",0));
			arguments.putString("account", getIntent().getStringExtra("account"));
			ChatHistoryFragment fragment = new ChatHistoryFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.main_detail_container, fragment).commit();
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
