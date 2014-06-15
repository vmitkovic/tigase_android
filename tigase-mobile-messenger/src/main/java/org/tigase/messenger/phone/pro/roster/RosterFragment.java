package org.tigase.messenger.phone.pro.roster;

import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.Preferences;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.providers.RosterProvider;

import tigase.jaxmpp.android.roster.RosterItemsCacheTableMetaData;
import tigase.jaxmpp.core.client.BareJID;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;

public class RosterFragment extends Fragment {

	public static interface OnClickListener {
		public void onRosterItemClicked(String action, String account, BareJID jid);
	}
	
//	public static RosterFragment newInstance(String layout, OnClickListener listener) {
	public static RosterFragment newInstance(String layout) {
		RosterFragment f = new RosterFragment();

		Bundle args = new Bundle();
		//args.putString("layout", layout);
		f.setArguments(args);

		return f;
	}
	
	public static final String FRAG_TAG = "roster_fragment";
	
	private static final boolean DEBUG = true;
	
	private static final String TAG = "RosterFragment";

	private Cursor c = null;
	private ListAdapter adapter = null;
	private AbsListView listView = null;
	
	private SharedPreferences mPreferences;
	private String rosterLayout = null;
	private String action = null;

	private OnClickListener onClickListener;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		if (activity instanceof OnClickListener) {
			onClickListener = (OnClickListener) activity;
		}
	}
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments() != null) {
			this.action = getArguments().getString("action");
			//this.rosterLayout = getArguments().getString("layout");
		}
		
		this.mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		rosterLayout = this.mPreferences.getString(Preferences.ROSTER_LAYOUT_KEY, "flat");
		//this.mPreferences.registerOnSharedPreferenceChangeListener(prefChangeListener);

	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (DEBUG)
			Log.d(TAG + "_rf", "onCreateView()");

		setHasOptionsMenu(true);
		
//		if (getArguments() != null) {
//			this.rosterLayout = getArguments().getString("layout");
//		}

		View layout;
//		if ("groups".equals(this.rosterLayout)) {
//			layout = inflater.inflate(R.layout.roster_list, null);
//		} else 
		if ("flat".equals(this.rosterLayout)) {
			layout = inflater.inflate(R.layout.roster_list_flat, null);
		} else if ("grid".equals(this.rosterLayout)) {
			layout = inflater.inflate(R.layout.roster_list_grid, null);
		} else {
			throw new RuntimeException("Unknown roster layout");
		}

		listView = (AbsListView) layout.findViewById(R.id.rosterList);
		listView.setTextFilterEnabled(true);
		//registerForContextMenu(listView);

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
//			if (c != null) {
//				getActivity().stopManagingCursor(c);
//			}
			reinitializeAdapters();
		
			if (onClickListener != null) {
				listView.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						Log.i(TAG, "Clicked on id=" + id);
						Uri uri = Uri.parse(RosterProvider.CONTENT_URI + "/" + id);
						Cursor c = inflater.getContext().getContentResolver().query(uri, null, null, null, null);
						if (c.moveToNext()) {
							String account = c.getString(c.getColumnIndex(RosterItemsCacheTableMetaData.FIELD_ACCOUNT));
							String jidStr = c.getString(c.getColumnIndex(RosterItemsCacheTableMetaData.FIELD_JID));
							BareJID jid = BareJID.bareJIDInstance(jidStr);
							onClickListener.onRosterItemClicked(action, account, jid);
						}
	//
//						Intent intent = new Intent();
//						intent.setAction(TigaseMobileMessengerActivity.ROSTER_CLICK_MSG);
//						intent.putExtra("id", id);
	//
//						getActivity().getApplicationContext().sendBroadcast(intent);
					}
				});
			}
//			listView.setOnItemClickListener(new OnItemClickListener() {
//
//				@Override
//				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//					Log.i(TAG, "Clicked on id=" + id);
//
//					Intent intent = new Intent();
//					intent.setAction(TigaseMobileMessengerActivity.ROSTER_CLICK_MSG);
//					intent.putExtra("id", id);
//
//					getActivity().getApplicationContext().sendBroadcast(intent);
//				}
//			});
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.roster_main_menu, menu);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.contactAdd), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		super.onCreateOptionsMenu(menu, inflater);
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
		if (item.getItemId() == R.id.connectButton) {
			try {
				Log.v(TAG, "Connecting..");
				((MainActivity) getActivity()).getJaxmppService().connect("");
				Log.v(TAG, "Connected.");
			} catch (RemoteException ex) {
				Log.v(TAG, "Exception connecting JaxmppService", ex);
			}
		}
		else if (item.getItemId() == R.id.disconnectButton) {
			try {
				Log.v(TAG, "Disconnecting..");
				((MainActivity) getActivity()).getJaxmppService().disconnect("");
				Log.v(TAG, "Disconnected.");
			} catch (RemoteException ex) {
				Log.v(TAG, "Exception disconnecting JaxmppService", ex);
			}			
		}
		else if (item.getItemId() == R.id.contactAdd) {
			ContactEditFragment frag = new ContactEditFragment();
			((MainActivity) getActivity()).switchFragments(frag, ContactEditFragment.FRAG_TAG);
			return true;
		}
		else if (item.getItemId() == R.id.showHideOffline) {
            boolean x = mPreferences.getBoolean(Preferences.SHOW_OFFLINE, Boolean.TRUE);

            Editor editor = mPreferences.edit();
            editor.putBoolean(Preferences.SHOW_OFFLINE, !x);
            editor.commit();

//            Uri insertedItem = Uri.parse(RosterProvider.CONTENT_URI);
//            getActivity().getContentResolver().notifyChange(insertedItem, null);			
            reinitializeAdapters();
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
		MainActivity activity = (MainActivity) getActivity();
		activity.fragmentChanged(this);
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

	private void reinitializeAdapters() {
		String selection = null;
		if (!showOffline()) {
			selection = "status";
		}
		if (c != null && !c.isClosed()) {
			c.close();
		}
		
		this.c = getActivity().getContentResolver().query(Uri.parse(RosterProvider.CONTENT_URI), null, selection, null,
				null);

		//getActivity().startManagingCursor(c);
		// FlatRosterAdapter.staticContext = inflater.getContext();

		if (listView instanceof ListView) {
			this.adapter = new FlatRosterAdapter(getActivity(), c, R.layout.roster_item);
			((ListView) listView).setAdapter((ListAdapter) adapter);
		} else if (listView instanceof GridView) {
			this.adapter = new FlatRosterAdapter(getActivity(), c, R.layout.roster_grid_item);
			((GridView) listView).setAdapter((ListAdapter) adapter);
		}		
	}
	
	private boolean showOffline() {
		return mPreferences.getBoolean(Preferences.SHOW_OFFLINE, false);
	}
}
