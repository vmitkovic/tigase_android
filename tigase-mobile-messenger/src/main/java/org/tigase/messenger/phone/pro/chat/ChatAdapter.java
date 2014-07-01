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
package org.tigase.messenger.phone.pro.chat;

import java.util.Map;

import org.tigase.messenger.phone.pro.MessengerApplication;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.ChatTableMetaData;
//import org.tigase.messenger.phone.pro.utils.AvatarHelper;

import org.tigase.messenger.phone.pro.db.providers.ChatHistoryProvider;
import org.tigase.messenger.phone.pro.service.GeolocationFeature;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.utils.EscapeUtils;
import tigase.jaxmpp.j2se.connectors.socket.StreamListener;
import tigase.jaxmpp.j2se.connectors.socket.XMPPDomBuilderHandler;
import tigase.jaxmpp.j2se.xml.J2seElement;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class ChatAdapter extends SimpleCursorAdapter {

	private static final String TAG = "ChatAdapter";
	
	static class ViewHolder {
		ImageView avatar;
		ImageView msgStatus;
		TextView nickname;
		TextView timestamp;
		TextView webview;
		ImageButton actionButton;
	}

	private final static String[] cols = new String[] { ChatTableMetaData.FIELD_ID, 
			ChatTableMetaData.FIELD_TIMESTAMP, ChatTableMetaData.FIELD_BODY,
			ChatTableMetaData.FIELD_STATE, ChatTableMetaData.FIELD_JID /*
																		 * ,
																		 * VCardsCacheTableMetaData
																		 * .
																		 * FIELD_DATA
																		 */
			, ChatTableMetaData.FIELD_ITEM_TYPE, ChatTableMetaData.FIELD_DATA};

	private final static int[] names = new int[] { R.id.chat_item_body };

	private static void setAvatarForJid(ImageView avatar, BareJID jid, Cursor cursor) {
		// roster uses this below
		// AvatarHelper.setAvatarToImageView(jid, avatar);
		// but it is not good as in chat async avatar loading while here
		// synchronized loading is better as we can use results from cache

		avatar.setImageBitmap(AvatarHelper.getAvatar(jid));
	}

	private String nickname;
	private String recipientName = null;

	public ChatAdapter(Context context, int layout) {
		super(context, layout, null, cols, names, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String tmp = null;// prefs.getString(Preferences.NICKNAME_KEY, null);
		nickname = tmp == null || tmp.length() == 0 ? null : tmp;
	}
	
	//---------------------------------
	@Override
	public int getViewTypeCount() {
		return 2;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Cursor c = getCursor();
		c.moveToPosition(position);
		int viewId = (convertView == null) ? -1 : convertView.getId();
		int type = c.getInt(c.getColumnIndex(ChatTableMetaData.FIELD_ITEM_TYPE));
		if (type != viewId) {
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			switch (type) {
			case ChatTableMetaData.ITEM_TYPE_LOCALITY:
				convertView = inflater.inflate(R.layout.chat_item_map, parent, false);
				convertView.setId(type);
				break;
			default:
			case ChatTableMetaData.ITEM_TYPE_MESSAGE:
				convertView = inflater.inflate(R.layout.chat_item_his, parent, false);
				convertView.setId(type);
				break;
			}
		}
		bindView(convertView, this.mContext, mCursor);
		return convertView;
	}
	//---------------------------------

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			view.setTag(holder);
			holder.nickname = (TextView) view.findViewById(R.id.chat_item_nickname);
			holder.webview = (TextView) view.findViewById(R.id.chat_item_body);
			holder.actionButton = (ImageButton) view.findViewById(R.id.actionButton);
			holder.timestamp = (TextView) view.findViewById(R.id.chat_item_timestamp);
			holder.avatar = (ImageView) view.findViewById(R.id.user_avatar);
			holder.msgStatus = (ImageView) view.findViewById(R.id.msgStatus);
		}

		final int id = cursor.getInt(cursor.getColumnIndex(ChatTableMetaData.FIELD_ID));
		final int state = cursor.getInt(cursor.getColumnIndex(ChatTableMetaData.FIELD_STATE));

		if (state == ChatTableMetaData.STATE_INCOMING || state == ChatTableMetaData.STATE_INCOMING_UNREAD) {
			final BareJID account = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_ACCOUNT)));
			final BareJID jid = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_JID)));
			setAvatarForJid(holder.avatar, jid, cursor);
			// direct use of Jaxmpp in this context is not allowed
			// JaxmppCore jaxmpp = ((MessengerApplication)
			// context.getApplicationContext()).getMultiJaxmpp().get(account);
			// RosterItem ri = jaxmpp.getRoster().get(jid);
			// holder.nickname.setText(ri == null ? jid.toString() :
			// RosterDisplayTools.getDisplayName(ri));
			holder.nickname.setText(recipientName == null ? jid.toString() : recipientName);

			holder.nickname.setTextColor(context.getResources().getColor(R.color.message_his_text));
			holder.webview.setTextColor(context.getResources().getColor(R.color.message_his_text));
			holder.timestamp.setTextColor(context.getResources().getColor(R.color.message_his_text));

			// view.setBackgroundColor(context.getResources().getColor(R.color.message_his_background));
			holder.webview.setBackgroundResource(R.drawable.bubble_5);
			holder.msgStatus.setVisibility(View.GONE);
		} else if (state == ChatTableMetaData.STATE_OUT_NOT_SENT || state == ChatTableMetaData.STATE_OUT_SENT) {
			final BareJID jid = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_AUTHOR_JID)));
			setAvatarForJid(holder.avatar, jid, cursor);
			holder.nickname.setText(this.nickname == null ? jid.getLocalpart() : this.nickname);

			holder.nickname.setTextColor(context.getResources().getColor(R.color.message_mine_text));
			holder.webview.setTextColor(context.getResources().getColor(R.color.message_mine_text));
			holder.timestamp.setTextColor(context.getResources().getColor(R.color.message_mine_text));

			if (state == ChatTableMetaData.STATE_OUT_SENT)
				holder.msgStatus.setVisibility(View.GONE);
			else if (state == ChatTableMetaData.STATE_OUT_NOT_SENT)
				holder.msgStatus.setVisibility(View.VISIBLE);

			holder.webview.setBackgroundResource(R.drawable.bubble_9);
			// view.setBackgroundColor(context.getResources().getColor(R.color.message_mine_background));
		} else {
			holder.msgStatus.setVisibility(View.GONE);
			holder.nickname.setText("?");
		}

		// java.text.DateFormat df = DateFormat.getTimeFormat(context);
		String txt = EscapeUtils.escape(cursor.getString(cursor.getColumnIndex(ChatTableMetaData.FIELD_BODY)));

		// webview.setMinimumHeight(webview.getMeasuredHeight());

		if (holder.actionButton != null) {
			holder.actionButton.setClickable(true);
			holder.actionButton.setFocusable(false);
			holder.actionButton.setFocusableInTouchMode(false);
			holder.actionButton.setImageResource(android.R.drawable.ic_menu_mapmode);
			holder.actionButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Cursor c = mContext.getContentResolver().query(Uri.parse(ChatHistoryProvider.CHAT_URI + "/whatever/" + id),
							null, null, null, null);
					Log.v(TAG, "map action button clicked");
					try {
						c.moveToNext();
					String dataStr = c.getString(c.getColumnIndex(ChatTableMetaData.FIELD_DATA));
					Log.v(TAG, "loaded data = " + dataStr);
					XMPPDomBuilderHandler handler = new XMPPDomBuilderHandler(new StreamListener() {
						@Override
						public void nextElement(Element element) {
							try {
								Log.v(TAG, "parsed, now decoding address..");
								Address address = GeolocationFeature.fromElement(new J2seElement(element));
								double lat = address.getLatitude();
								double lng = address.getLongitude();
								String uriString = address.getUrl() == null ? ("geo:"+lat+","+lng+"?z=14") : address.getUrl();
								Log.v(TAG, "calling intent for " + uriString);
								Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
								if (intent.resolveActivity(ChatAdapter.this.mContext.getPackageManager()) != null) {
									ChatAdapter.this.mContext.startActivity(intent);
								} else {
									intent.setData(Uri.parse("http://maps.google.com/maps?q="+lat+","+lng+"&z=14"));
									ChatAdapter.this.mContext.startActivity(intent);
								}
								
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}

						@Override
						public void xmppStreamClosed() {
						}

						@Override
						public void xmppStreamOpened(Map<String, String> attribs) {
						}
						
					});
					SimpleParser parser = SingletonFactory.getParserInstance();
					char[] data = dataStr.toCharArray();
					Log.v(TAG, "parsing..");
					parser.parse(handler, data, 0, data.length);					
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}			
			});
		}
		
		Spanned sp = Html.fromHtml(txt.replace("\n", "<br/>"));
		holder.webview.setText(sp);
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

	public void setRecipientName(String name) {
		this.recipientName = name;
	}
}
