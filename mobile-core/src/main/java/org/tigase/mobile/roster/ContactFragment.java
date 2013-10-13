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
package org.tigase.mobile.roster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp.ChatWrapper;
import org.tigase.mobile.R;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.TigaseMobileMessengerActivity;
import org.tigase.mobile.chat.ChatActivity;
import org.tigase.mobile.chat.ChatHistoryFragment;
import org.tigase.mobile.filetransfer.FileTransferUtility;
import org.tigase.mobile.utils.AvatarHelper;
import org.tigase.mobile.vcard.VCardViewActivity;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterItem;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
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
import android.widget.TextView;
import android.widget.Toast;

public class ContactFragment extends Fragment {

	private static final String TAG = "ContactFragment";
	
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
	
	private BareJID account;
	private BareJID jid;

//	private ResourcesAdapter resourcesAdapter;
//	private ListView resourcesView;

	private Listener<PresenceModule.PresenceEvent> presenceListener;
	
	private OnClickListener chatClickListener;
	private OnClickListener shareClickListener;

	private Button chatBtn;
	private Button shareBtn;
	
	private Listener<Connector.ConnectorEvent> accountStatusListener;
	
	private void onAccountStateChanged() {
		Jaxmpp jaxmpp = getJaxmpp();
		boolean connected = jaxmpp.isConnected();
	
		boolean shareAvailable = false;
		JID jid = FileTransferUtility.getBestJidForFeatures(jaxmpp, this.jid, FileTransferUtility.FEATURES);
		if (jid != null) {
			try {
				shareAvailable = FileTransferUtility.resourceContainsFeatures(jaxmpp, jid, FileTransferUtility.FEATURES);
			} catch (XMLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		shareBtn.setEnabled(connected && shareAvailable);		
		chatBtn.setEnabled(connected);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			updateActionBar();
		}
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getArguments() != null) {
			setContact(BareJID.bareJIDInstance(getArguments().getString("account")), 
					BareJID.bareJIDInstance(getArguments().getString("jid")));
		}
		
		accountStatusListener = new Listener<Connector.ConnectorEvent>() {

			@Override
			public void handleEvent(ConnectorEvent be) throws JaxmppException {
				onAccountStateChanged();
			}
			
		};
		getJaxmpp().addListener(Connector.StateChanged, accountStatusListener);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == TigaseMobileMessengerActivity.SELECT_FOR_SHARE && resultCode == Activity.RESULT_OK) {
			Uri selected = data.getData();
			String mimetype = data.getType();
			
			JID jid = JID.jidInstance(ContactFragment.this.jid);//, shareWithResource);
			final Jaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(account);
			if (jid.getResource() == null) {
				jid = FileTransferUtility.getBestJidForFeatures(jaxmpp, jid.getBareJid(), FileTransferUtility.FEATURES);
			}
			if (jid != null) {
				FileTransferUtility.startFileTransfer(getActivity(), jaxmpp, jid, selected, mimetype);
			}			
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.contact_menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		this.setHasOptionsMenu(true);
		
		this.layout = inflater.inflate(R.layout.contact_fragment, container, false);
		
		this.avatarView = (ImageView) this.layout.findViewById(R.id.avatar);
		this.nameTextView = (TextView) this.layout.findViewById(R.id.name);
//		this.resourcesView = (ListView) this.layout.findViewById(R.id.resources_view);
		
		this.chatClickListener = new OnClickListener() {

			@Override
			public void onClick(View view) {
				String resource = (String) view.getTag();
				
				final JID fullJid = JID.jidInstance(jid, resource);
				
				final Jaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(account);

				Chat chat = findChat(fullJid);
				if (chat == null) {
					try { jaxmpp.createChat(fullJid); } catch(Exception ex) { ex.printStackTrace(); }
					chat = findChat(fullJid);
				}

				Intent intent = new Intent(getActivity(), ChatActivity.class);
				intent.putExtra("chatId", chat.getId());
				intent.putExtra("account", account.toString());					
				getActivity().startActivity(intent);
			}
			
		};
		
		this.chatBtn = (Button) layout.findViewById(R.id.chat_btn);
		chatBtn.setOnClickListener(chatClickListener);

		this.shareClickListener = new OnClickListener() {

			@Override
			public void onClick(View view) {
//				shareWithResource = (String) view.getTag();
				Intent pickerIntent = new Intent(Intent.ACTION_PICK);
				pickerIntent.setType("video/*, images/*");
				pickerIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);				
				getActivity().startActivityForResult(pickerIntent, TigaseMobileMessengerActivity.SELECT_FOR_SHARE);
			}
			
		};
		
		this.shareBtn = (Button) layout.findViewById(R.id.share_btn);
		shareBtn.setOnClickListener(shareClickListener);
		
		return layout;
	}
	
	private Chat findChat(JID fullJid) {
		List<ChatWrapper> wrappers = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().getChats();
		Chat chat=null;
		for (ChatWrapper w : wrappers) {
			if (!w.isChat())
				return null;
			chat = w.getChat();
			if (fullJid.getResource() == null) {
				if (fullJid.getBareJid().equals(chat.getJid().getBareJid()))
					return chat;
			}
			else {
				if (fullJid.equals(chat.getJid())) {
					return chat;
				}
			}
		}
		return null;
	}
	
	public void setContact(BareJID account, BareJID _jid) {
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
		
		final Jaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(account);
//		this.resourcesAdapter = new ResourcesAdapter(getActivity().getApplicationContext(), jid, jaxmpp);		
//		
//		jaxmpp.addListener(PresenceModule.ContactAvailable, this.presenceListener);
//		jaxmpp.addListener(PresenceModule.ContactUnavailable, this.presenceListener);
//		resourcesView.setAdapter(resourcesAdapter);
		
		refresh();
	}
	
	public void refresh() {
		final Jaxmpp jaxmpp = getJaxmpp();
		RosterItem ri = jaxmpp.getRoster().get(jid);
		String name = RosterDisplayTools.getDisplayName(ri);
		if (name == null) name = jid.toString();
		nameTextView.setText(name);
		AvatarHelper.setAvatarToImageView(jid, avatarView);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			showActionBar();
		}
		onAccountStateChanged();
	}
	
	@Override
	public void onDestroy() {
		final Jaxmpp jaxmpp = getJaxmpp();
		jaxmpp.removeListener(PresenceModule.ContactAvailable, this.presenceListener);
		jaxmpp.removeListener(PresenceModule.ContactUnavailable, this.presenceListener);
		jaxmpp.removeListener(Connector.StateChanged, accountStatusListener);
		super.onDestroy();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.contactDetails) {
			Intent intent = new Intent(getActivity().getApplicationContext(), VCardViewActivity.class);
			intent.putExtra("account", account.toString());
			intent.putExtra("jid", jid.toString());
			this.startActivityForResult(intent, 0);
		} else if (item.getItemId() == R.id.contactEdit) {
			Intent intent = new Intent(getActivity().getApplicationContext(), ContactEditActivity.class);
			intent.putExtra("account", account.toString());
			intent.putExtra("jid", jid.toString());
			this.startActivityForResult(intent, 0);
		} else if (item.getItemId() == R.id.contactRemove) {
			DialogFragment newFragment = ContactRemoveDialog.newInstance(account, JID.jidInstance(jid));
			newFragment.show(getFragmentManager(), "dialog");
//		} else if (item.getItemId() == R.id.contactAuthorization) {
//			this.lastMenuInfo = item.getMenuInfo();
//			return true;
		} else if (item.getItemId() == R.id.contactAuthResend) {
			sendAuthResend();
		} else if (item.getItemId() == R.id.contactAuthRerequest) {
			sendAuthRerequest();
		} else if (item.getItemId() == R.id.contactAuthRemove) {
			sendAuthRemove();
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
		
		Jaxmpp jaxmpp = getJaxmpp();
		boolean connected = jaxmpp != null && jaxmpp.isConnected();
		
		menu.findItem(R.id.contactDetails).setEnabled(connected);
		menu.findItem(R.id.contactEdit).setEnabled(connected);
		menu.findItem(R.id.contactRemove).setEnabled(connected);
		menu.findItem(R.id.contactAuthorization).setEnabled(connected);

		super.onPrepareOptionsMenu(menu);
	}	

	private void sendAuthRemove() {
		final Jaxmpp jaxmpp = getJaxmpp();
		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					jaxmpp.getModule(PresenceModule.class).unsubscribed(JID.jidInstance(jid));
				} catch (JaxmppException e) {
					Log.w(TAG, "Can't remove auth", e);
				}
			}
		};
		(new Thread(r)).start();
		final String name = RosterDisplayTools.getDisplayName(jaxmpp.getSessionObject(), jid);
		String txt = String.format(getActivity().getString(R.string.auth_removed), name, jid.toString());
		Toast.makeText(getActivity().getApplicationContext(), txt, Toast.LENGTH_LONG).show();
	}

	private void sendAuthRerequest() {
		final Jaxmpp jaxmpp = getJaxmpp();

		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					jaxmpp.getModule(PresenceModule.class).subscribe(JID.jidInstance(jid));
				} catch (JaxmppException e) {
					Log.w(TAG, "Can't rerequest subscription", e);
				}
			}
		};
		(new Thread(r)).start();
		final String name = RosterDisplayTools.getDisplayName(jaxmpp.getSessionObject(), jid);
		String txt = String.format(getActivity().getString(R.string.auth_rerequested), name, jid.toString());
		Toast.makeText(getActivity().getApplicationContext(), txt, Toast.LENGTH_LONG).show();

	}

	private void sendAuthResend() {
		final Jaxmpp jaxmpp = getJaxmpp();

		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					jaxmpp.getModule(PresenceModule.class).subscribed(JID.jidInstance(jid));
				} catch (JaxmppException e) {
					Log.w(TAG, "Can't resend subscription", e);
				}
			}
		};
		(new Thread(r)).start();
		final String name = RosterDisplayTools.getDisplayName(jaxmpp.getSessionObject(), jid);
		String txt = String.format(getActivity().getString(R.string.auth_resent), name, jid.toString());
		Toast.makeText(getActivity().getApplicationContext(), txt, Toast.LENGTH_LONG).show();

	}
	
	private Jaxmpp getJaxmpp() {
		return ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp().get(account);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showActionBar() {
		if (!(getActivity() instanceof ContactActivity))
			return;
		
		ActionBar actionBar = getActivity().getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		RosterItem rosterItem = getJaxmpp().getRoster().get(jid);
		String name = rosterItem != null ? rosterItem.getName() : jid.toString();
		if (name == null || name.length() == 0) {
			actionBar.setTitle(jid.toString());
		}
		else {
			actionBar.setTitle(name);
			actionBar.setSubtitle(jid.toString());
		}		
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void updateActionBar() {
		getActivity().invalidateOptionsMenu();		
	}
}
