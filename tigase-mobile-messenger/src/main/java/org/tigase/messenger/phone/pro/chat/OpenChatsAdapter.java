package org.tigase.messenger.phone.pro.chat;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.providers.OpenChatsProvider;
import org.tigase.messenger.phone.pro.roster.RosterAdapterHelper;

import tigase.jaxmpp.android.chat.OpenChatTableMetaData;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.method.SingleLineTransformationMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class OpenChatsAdapter extends SimpleCursorAdapter {

	private final static String[] cols = new String[] { OpenChatTableMetaData.FIELD_ID, OpenChatTableMetaData.FIELD_ACCOUNT, 
		OpenChatTableMetaData.FIELD_JID, OpenChatsProvider.FIELD_NAME, OpenChatsProvider.FIELD_UNREAD_COUNT, 
		OpenChatTableMetaData.FIELD_TYPE, OpenChatsProvider.FIELD_STATE, OpenChatsProvider.FIELD_LAST_MESSAGE };
	private final static int[] names = new int[] { R.id.name };	
	
	static class ViewHolder {
		ImageView itemAvatar;
		TextView itemLastMessage;
		TextView itemName;
		ImageView itemPresence;
	}
	
	public OpenChatsAdapter(Context context, int layout, Cursor c) {
		super(context, layout, c, cols, names);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// GroupRosterAdapter and FlatRosterAdapter are using code

		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			view.setTag(holder);
			holder.itemName = (TextView) view.findViewById(R.id.name);
			holder.itemLastMessage = (TextView) view.findViewById(R.id.message);

			holder.itemAvatar = (ImageView) view.findViewById(R.id.avatar);
			holder.itemPresence = (ImageView) view.findViewById(R.id.status);
		}

		int unreadCount = cursor.getInt(cursor.getColumnIndex(OpenChatsProvider.FIELD_UNREAD_COUNT));

		holder.itemName.setTransformationMethod(SingleLineTransformationMethod.getInstance());
		String name = cursor.getString(cursor.getColumnIndex(OpenChatsProvider.FIELD_NAME));
		holder.itemName.setText(name);
		holder.itemName.setTypeface(null, unreadCount > 0 ? Typeface.BOLD : Typeface.NORMAL);
		
//		holder.itemLastMessage.setText(String.valueOf(unreadCount) + " unread messages"); 

		holder.itemLastMessage.setTransformationMethod(SingleLineTransformationMethod.getInstance());
		holder.itemLastMessage.setText(cursor.getString(cursor.getColumnIndex(OpenChatsProvider.FIELD_LAST_MESSAGE)));
		holder.itemLastMessage.setTypeface(null, unreadCount > 0 ? Typeface.BOLD : Typeface.NORMAL);
		
		int type = 0;
		try {
			type = cursor.getInt(cursor.getColumnIndex(OpenChatTableMetaData.FIELD_TYPE));
		}
		catch (Exception ex) {
			// should not happend - it may occur if field is null, but it should not be null
		} 
		
		switch (type) {
			case OpenChatTableMetaData.TYPE_CHAT:
				holder.itemAvatar.setImageResource(R.drawable.user_avatar);
				// add support for avatars!!
//				AvatarHelper.setAvatarToImageView(jid, holder.itemAvatar);
				break;
			case OpenChatTableMetaData.TYPE_MUC:
				holder.itemAvatar.setImageResource(R.drawable.group_chat);
				break;
		}
		
		int state = 0;
		try {
			state = cursor.getInt(cursor.getColumnIndex(OpenChatsProvider.FIELD_STATE));
		} catch (Exception ex) {
			// should not happen
		}
		int resource = RosterAdapterHelper.cPresenceToImageResource(state);		
		holder.itemPresence.setImageResource(resource);
	}	
}
