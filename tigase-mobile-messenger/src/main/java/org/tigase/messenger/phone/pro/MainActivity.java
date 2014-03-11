package org.tigase.messenger.phone.pro;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {

	private EditText msgInput;
	private Button sendBtn;
	
	private Messenger service;
	
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// TODO Auto-generated method stub
			MainActivity.this.service = new Messenger(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			// TODO Auto-generated method stub
			service = null;
		}
		
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		msgInput = (EditText) this.findViewById(R.id.msg_input);
		sendBtn = (Button) this.findViewById(R.id.send_btn);
		sendBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				String body = msgInput.getText().toString();
				Message msg = Message.obtain(null, JaxmppService.SEND_MESSAGE);
				Bundle data = new Bundle();
				data.putString("account", "andrzej.wojcik@tigase.im");
				data.putString("to", "andrzej.wojcik@tigase.org");
				data.putString("message", body);
				msg.setData(data);
				
				try {
					service.send(msg);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				MainActivity.this.finish();				
				Process.killProcess(Process.myPid());
			}			
		});
		startService(new Intent(this, JaxmppService.class));
		bindService(new Intent(MainActivity.this, JaxmppService.class), connection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onDestroy() {
		unbindService(connection);
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
