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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tigase.messenger.phone.pro.Constants;
import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.providers.RosterProvider;
import org.tigase.messenger.phone.pro.db.providers.RosterProviderExt;
import org.tigase.messenger.phone.pro.service.AsyncXmppCallback;
import org.tigase.messenger.phone.pro.ui.ShareDialog;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;
import org.tigase.messenger.phone.pro.MainActivity;

import tigase.jaxmpp.android.roster.RosterItemsCacheTableMetaData;
import tigase.jaxmpp.core.client.BareJID;
//import tigase.jaxmpp.core.client.Connector;
//import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.ElementWrapper;
//import tigase.jaxmpp.core.client.exceptions.JaxmppException;
//import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterCacheProvider;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCard;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
//import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
//import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
//import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
//import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
//import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.jaxmpp.j2se.connectors.socket.StreamListener;
import tigase.jaxmpp.j2se.connectors.socket.XMPPDomBuilderHandler;
import tigase.jaxmpp.j2se.xml.J2seElement;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ContactFragment extends Fragment {

	private static final String TAG = "ContactFragment";
	
	public static final String FRAG_TAG = "ContactFragment";
	
//	private class ResourcesAdapter extends BaseAdapter {
//
//		private final LayoutInflater inflater;
//		private final Jaxmpp jaxmpp;
//		private final BareJID jid;
//		
//		private List<String> resources = new ArrayList<String>();
//		
//		public ResourcesAdapter(Context context, BareJID jid, Jaxmpp jaxmpp) {
//			this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//			this.jaxmpp = jaxmpp;
//			this.jid = jid;
//			
//			Map<String,Presence> presences = jaxmpp.getPresence().getPresences(jid);
//			Log.v("ResourcesAdapter", "for account " + jaxmpp.getSessionObject().getUserBareJid() + " for jid " + jid + " found presences " + (presences==null?"null":presences.size()));
//			if (presences != null) {
//				resources.addAll(presences.keySet());
//			}
//			notifyDataSetChanged();
//		}
//		
//		public void add(String resource) {
//			resources.add(resource);
//			Collections.sort(resources);
//			notifyDataSetChanged();
//		}
//		
//		public void remove(String resource) {
//			resources.remove(resource);
//			notifyDataSetChanged();
//		}
//		
//		@Override
//		public int getCount() {
////			Map<String,Presence> presences = jaxmpp.getPresence().getPresences(jid);
////			if (presences == null)
////				return 0;
////			return presences.size();
//			return resources.size();
//		}
//
//		@Override
//		public Object getItem(int arg0) {
//			// TODO Auto-generated method stub
//			return resources.get(arg0);
//		}
//
//		@Override
//		public long getItemId(int arg0) {
//			// TODO Auto-generated method stub
//			return resources.get(arg0).hashCode();
//		}
//
//		@Override
//		public View getView(int position, View convertView, ViewGroup parent) {
//			View view;
//			if (convertView == null) {
//				view = inflater.inflate(R.layout.contact_resources_item, parent, false);
//			} else {
//				view = convertView;
//			}
//			
//			TextView resourceName = (TextView) view.findViewById(R.id.resource_name);
//			TextView resourceDescription = (TextView) view.findViewById(R.id.resource_description);
//			
//			Map<String,Presence> presences = jaxmpp.getPresence().getPresences(jid);
//			String resource = (String) getItem(position);
//			
//			view.setTag(resource);
//			
//			Presence p = presences.get(resource);
//			if (p != null) {
//				resourceName.setText(resource);
//				try {
//					resourceDescription.setText(p.getStatus());
//				} catch (XMLException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			
//			ImageButton chatBtn = (ImageButton) view.findViewById(R.id.chat_btn);
//			chatBtn.setOnClickListener(chatClickListener);
//			
//			ImageButton shareBtn = (ImageButton) view.findViewById(R.id.share_btn);
//			shareBtn.setOnClickListener(shareClickListener);
//			
//			return view;
//		}
//		
//	}
	private class Item {
		String value;
		int type;
		
		public Item(String value, int type) {
			this.value = value;
			this.type = type;
		}
	}
	
	private View layout;
	
	private ImageView avatarView;
	private TextView nameTextView;
	
	private TextView titleView;
	private TextView organizationView;
	
	private String account;
	private BareJID jid;
	private String name;

//	private ResourcesAdapter resourcesAdapter;
//	private ListView resourcesView;

//	private Listener<PresenceModule.PresenceEvent> presenceListener;
	
	private OnClickListener chatClickListener;
	private OnClickListener shareClickListener;

	private Button chatBtn;
	private Button shareBtn;

	private ProgressBar progressBar;

	private View phonesLayout;

	private View emailsLayout;

	private View addressesLayout;

	private OnClickListener dialOnClickListener;

	private OnClickListener emailOnClickListener;

	private OnClickListener addressOnClickListener;

	private OnClickListener smsOnClickListener;

	private ListView phoneListLayout;

	private ArrayAdapter<Item> phoneListAdapter;

	private ListView addressListLayout;

	private ArrayAdapter<Item> addressListAdapter;

	private ListView emailListLayout;

	private ArrayAdapter<Item> emailListAdapter;

	protected ShareDialog shareDialog = null;
	
//	private Listener<Connector.ConnectorEvent> accountStatusListener;
	
//	private void onAccountStateChanged() {
//		Jaxmpp jaxmpp = getJaxmpp();
//		boolean connected = jaxmpp.isConnected();
//	
//		boolean shareAvailable = false;
////		JID jid = FileTransferUtility.getBestJidForFeatures(jaxmpp, this.jid, FileTransferUtility.FEATURES);
////		if (jid != null) {
////			try {
////				shareAvailable = FileTransferUtility.resourceContainsFeatures(jaxmpp, jid, FileTransferUtility.FEATURES);
////			} catch (XMLException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			}
////		}
//		shareBtn.setEnabled(connected && shareAvailable);		
//		chatBtn.setEnabled(connected);
//		
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//			updateActionBar();
//		}
//	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getArguments() != null) {
			setContact(getArguments().getString("account"), 
					BareJID.bareJIDInstance(getArguments().getString("jid")));
		}
		
//		accountStatusListener = new Listener<Connector.ConnectorEvent>() {
//
//			@Override
//			public void handleEvent(ConnectorEvent be) throws JaxmppException {
//				onAccountStateChanged();
//			}
//			
//		};
//		getJaxmpp().addListener(Connector.StateChanged, accountStatusListener);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == MainActivity.SELECT_FOR_SHARE && resultCode == Activity.RESULT_OK) {
			if (shareDialog != null) {
				shareDialog.onActivityResult(data);
				shareDialog = null;
			}
//			Uri selected = data.getData();
//			String mimetype = data.getType();
//			
//			JID jid = JID.jidInstance(ContactFragment.this.jid);//, shareWithResource);
//			final Jaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(account);
//			if (jid.getResource() == null) {
//				jid = FileTransferUtility.getBestJidForFeatures(jaxmpp, jid.getBareJid(), FileTransferUtility.FEATURES);
//			}
//			if (jid != null) {
//				FileTransferUtility.startFileTransfer(getActivity(), jaxmpp, jid, selected, mimetype);
//			}			
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.contact_menu, menu);
		MenuItem refreshItem = menu.findItem(R.id.refreshVCard);
		MenuItemCompat.setShowAsAction(refreshItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	public View onCreateView(final LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		this.setHasOptionsMenu(true);
		
		this.layout = inflater.inflate(R.layout.contact_fragment, container, false);
		
		this.avatarView = (ImageView) this.layout.findViewById(R.id.avatar);
		this.nameTextView = (TextView) this.layout.findViewById(R.id.name);
		this.progressBar = (ProgressBar) this.layout.findViewById(R.id.progressBar);
		
		this.titleView = (TextView) this.layout.findViewById(R.id.title);
		this.organizationView = (TextView) this.layout.findViewById(R.id.org);
//		this.resourcesView = (ListView) this.layout.findViewById(R.id.resources_view);
		
		this.chatClickListener = new OnClickListener() {

			@Override
			public void onClick(View view) {
				String resource = (String) view.getTag();
				
				final JID fullJid = JID.jidInstance(jid, resource);
				
				((MainActivity) getActivity()).openChat(account, fullJid);
			}
			
		};
		
		this.chatBtn = (Button) layout.findViewById(R.id.chat_btn);
		chatBtn.setOnClickListener(chatClickListener);
		
		this.phonesLayout = (View) layout.findViewById(R.id.phone_layout);
		this.phoneListLayout = (ListView) phonesLayout.findViewById(R.id.listLayout);
		this.phoneListAdapter = new ArrayAdapter<Item>(this.getActivity(), R.layout.contact_fragment_phone_item) {
			public View getView (int position, View convertView, ViewGroup parent) {
				View view;
				if (convertView == null) {
					view = inflater.inflate(R.layout.contact_fragment_phone_item, parent, false);
				} else {
					view = convertView;
				}		
				
				TextView value = (TextView) view.findViewById(R.id.value);
				View btn = view.findViewById(R.id.valueLayout);
				TextView type = (TextView) view.findViewById(R.id.type);
				View smsBtn = view.findViewById(R.id.sms_button);
				
				Item item = getItem(position);
				value.setText(item.value);
				type.setText(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(item.type));
				
				btn.setOnClickListener(dialOnClickListener);
				smsBtn.setOnClickListener(smsOnClickListener);
				
				return view;
			}
		};
		this.phoneListLayout.setAdapter(phoneListAdapter);
		
		this.emailsLayout = (View) layout.findViewById(R.id.email_layout);		
		this.emailListLayout = (ListView) emailsLayout.findViewById(R.id.listLayout1);
		this.emailListAdapter = new ArrayAdapter<Item>(getActivity(), R.layout.contact_fragment_item) {
			public View getView (int position, View convertView, ViewGroup parent) {
				View view;
				if (convertView == null) {
					view = inflater.inflate(R.layout.contact_fragment_item, parent, false);
				} else {
					view = convertView;
				}		
				
				TextView value = (TextView) view.findViewById(R.id.value);
				View btn = view.findViewById(R.id.valueLayout);
				TextView type = (TextView) view.findViewById(R.id.type);
				
				Item item = getItem(position);
				value.setText(item.value);
				type.setText(ContactsContract.CommonDataKinds.Email.getTypeLabelResource(item.type));
				
				btn.setOnClickListener(emailOnClickListener);
				
				return view;
			}			
		};
		this.emailListLayout.setAdapter(emailListAdapter);

		this.addressesLayout = (View) layout.findViewById(R.id.locality_layout);	
		this.addressListLayout = (ListView) addressesLayout.findViewById(R.id.listLayout2);
		this.addressListAdapter = new ArrayAdapter<Item>(getActivity(), R.layout.contact_fragment_item) {
			public View getView (int position, View convertView, ViewGroup parent) {
				View view;
				if (convertView == null) {
					view = inflater.inflate(R.layout.contact_fragment_item, parent, false);
				} else {
					view = convertView;
				}		
				
				TextView value = (TextView) view.findViewById(R.id.value);
				View btn = view.findViewById(R.id.valueLayout);
				TextView type = (TextView) view.findViewById(R.id.type);
				
				Item item = getItem(position);
				value.setText(item.value);
				type.setText(ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabelResource(item.type));
				
				btn.setOnClickListener(addressOnClickListener);
				
				return view;
			}			
		};
		this.addressListLayout.setAdapter(addressListAdapter);
		
		this.dialOnClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				TextView tv = (TextView) v.findViewById(R.id.value);
				Intent intent = new Intent(Intent.ACTION_DIAL);
				intent.setData(Uri.parse("tel:" + tv.getText()));
				startActivity(Intent.createChooser(intent, "")); 
			}		
		};
		
		this.smsOnClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				TextView tv = (TextView) ((View)v.getParent()).findViewById(R.id.value);
				Intent intent = new Intent(Intent.ACTION_SENDTO);
				intent.setData(Uri.parse("smsto:" + tv.getText().toString()));
				intent.setType("text/plain");	
				startActivity(Intent.createChooser(intent, ""));
			}
		};
		
		this.emailOnClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				TextView tv = (TextView) v.findViewById(R.id.value);
				Intent intent = new Intent(Intent.ACTION_SENDTO);
				intent.setData(Uri.parse("mailto:"));
				intent.setType("*/*");	
				intent.putExtra(Intent.EXTRA_EMAIL, new String[] { tv.getText().toString() });
				startActivity(Intent.createChooser(intent, ""));
			}			
		};
		
		this.addressOnClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				TextView tv = (TextView) v.findViewById(R.id.value);
				Intent intent = new Intent(Intent.ACTION_VIEW);
				Uri uri = Uri.parse("geo:0,0?q="+Uri.encode(tv.getText().toString()));
				intent.setData(uri);
				startActivity(Intent.createChooser(intent, ""));
			}
		};
		
		this.shareClickListener = new OnClickListener() {

			@Override
			public void onClick(View view) {
				shareDialog  = ShareDialog.newInstance((MainActivity) getActivity(), ContactFragment.this, MainActivity.SELECT_FOR_SHARE, account, JID.jidInstance(jid), null);
				shareDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {		
					@Override
					public void onDismiss(DialogInterface dialog) {
						Log.v(TAG, "dialog dismissed");
						if (shareDialog != null && shareDialog.isFinished()) {
							Log.v(TAG, "releasing shareDialog instance");
							shareDialog = null;
						}
					}
				});
				shareDialog.show();
			}
			
		};
		
		this.shareBtn = (Button) layout.findViewById(R.id.share_btn);
		shareBtn.setOnClickListener(shareClickListener);
		
		return layout;
	}
	
//	private Chat findChat(JID fullJid) {
//		List<ChatWrapper> wrappers = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().getChats();
//		Chat chat=null;
//		for (ChatWrapper w : wrappers) {
//			if (!w.isChat())
//				return null;
//			chat = w.getChat();
//			if (fullJid.getResource() == null) {
//				if (fullJid.getBareJid().equals(chat.getJid().getBareJid()))
//					return chat;
//			}
//			else {
//				if (fullJid.equals(chat.getJid())) {
//					return chat;
//				}
//			}
//		}
//		return null;
//	}
	
	public void setContact(String account, BareJID _jid) {
		this.account = account;
		this.jid = _jid;
		
//		this.presenceListener = new Listener<PresenceModule.PresenceEvent>() {
//
//			@Override
//			public void handleEvent(PresenceEvent be) throws JaxmppException {
//				if (!jid.equals(be.getJid().getBareJid()))
//					return;
//				
//				if (be.getType() == PresenceModule.ContactAvailable) {
//					resourcesAdapter.add(be.getJid().getResource());
//				}
//				else if (be.getType() == PresenceModule.ContactUnavailable) {
//					resourcesAdapter.remove(be.getJid().getResource());
//				}
//			}
//			
//		};		
		
//		final Jaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(account);
//		this.resourcesAdapter = new ResourcesAdapter(getActivity().getApplicationContext(), jid, jaxmpp);		
//		
//		jaxmpp.addListener(PresenceModule.ContactAvailable, this.presenceListener);
//		jaxmpp.addListener(PresenceModule.ContactUnavailable, this.presenceListener);
//		resourcesView.setAdapter(resourcesAdapter);
		
		refreshView(null);
	}
	
	public void refreshView(VCard vcard) {
		phoneListAdapter.clear();
		emailListAdapter.clear();
		addressListAdapter.clear();
		
		long id = RosterProviderExt.createId(BareJID.bareJIDInstance(account), jid);
		Cursor c3 = this.getActivity().getContentResolver().query(RawContacts.CONTENT_URI, new String[] { RawContacts.CONTACT_ID }, RawContacts.ACCOUNT_TYPE
			+ "='" + Constants.ACCOUNT_TYPE + "' AND " + RawContacts.SOURCE_ID + " = ?" , new String[] { String.valueOf(id) }, null);
		try {
			if (c3.moveToNext()) {
				id = c3.getInt(0);
				Log.e(TAG, "got contact id " + id + " for " + jid);
			}
			else {
				id = 0;
			}
		} catch (Exception ex) {
			id = 0;
			Log.e(TAG, "Exception retrieving Android Contact contact id for " + jid);
		}
		finally {
			c3.close();
		}
		Set<String> phones = new HashSet<String>();
		Set<String> emails = new HashSet<String>();
		Set<String> addresses = new HashSet<String>();
		
		if (id != 0) {
			Cursor c2 = this.getActivity().getContentResolver().query(Data.CONTENT_URI, 
				new String[] { "data1", "data2", Data.MIMETYPE }, 
				Data.CONTACT_ID + " = ? AND " 
				+ "("+ Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "')"
				+ " OR ("+ Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "')"
				+ " OR ("+ Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE + "')", new String[] { String.valueOf(id) }, null);
			try {
				while (c2.moveToNext()) {
					Item item = new Item(c2.getString(0), c2.getInt(1));
					String type = c2.getString(2);
					if (ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(type)) {
						phones.add(item.value);
						this.phoneListAdapter.add(item);
					}
					else if (ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE.equals(type)) {
						emails.add(item.value);
						this.emailListAdapter.add(item);
					}
					else if (ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE.equals(type)) {
						addresses.add(item.value);
						this.addressListAdapter.add(item);
					}
				}
			
			} catch (Exception ex) {
				Log.e(TAG, "Exception while retrieving contact informations from Android Contacts for " + jid, ex);
			} finally {
				c2.close();
			}
		}
		
		if (vcard != null) {
			// Merge info about phones
			if (vcard.getHomeTelVoice() != null && vcard.getHomeTelVoice().length() > 0) {
				if (phones.add(vcard.getHomeTelVoice())) {
					Item item = new Item(vcard.getHomeTelVoice(), ContactsContract.CommonDataKinds.Phone.TYPE_HOME);
					this.phoneListAdapter.add(item);
				}
			}
			if (vcard.getWorkTelVoice() != null && vcard.getWorkTelVoice().length() > 0) {
				if (emails.add(vcard.getWorkTelVoice())) {
					Item item = new Item(vcard.getWorkTelVoice(), ContactsContract.CommonDataKinds.Phone.TYPE_WORK);
					this.phoneListAdapter.add(item);
				}
			}				
			// merge info about emails
			if (vcard.getHomeEmail() != null && vcard.getHomeEmail().length() > 0) {
				if (emails.add(vcard.getHomeEmail())) {
					Item item = new Item(vcard.getHomeEmail(), ContactsContract.CommonDataKinds.Email.TYPE_HOME);
					this.emailListAdapter.add(item);
				}
			}
			if (vcard.getWorkEmail() != null && vcard.getWorkEmail().length() > 0) {
				if (emails.add(vcard.getWorkEmail())) {
					Item item = new Item(vcard.getWorkEmail(), ContactsContract.CommonDataKinds.Email.TYPE_WORK);
					this.emailListAdapter.add(item);
				}
			}			
			// merge info about addresses
			String homeAddress = "";
			if (vcard != null) {
				if (vcard.getHomeAddressStreet() != null)
					homeAddress += vcard.getHomeAddressStreet();
				if (vcard.getHomeAddressPCode() != null && vcard.getHomeAddressPCode().length() > 0) {
					if (homeAddress.length() > 0)
						homeAddress += ", ";
					homeAddress += vcard.getHomeAddressPCode();
					if (vcard.getHomeAddressLocality() != null && vcard.getHomeAddressLocality().length() > 0) {
						homeAddress += " " + vcard.getHomeAddressLocality();
					}
				}
				else if (vcard.getHomeAddressLocality() != null && vcard.getHomeAddressLocality().length() > 0) {
					if (homeAddress.length() > 0)
						homeAddress += ", ";
					homeAddress += vcard.getHomeAddressLocality();
				}
				if (vcard.getHomeAddressCtry() != null && vcard.getHomeAddressCtry().length() > 0) {
					if (homeAddress.length() > 0)
						homeAddress += ", ";
					homeAddress += vcard.getHomeAddressCtry();
				}
			}		
			if (homeAddress.length() > 0) {
				if (emails.add(homeAddress)) {
					Item item = new Item(homeAddress, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME);
					this.addressListAdapter.add(item);
				}
			}
			String workAddress = "";
			if (vcard != null) {
				if (vcard.getWorkAddressStreet() != null)
					workAddress += vcard.getWorkAddressStreet();
				if (vcard.getWorkAddressPCode() != null && vcard.getWorkAddressPCode().length() > 0) {
					if (workAddress.length() > 0)
						workAddress += ", ";
					workAddress += vcard.getWorkAddressPCode();
					if (vcard.getWorkAddressLocality() != null && vcard.getWorkAddressLocality().length() > 0) {
						workAddress += " " + vcard.getWorkAddressLocality();
					}
				}
				else if (vcard.getWorkAddressLocality() != null && vcard.getWorkAddressLocality().length() > 0) {
					if (workAddress.length() > 0)
						workAddress += ", ";
					workAddress += vcard.getWorkAddressLocality();
				}
				if (vcard.getWorkAddressCtry() != null && vcard.getWorkAddressCtry().length() > 0) {
					if (workAddress.length() > 0)
						workAddress += ", ";
					workAddress += vcard.getWorkAddressCtry();
				}
			}
			if (workAddress.length() > 0) {
				if (emails.add(workAddress)) {
					Item item = new Item(workAddress, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK);
					this.addressListAdapter.add(item);
				}
			}
		}

		Uri uri = Uri.parse(RosterProvider.CONTENT_URI + "/" + Uri.encode(jid.toString()));
		Cursor c = getActivity().getContentResolver().query(uri, null, null, null, null);
		if (c.moveToNext()) {
			this.name = c.getString(c.getColumnIndex(RosterItemsCacheTableMetaData.FIELD_NAME));
		}		
		else {
			name = "";
		}
		nameTextView.setText(name);
		AvatarHelper.setAvatarToImageView(jid, avatarView);
		
		titleView.setVisibility(vcard == null || vcard.getTitle() == null ? View.GONE : View.VISIBLE);
		organizationView.setVisibility(vcard == null || vcard.getOrgName() == null ? View.GONE : View.VISIBLE);
		
//		// Phones section
		phonesLayout.setVisibility(phoneListAdapter.getCount() != 0 ? View.VISIBLE : View.GONE);
		
		// Emails section
		emailsLayout.setVisibility(emailListAdapter.getCount() != 0 ? View.VISIBLE : View.GONE);		
				
		// Locality section
		addressesLayout.setVisibility(addressListAdapter.getCount() != 0 ? View.VISIBLE : View.GONE);		
//		
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//			showActionBar();
//		}
//		onAccountStateChanged();
	}
	
	@Override
	public void onDestroy() {
//		final Jaxmpp jaxmpp = getJaxmpp();
//		jaxmpp.removeListener(PresenceModule.ContactAvailable, this.presenceListener);
//		jaxmpp.removeListener(PresenceModule.ContactUnavailable, this.presenceListener);
//		jaxmpp.removeListener(Connector.StateChanged, accountStatusListener);
		super.onDestroy();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
//		if (item.getItemId() == R.id.contactDetails) {
//			Intent intent = new Intent(getActivity().getApplicationContext(), VCardViewActivity.class);
//			intent.putExtra("account", account.toString());
//			intent.putExtra("jid", jid.toString());
//			this.startActivityForResult(intent, 0);
//		} else 
		if (item.getItemId() == R.id.refreshVCard) {	
			progressBar.setVisibility(View.VISIBLE);
			final IJaxmppService jaxmppService = ((MainActivity) getActivity()).getJaxmppService();
			new Thread() {
				public void run() {
					
			try {
				jaxmppService.retrieveVCard(account, jid.toString(), new AsyncXmppCallback(new VCardModule.VCardAsyncCallback() {
					
					@Override
					public void onTimeout() throws JaxmppException {
						layout.post(new Runnable() {
							public void run() {
								try {
									progressBar.setVisibility(View.INVISIBLE);
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}
						});
					}
					
					@Override
					public void onError(Stanza responseStanza, ErrorCondition error)
							throws JaxmppException {
						layout.post(new Runnable() {
							public void run() {
								try {
									progressBar.setVisibility(View.INVISIBLE);
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}
						});
					}
					
					@Override
					protected void onVCardReceived(final VCard vcard) throws XMLException {
						layout.post(new Runnable() {
							public void run() {
								progressBar.setVisibility(View.INVISIBLE);
								refreshView(vcard);
							}
						});
					}
				}));
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				progressBar.setVisibility(View.INVISIBLE);
			}
				}
			}.start();
		}
		if (item.getItemId() == R.id.contactEdit) {
			Fragment frag = new ContactEditFragment();
			Bundle args = new Bundle();
			args.putString("account", account);
			args.putString("jid", jid.toString());
			frag.setArguments(args);
			((MainActivity) getActivity()).switchFragments(frag, ContactEditFragment.FRAG_TAG);
//			Intent intent = new Intent(getActivity().getApplicationContext(), ContactEditActivity.class);
//			intent.putExtra("account", account.toString());
//			intent.putExtra("jid", jid.toString());
//			this.startActivityForResult(intent, 0);
//		} else if (item.getItemId() == R.id.contactRemove) {
//			DialogFragment newFragment = ContactRemoveDialog.newInstance(account, JID.jidInstance(jid));
//			newFragment.show(getFragmentManager(), "dialog");
////		} else if (item.getItemId() == R.id.contactAuthorization) {
////			this.lastMenuInfo = item.getMenuInfo();
////			return true;
//		} else if (item.getItemId() == R.id.contactAuthResend) {
//			sendAuthResend();
//		} else if (item.getItemId() == R.id.contactAuthRerequest) {
//			sendAuthRerequest();
//		} else if (item.getItemId() == R.id.contactAuthRemove) {
//			sendAuthRemove();
		}
		else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
				&& Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			MenuInflater inflater = new MenuInflater(this.getActivity().getApplicationContext());
			onCreateOptionsMenu(menu, inflater);
		}
		
//		Jaxmpp jaxmpp = getJaxmpp();
		boolean connected = true;
//		try {
//			connected = ((MainActivity) getActivity()).getJaxmppService().isConnected(account);// jaxmpp != null && jaxmpp.isConnected();
//		}
//		catch (Exception ex) {
//			ex.printStackTrace();
//		}
//		
//		menu.findItem(R.id.contactDetails).setEnabled(connected);
		menu.findItem(R.id.contactEdit).setEnabled(connected);
		menu.findItem(R.id.contactRemove).setEnabled(connected);
		menu.findItem(R.id.contactAuthorization).setEnabled(connected);

		super.onPrepareOptionsMenu(menu);
	}	

//	private void sendAuthRemove() {
//		final Jaxmpp jaxmpp = getJaxmpp();
//		Runnable r = new Runnable() {
//
//			@Override
//			public void run() {
//				try {
//					jaxmpp.getModule(PresenceModule.class).unsubscribed(JID.jidInstance(jid));
//				} catch (JaxmppException e) {
//					Log.w(TAG, "Can't remove auth", e);
//				}
//			}
//		};
//		(new Thread(r)).start();
//		final String name = RosterDisplayTools.getDisplayName(jaxmpp.getSessionObject(), jid);
//		String txt = String.format(getActivity().getString(R.string.auth_removed), name, jid.toString());
//		Toast.makeText(getActivity().getApplicationContext(), txt, Toast.LENGTH_LONG).show();
//	}
//
//	private void sendAuthRerequest() {
//		final Jaxmpp jaxmpp = getJaxmpp();
//
//		Runnable r = new Runnable() {
//
//			@Override
//			public void run() {
//				try {
//					jaxmpp.getModule(PresenceModule.class).subscribe(JID.jidInstance(jid));
//				} catch (JaxmppException e) {
//					Log.w(TAG, "Can't rerequest subscription", e);
//				}
//			}
//		};
//		(new Thread(r)).start();
//		final String name = RosterDisplayTools.getDisplayName(jaxmpp.getSessionObject(), jid);
//		String txt = String.format(getActivity().getString(R.string.auth_rerequested), name, jid.toString());
//		Toast.makeText(getActivity().getApplicationContext(), txt, Toast.LENGTH_LONG).show();
//
//	}
//
//	private void sendAuthResend() {
//		final Jaxmpp jaxmpp = getJaxmpp();
//
//		Runnable r = new Runnable() {
//
//			@Override
//			public void run() {
//				try {
//					jaxmpp.getModule(PresenceModule.class).subscribed(JID.jidInstance(jid));
//				} catch (JaxmppException e) {
//					Log.w(TAG, "Can't resend subscription", e);
//				}
//			}
//		};
//		(new Thread(r)).start();
//		final String name = RosterDisplayTools.getDisplayName(jaxmpp.getSessionObject(), jid);
//		String txt = String.format(getActivity().getString(R.string.auth_resent), name, jid.toString());
//		Toast.makeText(getActivity().getApplicationContext(), txt, Toast.LENGTH_LONG).show();
//
//	}
//	
//	private Jaxmpp getJaxmpp() {
//		return ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(account);
//	}
	
//	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
//	private void showActionBar() {
//		if (!(getActivity() instanceof ContactActivity))
//			return;
//		
//		ActionBar actionBar = getActivity().getActionBar();
//		actionBar.setDisplayHomeAsUpEnabled(true);
//		RosterItem rosterItem = getJaxmpp().getRoster().get(jid);
//		String name = rosterItem != null ? rosterItem.getName() : jid.toString();
//		if (name == null || name.length() == 0) {
//			actionBar.setTitle(jid.toString());
//		}
//		else {
//			actionBar.setTitle(name);
//			actionBar.setSubtitle(jid.toString());
//		}		
//	}
	
//	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
//	private void updateActionBar() {
//		getActivity().invalidateOptionsMenu();		
//	}
}
