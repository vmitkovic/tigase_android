package org.tigase.messenger.phone.pro;

import org.tigase.messenger.phone.pro.service.JaxmppService;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;
import org.tigase.messenger.phone.pro.utils.ImageHelper;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class MessengerApplication extends Application {

	@Override
    public void onCreate() {
        super.onCreate();
        AvatarHelper.initilize(this);
        
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setContentTitle("Messenger").setContentText("Started");
		NotificationManager mNotificationManager =
			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);	
		mNotificationManager.notify(0, builder.build());        
		Intent startServiceIntent = new Intent(getApplicationContext(), JaxmppService.class);
		startServiceIntent.setAction("connect-all");
        startService(startServiceIntent); 
    }

	@Override
	public void onTrimMemory(int level) {
		ImageHelper.onTrimMemory(level);
	}	
}
