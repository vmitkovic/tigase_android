package org.tigase.messenger.phone.pro;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {

	public static final String ACTIVITIES_ENABLED = "activities_enabled";
	
	public static final String ACTIVITY_IN_VEHICLE_STATUS = "activities_in_vehicle_status";
	
	public static final String ACTIVITY_IN_VEHICLE_DESCR = "activities_in_vehicle_description";
	
	public static final String AUTOSTART_KEY = "autostart";
	
	public static final String AWAY_PRIORITY_KEY = "away_priority";
	
	public static final String CHAT_LAYOUT_KEY = "chat_layout";
	
	public static final String DEFAULT_PRIORITY_KEY = "default_priority";
	
	public static final String ENABLE_CHAT_STATE_SUPPORT_KEY = "chat_state_enabled";
	
	public static final String ENTER_TO_SEND_KEY = "enter_to_send";
	
	public static final String KEEPALIVE_TIME_KEY = "keepalive_time";
	
	public static final String NOTIFICATION_CHAT_KEY = "notification_chat";

	public static final String NOTIFICATION_FILE_KEY = "notification_file";
	
	public static final String NOTIFICATION_MUC_ERROR_KEY = "notification_mucerror";
	
	public static final String NOTIFICATION_MUC_MENTIONED_KEY = "notification_muc";
	
	public static final String NOTIFICATION_SOUND_KEY = "notification_sound";
	
	public static final String NOTIFICATION_VIBRATE_KEY = "notification_vibrate";
	
	public static final String NOTIFICATION_WARNING_KEY = "notification_warning";
	
	public static final String ROSTER_LAYOUT_KEY = "roster_layout";

	public static final String ROSTER_SORTING_KEY = "roster_sorting";
	
	public static final String ROSTER_VERSION_KEY = "roster_version";

	public static final String SERVICE_ACTIVATED = "service_activated";
	
	public static final String SHOW_OFFLINE = "show_offline";
	
	public static final String MAIN_WINDOW_TABS = "main_window_tabs";

	public static SharedPreferences getDefaultSharedPreferences(Context context) {
		return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_MULTI_PROCESS);
	}
	
}
