package org.tigase.messenger.phone.pro.chat;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.providers.OpenChatsProvider;
import org.tigase.messenger.phone.pro.roster.RosterFragment;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ChatsListFragment extends Fragment {

	public static ChatsListFragment newInstance(String layout) {
		ChatsListFragment f = new ChatsListFragment();

		Bundle args = new Bundle();
		args.putString("layout", layout);
		f.setArguments(args);

		return f;
	}	
	
	private static boolean DEBUG = false;
	private static final String TAG = "ChatsListFragment";

	private Cursor c = null;
	private ListAdapter adapter = null;
	private AbsListView listView = null;
	
	private String chatsLayout = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments() != null) {
			this.chatsLayout = getArguments().getString("layout");
		}

		this.setHasOptionsMenu(true);
	}	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// menu.clear();

		inflater.inflate(R.menu.chat_list_menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (DEBUG)
			Log.d(TAG + "_rf", "onCreateView()");

		if (getArguments() != null) {
			this.chatsLayout = getArguments().getString("layout");
		}

		View layout;
//		if ("groups".equals(this.rosterLayout)) {
//			layout = inflater.inflate(R.layout.roster_list, null);
//		} else if ("flat".equals(this.rosterLayout)) {
		layout = inflater.inflate(R.layout.chats_list, null);
//		} else if ("grid".equals(this.rosterLayout)) {
//			layout = inflater.inflate(R.layout.roster_list_grid, null);
//		} else {
//			throw new RuntimeException("Unknown roster layout");
//		}

		listView = (AbsListView) layout.findViewById(R.id.chatsList);
		listView.setTextFilterEnabled(true);
		registerForContextMenu(listView);

//		if (listView instanceof ExpandableListView) {
//			if (c != null) {
//				getActivity().stopManagingCursor(c);
//			}
//			this.c = inflater.getContext().getContentResolver().query(Uri.parse(RosterProvider.GROUP_URI), null, null, null,
//					null);
//			getActivity().startManagingCursor(c);
//			GroupsRosterAdapter.staticContext = inflater.getContext();
//
//			this.adapter = new GroupsRosterAdapter(inflater.getContext(), c);
//
//			((ExpandableListView) listView).setAdapter((ExpandableListAdapter) adapter);
//			((ExpandableListView) listView).setOnChildClickListener(new OnChildClickListener() {
//
//				@Override
//				public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
//
//					Log.i(TAG, "Clicked on id=" + id);
//
//					Intent intent = new Intent();
//					intent.setAction(TigaseMobileMessengerActivity.ROSTER_CLICK_MSG);
//					intent.putExtra("id", id);
//
//					getActivity().getApplicationContext().sendBroadcast(intent);
//					return true;
//				}
//			});
//		} else if (listView instanceof ListView || listView instanceof GridView) {
			if (c != null) {
				getActivity().stopManagingCursor(c);
			}
			this.c = inflater.getContext().getContentResolver().query(Uri.parse(OpenChatsProvider.OPEN_CHATS_URI), null, null, null,
					null);

			getActivity().startManagingCursor(c);
			// FlatRosterAdapter.staticContext = inflater.getContext();

			this.adapter = new OpenChatsAdapter(inflater.getContext(), R.layout.chat_list_item, c);
			((ListView) listView).setAdapter((ListAdapter) adapter);
//			if (listView instanceof ListView) {
//				this.adapter = new FlatRosterAdapter(inflater.getContext(), c, R.layout.roster_item);
//				((ListView) listView).setAdapter((ListAdapter) adapter);
//			} else if (listView instanceof GridView) {
//				this.adapter = new FlatRosterAdapter(inflater.getContext(), c, R.layout.roster_grid_item);
//				((GridView) listView).setAdapter((ListAdapter) adapter);
//			}

			listView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Log.i(TAG, "Clicked on chat with id=" + id);

					Intent intent = new Intent(getActivity(), ChatActivity.class);
					intent.putExtra("chatId", id);
					getActivity().startActivity(intent);
//					intent.setAction(TigaseMobileMessengerActivity.ROSTER_CLICK_MSG);
//					intent.putExtra("id", id);
//
//					getActivity().getApplicationContext().sendBroadcast(intent);
				}
			});
//
//		}
		// there can be no connection status icon - we have notifications and
		// accounts view in Android >= 3.0
//		this.connectionStatus = (ImageView) layout.findViewById(R.id.connection_status);
//		this.progressBar = (ProgressBar) layout.findViewById(R.id.progressBar1);
//
//		if (DEBUG)
//			Log.d(TAG + "_rf", "layout created");
//
//		long[] expandedIds = savedInstanceState == null ? null : savedInstanceState.getLongArray("ExpandedIds");
//		if (expandedIds != null) {
//			restoreExpandedState(expandedIds);
//		}

		return layout;
	}

	@Override
	public void onDestroyView() {
		if (c != null) {
			if (DEBUG)
				Log.d(TAG, "Closing cursor");
			c.close();
		}
		super.onDestroyView();
		if (DEBUG)
			Log.d(TAG + "_rf", "onDestroyView()");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.newChat) {
			Log.v(TAG, "new chat button clicked");
			FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
			//ft.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left);
			ft.setCustomAnimations(R.animator.slide_in_left, R.animator.slide_out_right, R.animator.slide_out_left, R.animator.slide_in_right);
			RosterFragment rosterFragment = RosterFragment.newInstance(null);
			ft.replace(R.id.content_frame, rosterFragment);
			ft.addToBackStack(null);
			ft.commit();
		}
		else if (item.getItemId() == R.id.newGroupChat) {
			Log.v(TAG, "new group chat button not supported yet");
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (DEBUG)
			Log.d(TAG + "_rf", "onResume()");
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

//		this.expandedIds = getExpandedIds();
//		if (DEBUG)
//			Log.d(TAG, "Save roster view state." + (this.expandedIds != null));
//		outState.putLongArray("ExpandedIds", this.expandedIds);
	}

	@Override
	public void onStart() {
		super.onStart();
//		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();
//
//		jaxmpp.addListener(Connector.StateChanged, this.connectorListener);
//		jaxmpp.addListener(JaxmppCore.Connected, this.connectedListener);
//		updateConnectionStatus();
//
//		if (DEBUG)
//			Log.d(TAG + "_rf", "onStart() " + getView());
//
//		if (this.expandedIds != null) {
//			restoreExpandedState(expandedIds);
//		}
	}

	@Override
	public void onStop() {
//		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();
//
//		jaxmpp.removeListener(Connector.StateChanged, this.connectorListener);
//		jaxmpp.removeListener(JaxmppCore.Connected, this.connectedListener);
		super.onStop();

//		expandedIds = getExpandedIds();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (DEBUG)
			Log.d(TAG + "_rf", "onViewCreated()");
	}	
	
}
