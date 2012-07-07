package org.tigase.mobile.chat;

import java.sql.Date;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.db.ChatTableMetaData;
import org.tigase.mobile.db.RosterTableMetaData;
import org.tigase.mobile.db.VCardsCacheTableMetaData;
import org.tigase.mobile.utils.AvatarHelper;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.utils.EscapeUtils;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ChatAdapter extends SimpleCursorAdapter {

	private final static String[] cols = new String[] { ChatTableMetaData.FIELD_TIMESTAMP, ChatTableMetaData.FIELD_BODY,
			ChatTableMetaData.FIELD_STATE, ChatTableMetaData.FIELD_JID/*, VCardsCacheTableMetaData.FIELD_DATA*/ };
	private final static int[] names = new int[] { R.id.chat_item_body };

	private String nickname;
	private final RosterDisplayTools rdt;

	public ChatAdapter(Context context, int layout, Cursor c) {
		super(context, layout, c, cols, names);
		this.rdt = new RosterDisplayTools(context);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String tmp = null;// prefs.getString(Preferences.NICKNAME_KEY, null);
		nickname = tmp == null || tmp.length() == 0 ? null : tmp;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView nickname = (TextView) view.findViewById(R.id.chat_item_nickname);
		TextView webview = (TextView) view.findViewById(R.id.chat_item_body);
		TextView timestamp = (TextView) view.findViewById(R.id.chat_item_timestamp);
		ImageView msgStatus = (ImageView) view.findViewById(R.id.msgStatus);

		final int state = cursor.getInt(cursor.getColumnIndex(ChatTableMetaData.FIELD_STATE));

		ImageView avatar = (ImageView) view.findViewById(R.id.user_avatar);

		if (state == ChatTableMetaData.STATE_INCOMING) {
			final BareJID account = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_ACCOUNT)));
			final BareJID jid = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_JID)));
			setAvatarForJid(avatar, jid, cursor);
			JaxmppCore jaxmpp = ((MessengerApplication) context.getApplicationContext()).getMultiJaxmpp().get(account);
			RosterItem ri = jaxmpp.getRoster().get(jid);
			nickname.setText(ri == null ? jid.toString() : rdt.getDisplayName(ri));

			nickname.setTextColor(context.getResources().getColor(R.color.message_his_text));
			webview.setTextColor(context.getResources().getColor(R.color.message_his_text));
			timestamp.setTextColor(context.getResources().getColor(R.color.message_his_text));

			view.setBackgroundColor(context.getResources().getColor(R.color.message_his_background));
			msgStatus.setVisibility(View.GONE);
		} else if (state == ChatTableMetaData.STATE_OUT_NOT_SENT || state == ChatTableMetaData.STATE_OUT_SENT) {
			final BareJID jid = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_AUTHOR_JID)));
			setAvatarForJid(avatar, jid, cursor);
			nickname.setText(this.nickname == null ? jid.getLocalpart() : this.nickname);

			nickname.setTextColor(context.getResources().getColor(R.color.message_mine_text));
			webview.setTextColor(context.getResources().getColor(R.color.message_mine_text));
			timestamp.setTextColor(context.getResources().getColor(R.color.message_mine_text));

			if (state == ChatTableMetaData.STATE_OUT_SENT)
				msgStatus.setVisibility(View.GONE);
			else if (state == ChatTableMetaData.STATE_OUT_NOT_SENT)
				msgStatus.setVisibility(View.VISIBLE);

			view.setBackgroundColor(context.getResources().getColor(R.color.message_mine_background));
		} else {
			msgStatus.setVisibility(View.GONE);
			nickname.setText("?");
		}

		java.text.DateFormat df = DateFormat.getTimeFormat(context);
		final String txt = EscapeUtils.escape(cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_BODY)));
		webview.setText(Html.fromHtml(txt));
		// webview.setMinimumHeight(webview.getMeasuredHeight());

		Date t = new Date(cursor.getLong(cursor.getColumnIndex(ChatTableMetaData.FIELD_TIMESTAMP)));
		timestamp.setText(df.format(t));

	}
	
	private void setAvatarForJid(ImageView avatar, BareJID jid, Cursor cursor) {
		// old implementation
//		Bitmap bmp = AvatarHelper.getAvatar(jid, cursor, VCardsCacheTableMetaData.FIELD_DATA);
//		if (bmp != null) {
//			avatar.setImageBitmap(bmp);
//		} else {
//			avatar.setImageResource(R.drawable.user_avatar);
//		}
		
		// roster uses this below
		//AvatarHelper.setAvatarToImageView(jid, avatar);
		// but it is not good as in chat async avatar loading while here 
		// synchronized loading is better as we can use results from cache
		avatar.setImageBitmap(AvatarHelper.getAvatar(jid));
	}
}
