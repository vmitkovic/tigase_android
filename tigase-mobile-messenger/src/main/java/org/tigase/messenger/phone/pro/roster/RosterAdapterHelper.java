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
package org.tigase.messenger.phone.pro.roster;

//import org.tigase.messenger.phone.pro.ClientIconsTool;
//import org.tigase.messenger.phone.pro.MessengerApplication;
//import org.tigase.messenger.phone.pro.R;
//import org.tigase.messenger.phone.pro.db.GeolocationTableMetaData;
//import org.tigase.messenger.phone.pro.db.RosterTableMetaData;
//import org.tigase.messenger.phone.pro.pubsub.GeolocationModule;
//import org.tigase.messenger.phone.pro.utils.AvatarHelper;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.GeolocationTableMetaData;
import org.tigase.messenger.phone.pro.db.RosterTableMetaData;
import org.tigase.messenger.phone.pro.db.providers.GeolocationProvider;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.text.method.SingleLineTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Used for common code used by *RosterAdapter classes
 * 
 * @author andrzej
 * 
 */
public class RosterAdapterHelper {

	/**
	 * Class used to store instances of rendering components for faster lookup
	 * 
	 * @author andrzej
	 * 
	 */
	static class ViewHolder {
		ImageView clientTypeIndicator;
		ImageView itemAvatar;
		TextView itemDescription;
		TextView itemJid;
		ImageView itemPresence;
		View openChatNotifier;
	}

	private static final String TAG = "RosterAdapterHelper";

	/**
	 * Created to remove duplication of code for binding view for
	 * FlatRosterAdapter and GroupRosterAdapter
	 * 
	 * @param view
	 * @param context
	 * @param cursor
	 */
	public static void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			view.setTag(holder);
			holder.itemJid = (TextView) view.findViewById(R.id.roster_item_jid);
			holder.itemDescription = (TextView) view.findViewById(R.id.roster_item_description);
			holder.openChatNotifier = view.findViewById(R.id.openChatNotifier);

			holder.itemAvatar = (ImageView) view.findViewById(R.id.imageView1);
			holder.itemPresence = (ImageView) view.findViewById(R.id.roster_item_precence);
			holder.clientTypeIndicator = (ImageView) view.findViewById(R.id.client_type_indicator);
		}

		holder.itemJid.setTransformationMethod(SingleLineTransformationMethod.getInstance());
		String name = cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_DISPLAY_NAME));
		holder.itemJid.setText(name);

		BareJID account = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_ACCOUNT)));

		final BareJID jid = BareJID.bareJIDInstance(cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_JID)));
		//final Jaxmpp jaxmpp = ((MessengerApplication) context.getApplicationContext()).getMultiJaxmpp().get(account);

		if (holder.clientTypeIndicator != null) {
			holder.clientTypeIndicator.setVisibility(View.INVISIBLE);

//			CapabilitiesModule capabilitiesModule = jaxmpp.getModule(CapabilitiesModule.class);
//			try {
//				final String nodeName = jaxmpp.getSessionObject().getUserProperty(CapabilitiesModule.NODE_NAME_KEY);
//				for (Presence p : jaxmpp.getPresence().getPresences(jid).values()) {
//					if (p.getType() != null)
//						continue;
//
//					Integer client = null;// ClientIconsTool.getResourceImage(p, capabilitiesModule, nodeName);
//					if (client != null) {
//						holder.clientTypeIndicator.setImageResource(client);
//						holder.clientTypeIndicator.setVisibility(View.VISIBLE);
//					} else {
//						holder.clientTypeIndicator.setVisibility(View.INVISIBLE);
//					}
//				}
//			} catch (Exception e) {
//				// TODO: handle exception
//			}
		}

//		boolean co = jaxmpp.getModule(MessageModule.class).getChatManager().isChatOpenFor(jid);
//		holder.openChatNotifier.setVisibility(co ? View.VISIBLE : View.INVISIBLE);

		Integer p = cursor.getInt(cursor.getColumnIndex(RosterTableMetaData.FIELD_PRESENCE));

		if (p == CPresence.OFFLINE) {			
			holder.itemPresence.setImageResource(R.drawable.user_offline);
			if (cursor.getInt(cursor.getColumnIndex(RosterTableMetaData.FIELD_ASK)) != 0) {
				holder.itemPresence.setImageResource(R.drawable.user_ask);
			}
			else {
				String subscr = cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_SUBSCRIPTION));
				if (subscr == null || "none".equals(subscr)) {
					holder.itemPresence.setImageResource(R.drawable.user_noauth);
				}
			}
		}
		else {
			int presenceDrawable = cPresenceToImageResource(p);
			holder.itemPresence.setImageResource(presenceDrawable);
		}

		if (holder.itemDescription != null) {
			String status = cursor.getString(cursor.getColumnIndex(RosterTableMetaData.FIELD_STATUS_MESSAGE));
			if (status != null) {
				holder.itemDescription.setText(Html.fromHtml(status));
			} else {
				status = "";
				Cursor gc = null;
				try {
					gc = context.getContentResolver().query(Uri.parse(GeolocationProvider.CONTENT_URI), new String[] {
						GeolocationTableMetaData.FIELD_COUNTRY, GeolocationTableMetaData.FIELD_LOCALITY,
						GeolocationTableMetaData.FIELD_STREET
					}, GeolocationTableMetaData.FIELD_JID + "=?", new String[] { jid.toString() }, null);
					if (gc.moveToNext()) {
						for (int i=0; i<3; i++) {
							if (!gc.isNull(i)) {
								if (status.length() > 0) {
									status += ", ";
								}
								status += gc.getString(i);
							}
						}
					}
				} catch (Exception ex) {
					Log.e(TAG, "Exception retrieving contact geolocation", ex);
				} finally {
					if (gc != null) {
						gc.close();
					}
				}
				
//				// TODO: is it fast enough?
//				GeolocationModule geoModule = jaxmpp.getModule(GeolocationModule.class);
//				if (geoModule != null) {
//					ContentValues geoValue = geoModule.getLocationForJid(jid);
//					if (geoValue != null) {
//						String locality = geoValue.getAsString(GeolocationTableMetaData.FIELD_LOCALITY);
//						if (locality != null) {
//							status = locality;
//						}
//						String country = geoValue.getAsString(GeolocationTableMetaData.FIELD_COUNTRY);
//						if (country != null) {
//							if (status.length() > 0) {
//								status += ", ";
//							}
//							status += country;
//						}
//					}
//				}
				holder.itemDescription.setText(status);
			}
		}
		AvatarHelper.setAvatarToImageView(jid, holder.itemAvatar);
	}

	public static int cPresenceToImageResource(int p) {
		int result;
		switch (p) {
		case CPresence.CHAT:
			result = R.drawable.user_free_for_chat;
			break;
		case CPresence.ONLINE:
			result = R.drawable.user_available;
			break;
		case CPresence.AWAY:
			result = R.drawable.user_away;
			break;
		case CPresence.XA:
			result =R.drawable.user_extended_away;
			break;
		case CPresence.DND:
			result = R.drawable.user_busy;
			break;
//		case requested:
//			holder.itemPresence.setImageResource(R.drawable.user_ask);
//			break;
		case CPresence.ERROR:
			result = R.drawable.user_error;
			break;
//		case offline_nonauth:
//			holder.itemPresence.setImageResource(R.drawable.user_noauth);
//			break;
		default:
			result = R.drawable.user_offline;
			break;
		}
		return result;
	}
}
