package org.tigase.messenger.phone.pro;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class MessengerApplication extends Application {

	@Override
    public void onCreate() {
        super.onCreate();
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setContentTitle("Messenger").setContentText("Started");
		NotificationManager mNotificationManager =
			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);	
		mNotificationManager.notify(0, builder.build());        
        startService(new Intent(getApplicationContext(), JaxmppService.class)); 
    }
	
}
