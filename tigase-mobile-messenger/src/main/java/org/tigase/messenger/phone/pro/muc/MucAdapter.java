/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package org.tigase.messenger.phone.pro.muc;

import org.tigase.messenger.phone.pro.Preferences;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.ChatTableMetaData;
import org.tigase.messenger.phone.pro.ui.Layouts;

import tigase.jaxmpp.core.client.xmpp.utils.EscapeUtils;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class MucAdapter extends SimpleCursorAdapter {

	private static final String TAG = "MucAdapter";
	
	static class ViewHolder {
		ImageView avatar;
		TextView body;
		ImageView msgStatus;
		TextView nickname;
		TextView timestamp;
	}

	private final static String[] cols = new String[] { ChatTableMetaData.FIELD_TIMESTAMP, ChatTableMetaData.FIELD_BODY,
			ChatTableMetaData.FIELD_STATE, ChatTableMetaData.FIELD_JID /*
																		 * ,
																		 * VCardsCacheTableMetaData
																		 * .
																		 * FIELD_DATA
																		 */};

	private final static int[] names = new int[] { R.id.chat_item_body };
	
	static int getOccupantBubble(final String nick) {
		if (nick == null)
			return R.drawable.bubble_1;

		final int i = nick.hashCode();
		final int color = Math.abs(i ^ (i >>> 5)) % 14;

		switch (color) {
		case 0:
			return R.drawable.bubble_1;
		case 1:
			return R.drawable.bubble_2;
		case 2:
			return R.drawable.bubble_3;
		case 3:
			return R.drawable.bubble_4;
		case 4:
			return R.drawable.bubble_5;
		case 5:
			return R.drawable.bubble_6;
		case 6:
			return R.drawable.bubble_7;
		case 7:
			return R.drawable.bubble_8;
		case 8:
			return R.drawable.bubble_10;
		case 9:
			return R.drawable.bubble_11;
		case 10:
			return R.drawable.bubble_12;
		case 11:
			return R.drawable.bubble_13;
		case 12:
			return R.drawable.bubble_14;
		case 13:
			return R.drawable.bubble_15;
		case 14:
			return R.drawable.bubble_16;
		default:
			return R.drawable.bubble_1;
		}
	}
	
	static int getOccupantColor(final String nick) {
		if (nick == null)
			return R.color.mucmessage_his_nickname_0;

		final int i = nick.hashCode();
		final int color = Math.abs(i ^ (i >>> 5)) % 17;

		switch (color) {
		case 0:
			return R.color.mucmessage_his_nickname_0;
		case 1:
			return R.color.mucmessage_his_nickname_1;
		case 2:
			return R.color.mucmessage_his_nickname_2;
		case 3:
			return R.color.mucmessage_his_nickname_3;
		case 4:
			return R.color.mucmessage_his_nickname_4;
		case 5:
			return R.color.mucmessage_his_nickname_5;
		case 6:
			return R.color.mucmessage_his_nickname_6;
		case 7:
			return R.color.mucmessage_his_nickname_7;
		case 8:
			return R.color.mucmessage_his_nickname_8;
		case 9:
			return R.color.mucmessage_his_nickname_9;
		case 10:
			return R.color.mucmessage_his_nickname_10;
		case 11:
			return R.color.mucmessage_his_nickname_11;
		case 12:
			return R.color.mucmessage_his_nickname_12;
		case 13:
			return R.color.mucmessage_his_nickname_13;
		case 14:
			return R.color.mucmessage_his_nickname_14;
		case 15:
			return R.color.mucmessage_his_nickname_15;
		case 16:
			return R.color.mucmessage_his_nickname_16;
		default:
			return R.color.mucmessage_his_nickname_0;
		}
	}


	private final OnClickListener nickameClickListener;
	private String participantName;
	private Layouts.ChatLayout chatLayoutMine;
	private Layouts.ChatLayout chatLayoutHis;

	
	public MucAdapter(Context context, int layout, OnClickListener nickameClickListener) {
		super(context, layout, null, cols, names, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

		this.nickameClickListener = nickameClickListener;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String tmp = null;// prefs.getString(Preferences.NICKNAME_KEY, null);
		String layoutType = prefs.getString(Preferences.CHAT_LAYOUT_KEY, "bubble");
		Log.v(TAG, "got chat layout = " + layoutType);
		if ("bubble".equals(layoutType)) {
			chatLayoutMine = Layouts.CHAT_BUBBLE;
			chatLayoutHis = Layouts.CHAT_BUBBLE;
		} else {
			chatLayoutMine = Layouts.CHAT_SIMPLE_MINE;
			chatLayoutHis = Layouts.CHAT_SIMPLE_HIS;
		}
	}

	@Override
	public int getViewTypeCount() {
		return (chatLayoutMine == chatLayoutHis) ? 1 : 2;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Cursor c = getCursor();
		c.moveToPosition(position);
//		String account = c.getString(c.getColumnIndex(ChatTableMetaData.FIELD_ACCOUNT));
//		String author = c.getString(c.getColumnIndex(ChatTableMetaData.FIELD_AUTHOR_JID));
		final String nick = c.getString(c.getColumnIndex(ChatTableMetaData.FIELD_AUTHOR_NICKNAME));
		boolean mine = (nick != null && nick.equals(participantName));//account.equals(author);
		Layouts.ChatLayout chatLayout = (mine) ? chatLayoutMine : chatLayoutHis;
		int viewId = (convertView == null) ? -1 : convertView.getId();
		int type = ChatTableMetaData.ITEM_TYPE_MESSAGE;//c.getInt(c.getColumnIndex(ChatTableMetaData.FIELD_ITEM_TYPE));
		int maxViews = getViewTypeCount();
		int expectedViewId = (chatLayoutMine == chatLayoutHis || mine) ? type : type + (maxViews/2);
		if (expectedViewId != viewId) {
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			switch (type) {
			// maybe we would add it later or add other here?
//			case ChatTableMetaData.ITEM_TYPE_FILE:
//				convertView = inflater.inflate(chatLayout.file, parent, false);
//				convertView.setId(type);
//				break;
//			case ChatTableMetaData.ITEM_TYPE_IMAGE:
//				convertView = inflater.inflate(chatLayout.image, parent, false);
//				convertView.setId(type);
//				break;
//			case ChatTableMetaData.ITEM_TYPE_VIDEO:
//				convertView = inflater.inflate(chatLayout.video, parent, false);
//				convertView.setId(type);
//				break;
//			case ChatTableMetaData.ITEM_TYPE_LOCALITY:
//				convertView = inflater.inflate(chatLayout.locality, parent, false);
//				convertView.setId(type);
//				break;
			default:
			case ChatTableMetaData.ITEM_TYPE_MESSAGE:
				convertView = inflater.inflate(chatLayout.message, parent, false);
				convertView.setId(expectedViewId);
				break;
			}
		}
		bindView(convertView, this.mContext, mCursor);
		return convertView;
	}
	
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			view.setTag(holder);
			holder.nickname = (TextView) view.findViewById(R.id.chat_item_nickname);
			holder.body = (TextView) view.findViewById(R.id.chat_item_body);
			holder.msgStatus = (ImageView) view.findViewById(R.id.msgStatus);
			holder.timestamp = (TextView) view.findViewById(R.id.chat_item_timestamp);
			holder.avatar = (ImageView) view.findViewById(R.id.user_avatar);
		}

		holder.nickname.setOnClickListener(nickameClickListener);

		final int state = cursor.getInt(cursor.getColumnIndex(ChatTableMetaData.FIELD_STATE));

		// byte[] avatarData =
		// cursor.getBlob(cursor.getColumnIndex(VCardsCacheTableMetaData.FIELD_DATA));
		holder.avatar.setVisibility(View.GONE);
		holder.msgStatus.setVisibility(View.GONE);

		// final BareJID account =
		// BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_ACCOUNT)));
		final String nick = cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_AUTHOR_NICKNAME));

		// JaxmppCore jaxmpp = ((MessengerApplication)
		// context.getApplicationContext()).getMultiJaxmpp().get(account);
		holder.nickname.setText(nick);

		String bd = cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_BODY));
		if (bd == null) {
			bd = "(null)";
		}

		if ((chatLayoutMine == chatLayoutHis)) {
			if (nick != null && nick.equals(participantName)) {
				holder.body.setBackgroundResource(R.drawable.bubble_9);
			} else {
				int bubble = getOccupantBubble(nick);
				holder.body.setBackgroundResource(bubble);
			}
		}
		else {
			if (nick != null && !nick.equals(participantName)) {
				int color = getOccupantColor(participantName);
				holder.nickname.setTextColor(context.getResources().getColor(color));
			} else {
				holder.nickname.setTextColor(context.getResources().getColor(R.color.mucmessage_mine_nickname));
			}
		}

		// java.text.DateFormat df = DateFormat.getTimeFormat(context);

		if (bd != null && bd.startsWith("/me ")) {
			String t = nick + " " + bd.substring(4);
			final String txt = EscapeUtils.escape(t);
			holder.body.setText(Html.fromHtml(txt.replace("\n", "<br/>").replace(participantName,
					"<b>" + participantName + "</b>")));

		} else {
			final String txt = EscapeUtils.escape(bd);
			holder.body.setText(Html.fromHtml(txt.replace("\n", "<br/>").replace(participantName,
					"<b>" + participantName + "</b>")));
		}

		// webview.setMinimumHeight(webview.getMeasuredHeight());

		// Date t = new
		// Date(cursor.getLong(cursor.getColumnIndex(ChatTableMetaData.FIELD_TIMESTAMP)));
		// holder.timestamp.setText(df.format(t));
		long ts = cursor.getLong(cursor.getColumnIndex(ChatTableMetaData.FIELD_TIMESTAMP));
		CharSequence tsStr =
		// DateUtils.isToday(ts)
		// ? DateUtils.getRelativeTimeSpanString(ts, System.currentTimeMillis(),
		// DateUtils.MINUTE_IN_MILLIS) :
		DateUtils.getRelativeDateTimeString(mContext, ts, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
		holder.timestamp.setText(tsStr);

	}

	public void setParticipantName(String name) {
		this.participantName = name;
	}
}
