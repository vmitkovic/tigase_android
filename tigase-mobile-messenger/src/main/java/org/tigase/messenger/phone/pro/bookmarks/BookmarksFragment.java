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
package org.tigase.messenger.phone.pro.bookmarks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.MainActivityHelper;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.muc.JoinMucDialog;
import org.tigase.messenger.phone.pro.roster.RosterFragment;
import org.tigase.messenger.phone.pro.service.AsyncXmppCallback;
import org.tigase.messenger.phone.pro.MainActivity;

import tigase.jaxmpp.android.xml.ParcelableElement;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.PacketWriter;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.DefaultElement;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.BookmarksModule;
import tigase.jaxmpp.core.client.xmpp.modules.BookmarksModule.BookmarksAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.Toast;

public class BookmarksFragment extends Fragment {
	
	public static class Bookmark {

		protected BareJID accountJid;

		protected boolean autojoin;
		protected String id;
		protected BareJID jid;
		protected String name;
		protected String nick;
		protected String password;

		public Bookmark() {
			id = UUID.randomUUID().toString();
			autojoin = false;
		}
	}

	private class BookmarksAsyncCallbackImpl extends BookmarksAsyncCallback {

		private final BareJID accountJid;
		private final BookmarksAdapter adapter;

		public BookmarksAsyncCallbackImpl(BareJID accountJid, BookmarksAdapter adapter2) {
			this.adapter = adapter2;
			this.accountJid = accountJid;
		}

		@Override
		public void onBookmarksReceived(final List<Element> bookmarks) {
			if (bookmarks == null)
				return;

			listView.post(new Runnable() {
				@Override
				public void run() {
					for (Element elem : bookmarks) {
						try {
							if ("conference".equals(elem.getName())) {
								Bookmark bookmark = new Bookmark();
								bookmark.name = elem.getAttribute("name");
								bookmark.accountJid = accountJid;
								bookmark.jid = BareJID.bareJIDInstance(elem.getAttribute("jid"));
								if (elem.getAttribute("autojoin") != null) {
									bookmark.autojoin = Boolean.parseBoolean(elem.getAttribute("autojoin"));
								}
								if (bookmark.name == null) {
									bookmark.name = bookmark.jid.toString();
								}
								for (Element child : elem.getChildren()) {
									if ("nick".equals(child.getName())) {
										bookmark.nick = child.getValue();
									} else if ("password".equals(child.getName())) {
										bookmark.password = child.getValue();
									}
								}
								adapter.add(bookmark);
							}
						} catch (XMLException ex) {
							Log.e(TAG, "exception processing bookmarks", ex);
						}
					}

					for (int i = 0; i < adapter.getGroupCount(); i++) {
						listView.expandGroup(i);
					}
					// adapter.sort(new Comparator<Bookmark>() {
					//
					// @Override
					// public int compare(Bookmark lhs, Bookmark rhs) {
					// if (lhs == null || rhs == null)
					// return -1;
					// return lhs.name.compareTo(rhs.name);
					// }
					//
					// });
				}
			});
		}

		@Override
		public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
			// TODO Auto-generated method stub

		}

		@Override
		public void onTimeout() throws JaxmppException {
			// TODO Auto-generated method stub

		}
	}

	private static final int DIALOG_EDIT_BOOKMARK = 1;;
	
	public static final String FRAG_TAG = "BookmarksFragment";

	private static final String TAG = "BookmarksActivity";
	private BookmarksAdapter adapter;

	private ExpandableListView listView;

	public void editItem(Bookmark bookmark) {
		Bundle data = new Bundle();

		data.putBoolean("editMode", true);

		data.putString("id", bookmark.id);
		data.putString("account", bookmark.accountJid.toString());
		data.putString("room", bookmark.jid.getLocalpart());
		data.putString("server", bookmark.jid.getDomain());
		data.putString("name", bookmark.name);
		data.putString("nick", bookmark.nick);
		data.putString("password", bookmark.password);
		data.putBoolean("autojoin", bookmark.autojoin);

		JoinMucDialog dlg = JoinMucDialog.newInstance(data);
		dlg.show(getActivity().getSupportFragmentManager(), "dialog");
	}

	@TargetApi(11)
	private void initializeContextActions() {

		listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

			private Bookmark getBookmarkFromFlatPosition(int pos) {
				long packed = listView.getExpandableListPosition(pos);
				int child = ExpandableListView.getPackedPositionChild(packed);
				int group = ExpandableListView.getPackedPositionGroup(packed);

				return (Bookmark) adapter.getChild(group, child);
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				SparseBooleanArray selection = listView.getCheckedItemPositions();

				if (item.getItemId() == R.id.edit) {
					for (int i = 0; i < selection.size(); i++) {
						if (selection.valueAt(i)) {
							int pos = selection.keyAt(i);

							Bookmark bookmark = getBookmarkFromFlatPosition(pos);
							editItem(bookmark);
						}
					}
					mode.finish(); // Action picked, so close the CAB
					return true;
				} else if (item.getItemId() == R.id.remove) {
					List<Bookmark> items = new ArrayList<Bookmark>();
					for (int i = 0; i < selection.size(); i++) {
						if (selection.valueAt(i)) {
							int pos = selection.keyAt(i);

							Bookmark bookmark = getBookmarkFromFlatPosition(pos);
							if (bookmark != null) {
								items.add(bookmark);
							}
						}
					}
					removeItems(items);
					mode.finish();
					return true;
				} else {
					return false;
				}

			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.bookmarks_context_menu, menu);
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
				Menu menu = mode.getMenu();
				for (int i = 0; i < menu.size(); i++) {
					MenuItem mi = menu.getItem(i);
					if (mi.getItemId() == R.id.edit) {
						mi.setVisible(listView.getCheckedItemCount() < 2);
					}
				}
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				// TODO Auto-generated method stub
				return true;
			}

		});
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		if (item.getItemId() == R.id.edit) {
			Bookmark bookmark = adapter.getChildById(info.id);
			editItem(bookmark);
			return true;
		} else if (item.getItemId() == R.id.remove) {
			Bookmark bookmark = adapter.getChildById(info.id);
			List<Bookmark> bookmarks = new ArrayList<Bookmark>();
			bookmarks.add(bookmark);
			removeItems(bookmarks);
			return true;
		} else {
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View layout = inflater.inflate(R.layout.bookmarks_list, null);
		listView = (ExpandableListView) layout.findViewById(R.id.bookmarksList);

		adapter = new BookmarksAdapter(this.getActivity());

		listView.setAdapter(adapter);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			initializeContextActions();
		}

		listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				final Bookmark bookmark = (Bookmark) adapter.getChild(groupPosition, childPosition);

				new Thread() {
					@Override
					public void run() {
						IJaxmppService jaxmppService = ((MainActivity) BookmarksFragment.this.getActivity()).getJaxmppService();
						try {
							jaxmppService.joinRoom(bookmark.accountJid.toString(), bookmark.jid.toString(),
									bookmark.nick, bookmark.password, "org.tigase.messenger.phone.pro.MUC_ROOM_JOINED");

							getActivity().getSupportFragmentManager().popBackStack(RosterFragment.FRAG_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
						} catch (RemoteException e) {
							Log.e(TAG, "exception while trying to join room");
						}
					}
				}.start();

				return true;
			}

		});

		refresh();		
		return layout;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.bookmarks_context_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.add) {
			Bundle data = new Bundle();
			data.putBoolean("editMode", true);
			JoinMucDialog dlg = JoinMucDialog.newInstance(data);
			dlg.show(getActivity().getSupportFragmentManager(), "dialog");
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.bookmarks_menu, menu);
		MenuItem addItem = menu.findItem(R.id.add);
		MenuItemCompat.setShowAsAction(addItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		super.onCreateOptionsMenu(menu, inflater);
	}

	public void publishBookmarks(final BareJID accountJID, final List<Bookmark> bookmarks, final AsyncCallback callback) {
		new Thread() {
			@Override
			public void run() {

				List<ParcelableElement> items = new ArrayList<ParcelableElement>();

				for (Bookmark bookmark : bookmarks) {
					try {
						Element item = ElementFactory.create("conference");
						if (bookmark.name != null && bookmark.name.length() > 0) {
							item.setAttribute("name", bookmark.name);
						}
						item.setAttribute("jid", bookmark.jid.toString());
						if (bookmark.autojoin) {
							item.setAttribute("autojoin", "true");
						}
						if (bookmark.nick != null && bookmark.nick.length() > 0) {
							item.addChild(ElementFactory.create("nick", bookmark.nick, null));
						}
						if (bookmark.password != null && bookmark.password.length() > 0) {
							item.addChild(ElementFactory.create("password", bookmark.password, null));
						}
						items.add(ParcelableElement.fromElement(item));
					} catch (XMLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				IJaxmppService jaxmppService = ((MainActivity) BookmarksFragment.this.getActivity()).getJaxmppService();
				//MultiJaxmpp multi = ((MessengerApplication) getApplicationContext()).getMultiJaxmpp();
				//BookmarksModule module = multi.get(accountJID).getModule(BookmarksModule.class);
				try {
					jaxmppService.publishBookmarks(accountJID.toString(), items, new AsyncXmppCallback(callback));
				} catch (RemoteException ex) {
					ex.printStackTrace();
				}
			}
		}.start();
	}

	private void refresh() {
		listView.post(new Runnable() {
			@Override
			public void run() {
				adapter.clear();
			}
		});

		new Thread() {
			public void run() {
				IJaxmppService jaxmppService = ((MainActivity) BookmarksFragment.this.getActivity()).getJaxmppService();
				try {
					List<String> accounts = jaxmppService.getAccounts(true);
					for (String account : accounts) {
						jaxmppService.retrieveBookmarks(account, new AsyncXmppCallback(new BookmarksAsyncCallbackImpl(
								BareJID.bareJIDInstance(account), adapter)));
					}
				} catch (RemoteException ex) {
					ex.printStackTrace();
				}
			}
		}.start();

		this.registerForContextMenu(listView);
	}

	public void removeItems(List<Bookmark> bookmarks) {
		Map<BareJID, List<Bookmark>> map = new HashMap<BareJID, List<Bookmark>>();
		for (int i = 0; i < adapter.getGroupCount(); i++) {
			BareJID account = (BareJID) adapter.getGroup(i);
			map.put(account, adapter.getChildrenForGroup(account));
		}

		Set<BareJID> changedAccounts = new HashSet<BareJID>();

		for (Bookmark bookmark : bookmarks) {
			List<Bookmark> list = map.get(bookmark.accountJid);
			list.remove(bookmark);
			changedAccounts.add(bookmark.accountJid);
		}

		// need to send changed bookmarks
		for (BareJID accountJID : changedAccounts) {
			publishBookmarks(accountJID, map.get(accountJID), new AsyncCallback() {

				@Override
				public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
					refresh();
					Toast.makeText(BookmarksFragment.this.getActivity(), "Could not remove bookmark", Toast.LENGTH_SHORT).show();
				}

				@Override
				public void onSuccess(Stanza responseStanza) throws JaxmppException {
					refresh();
					Toast.makeText(BookmarksFragment.this.getActivity(), "Bookmarks removed", Toast.LENGTH_SHORT).show();
				}

				@Override
				public void onTimeout() throws JaxmppException {
					refresh();
					Toast.makeText(BookmarksFragment.this.getActivity(), "Request timed out", Toast.LENGTH_SHORT).show();
				}

			});
		}
	}

	public void saveItem(Bundle data) {
		String id = data.getString("id");
		BareJID account = BareJID.bareJIDInstance(data.getString("account"));

		List<Bookmark> bookmarks = adapter.getChildrenForGroup(account);
		if (bookmarks == null) {
			bookmarks = new ArrayList<Bookmark>(1);
		}
		Bookmark bookmark = null;
		for (Bookmark item : bookmarks) {
			if (id != null && id.equals(item.id)) {
				bookmark = item;
			}
		}

		if (bookmark == null) {
			bookmark = new Bookmark();
			bookmark.accountJid = account;
			bookmarks.add(bookmark);
		}

		bookmark.jid = BareJID.bareJIDInstance(data.getString("room"), data.getString("server"));
		bookmark.name = data.getString("name");
		bookmark.nick = data.getString("nick");
		bookmark.password = data.getString("password");
		bookmark.autojoin = data.getBoolean("autojoin");

		publishBookmarks(account, bookmarks, new AsyncCallback() {

			@Override
			public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
				refresh();
				Toast.makeText(BookmarksFragment.this.getActivity(), "Could not remove bookmark", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onSuccess(Stanza responseStanza) throws JaxmppException {
				refresh();
				Toast.makeText(BookmarksFragment.this.getActivity(), "Bookmark saved", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onTimeout() throws JaxmppException {
				refresh();
				Toast.makeText(BookmarksFragment.this.getActivity(), "Request timed out", Toast.LENGTH_SHORT).show();
			}

		});
	}
}