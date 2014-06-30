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

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.ChatTableMetaData;

import tigase.jaxmpp.core.client.xmpp.utils.EscapeUtils;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class MucAdapter extends SimpleCursorAdapter {

	static class ViewHolder {
		ImageView avatar;
		TextView body;
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

	private final OnClickListener nickameClickListener;
	private String participantName;

	public MucAdapter(Context context, int layout, OnClickListener nickameClickListener) {
		super(context, layout, null, cols, names, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

		this.nickameClickListener = nickameClickListener;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String tmp = null;// prefs.getString(Preferences.NICKNAME_KEY, null);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			view.setTag(holder);
			holder.nickname = (TextView) view.findViewById(R.id.chat_item_nickname);
			holder.body = (TextView) view.findViewById(R.id.chat_item_body);
			holder.timestamp = (TextView) view.findViewById(R.id.chat_item_timestamp);
			holder.avatar = (ImageView) view.findViewById(R.id.user_avatar);
		}

		holder.nickname.setOnClickListener(nickameClickListener);

		final int state = cursor.getInt(cursor.getColumnIndex(ChatTableMetaData.FIELD_STATE));

		// byte[] avatarData =
		// cursor.getBlob(cursor.getColumnIndex(VCardsCacheTableMetaData.FIELD_DATA));
		holder.avatar.setVisibility(View.GONE);

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

		if (nick != null && nick.equals(participantName)) {
			holder.body.setBackgroundResource(R.drawable.bubble_9);
		} else {
			int bubble = getOccupantBubble(nick);
			holder.body.setBackgroundResource(bubble);
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
