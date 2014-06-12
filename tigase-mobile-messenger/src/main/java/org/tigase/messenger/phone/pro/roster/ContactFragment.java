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
import java.util.List;
import java.util.Map;

import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.providers.RosterProvider;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;
import org.tigase.messenger.phone.pro.MainActivity;

import tigase.jaxmpp.android.roster.RosterItemsCacheTableMetaData;
import tigase.jaxmpp.core.client.BareJID;
//import tigase.jaxmpp.core.client.Connector;
//import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xml.ElementWrapper;
//import tigase.jaxmpp.core.client.exceptions.JaxmppException;
//import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterCacheProvider;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCard;
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
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
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

	private View phoneHomeLayout;

	private View phoneWorkLayout;

	private TextView phoneHomeNumber;

	private TextView phoneWorkNumber;

	private View emailHomeLayout;

	private TextView emailHomeNumber;

	private View emailWorkLayout;

	private TextView emailWorkNumber;

	private View addressesLayout;

	private View addressHomeLayout;

	private TextView addressHomeNumber;

	private View addressWorkLayout;

	private TextView addressWorkNumber;

	private OnClickListener dialOnClickListener;

	private OnClickListener emailOnClickListener;

	private OnClickListener addressOnClickListener;

	private OnClickListener smsOnClickListener;
	
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
	
//	@Override
//	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		if (requestCode == TigaseMobileMessengerActivity.SELECT_FOR_SHARE && resultCode == Activity.RESULT_OK) {
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
//		}
//	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.contact_menu, menu);
		MenuItem refreshItem = menu.findItem(R.id.refreshVCard);
		MenuItemCompat.setShowAsAction(refreshItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
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
		this.phoneHomeLayout = (View) this.phonesLayout.findViewById(R.id.phonenumber_home_layout);
		this.phoneHomeNumber = (TextView) this.phoneHomeLayout.findViewById(R.id.number);
		this.phoneWorkLayout = (View) this.phonesLayout.findViewById(R.id.phonenumber_work_layout);
		this.phoneWorkNumber = (TextView) this.phoneWorkLayout.findViewById(R.id.number);
		
		this.emailsLayout = (View) layout.findViewById(R.id.email_layout);
		this.emailHomeLayout = (View) this.emailsLayout.findViewById(R.id.email_home_layout);
		this.emailHomeNumber = (TextView) this.emailHomeLayout.findViewById(R.id.address);
		this.emailWorkLayout = (View) this.emailsLayout.findViewById(R.id.email_work_layout);
		this.emailWorkNumber = (TextView) this.emailWorkLayout.findViewById(R.id.address);		

		this.addressesLayout = (View) layout.findViewById(R.id.locality_layout);
		this.addressHomeLayout = (View) this.addressesLayout.findViewById(R.id.locality_home_layout);
		this.addressHomeNumber = (TextView) this.addressHomeLayout.findViewById(R.id.address);
		this.addressWorkLayout = (View) this.addressesLayout.findViewById(R.id.locality_work_layout);
		this.addressWorkNumber = (TextView) this.addressWorkLayout.findViewById(R.id.address);		
		
		this.dialOnClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				TextView tv = (TextView) v.findViewById(R.id.number);
				Intent intent = new Intent(Intent.ACTION_DIAL);
				intent.setData(Uri.parse("tel:" + tv.getText()));
				startActivity(Intent.createChooser(intent, "")); 
			}		
		};
		this.phoneHomeLayout.setOnClickListener(dialOnClickListener);
		this.phoneWorkLayout.setOnClickListener(dialOnClickListener);
		
		this.smsOnClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				TextView tv = (TextView) ((View)v.getParent()).findViewById(R.id.number);
				Intent intent = new Intent(Intent.ACTION_SENDTO);
				intent.setData(Uri.parse("smsto:" + tv.getText().toString()));
				intent.setType("text/plain");	
				startActivity(Intent.createChooser(intent, ""));
			}
		};
		this.phoneHomeLayout.findViewById(R.id.sms_button).setOnClickListener(smsOnClickListener);
		this.phoneWorkLayout.findViewById(R.id.sms_button).setOnClickListener(smsOnClickListener);
		
		this.emailOnClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				TextView tv = (TextView) v.findViewById(R.id.address);
				Intent intent = new Intent(Intent.ACTION_SENDTO);
				intent.setData(Uri.parse("mailto:"));
				intent.setType("*/*");	
				intent.putExtra(Intent.EXTRA_EMAIL, new String[] { tv.getText().toString() });
				startActivity(Intent.createChooser(intent, ""));
			}			
		};
		this.emailHomeLayout.setOnClickListener(emailOnClickListener);
		this.emailWorkLayout.setOnClickListener(emailOnClickListener);
		
		this.addressOnClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				TextView tv = (TextView) v.findViewById(R.id.address);
				Intent intent = new Intent(Intent.ACTION_VIEW);
				Uri uri = Uri.parse("geo:0,0?q="+Uri.encode(tv.getText().toString()));
				intent.setData(uri);
				startActivity(Intent.createChooser(intent, ""));
			}
		};
		this.addressHomeLayout.setOnClickListener(addressOnClickListener);
		this.addressWorkLayout.setOnClickListener(addressOnClickListener);
		
//		this.shareClickListener = new OnClickListener() {
//
//			@Override
//			public void onClick(View view) {
////				shareWithResource = (String) view.getTag();
//				Intent pickerIntent = new Intent(Intent.ACTION_PICK);
//				pickerIntent.setType("video/*, images/*");
//				pickerIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);				
//				getActivity().startActivityForResult(pickerIntent, TigaseMobileMessengerActivity.SELECT_FOR_SHARE);
//			}
//			
//		};
//		
//		this.shareBtn = (Button) layout.findViewById(R.id.share_btn);
//		shareBtn.setOnClickListener(shareClickListener);
		
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
//		final Jaxmpp jaxmpp = getJaxmpp();
//		RosterItem ri = jaxmpp.getRoster().get(jid);
//		String name = RosterDisplayTools.getDisplayName(ri);
//		if (name == null) name = jid.toString();
//		nameTextView.setText(name);
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
		
		// Phones section
		boolean hasNoHomePhone = vcard == null || vcard.getHomeTelVoice() == null || vcard.getHomeTelVoice().length() == 0;
		phoneHomeLayout.setVisibility(hasNoHomePhone ? View.GONE : View.VISIBLE);
		if (!hasNoHomePhone)
			phoneHomeNumber.setText(vcard.getHomeTelVoice());
		
		boolean hasNoWorkPhone = vcard == null || vcard.getWorkTelVoice() == null || vcard.getWorkTelVoice().length() == 0;
		phoneWorkLayout.setVisibility(hasNoWorkPhone ? View.GONE : View.VISIBLE);
		if (!hasNoWorkPhone)
			phoneWorkNumber.setText(vcard.getWorkTelVoice());
		
		phonesLayout.setVisibility(!hasNoHomePhone || !hasNoWorkPhone ? View.VISIBLE : View.GONE);
		
		// Emails section
		boolean hasNoHomeEmail = vcard == null || vcard.getHomeEmail() == null || vcard.getHomeEmail().length() == 0;
		emailHomeLayout.setVisibility(hasNoHomeEmail ? View.GONE : View.VISIBLE);
		if (!hasNoHomeEmail)
			emailHomeNumber.setText(vcard.getHomeEmail());
		
		boolean hasNoWorkEmail = vcard == null || vcard.getWorkEmail() == null || vcard.getWorkEmail().length() == 0;
		emailWorkLayout.setVisibility(hasNoWorkEmail ? View.GONE : View.VISIBLE);
		if (!hasNoWorkEmail)
			emailWorkNumber.setText(vcard.getWorkEmail());
		
		emailsLayout.setVisibility(!hasNoHomeEmail || !hasNoWorkEmail ? View.VISIBLE : View.GONE);		
		
		// Locality section
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
		boolean hasNoHomeAddress = homeAddress.length() == 0;
		addressHomeLayout.setVisibility(hasNoHomeAddress ? View.GONE : View.VISIBLE);
		if (!hasNoHomeAddress)
			addressHomeNumber.setText(homeAddress);
		
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
		boolean hasNoWorkAddress = workAddress.length() == 0;
		addressWorkLayout.setVisibility(hasNoWorkAddress ? View.GONE : View.VISIBLE);
		if (!hasNoWorkAddress)
			addressWorkNumber.setText(workAddress);
		
		addressesLayout.setVisibility(!hasNoHomeAddress || !hasNoWorkAddress ? View.VISIBLE : View.GONE);		
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
				jaxmppService.retrieveVCard(account, jid.toString(), new RosterUpdateCallback.Stub() {

					@Override
					public void onSuccess(final String msg) throws RemoteException {
						// TODO Auto-generated method stub
						layout.post(new Runnable() {
							public void run() {
								
						try {
							progressBar.setVisibility(View.INVISIBLE);
							
							XMPPDomBuilderHandler handler = new XMPPDomBuilderHandler(new StreamListener() {
								@Override
								public void nextElement(Element element) {
									try {
										VCard vcard = VCard.fromElement(new J2seElement(element));
										refreshView(vcard);
									} catch (XMLException ex) {
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
							char[] data = msg.toCharArray();
							parser.parse(handler, data, 0, data.length);
							
						} catch (Exception ex) {
							ex.printStackTrace();
						}
							}
						});
					}

					@Override
					public void onFailure(String msg) throws RemoteException {
						// TODO Auto-generated method stub
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
					
				});
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
