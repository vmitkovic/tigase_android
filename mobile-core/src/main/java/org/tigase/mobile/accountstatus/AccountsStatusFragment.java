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
package org.tigase.mobile.accountstatus;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.MultiJaxmpp;
import org.tigase.mobile.R;
import org.tigase.mobile.service.JaxmppService;
import org.tigase.mobile.utils.AvatarHelper;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.JaxmppCore.JaxmppEvent;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.PingModule;
import tigase.jaxmpp.core.client.xmpp.modules.PingModule.PingAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class AccountsStatusFragment extends ListFragment {

	public static interface AccountSelectionListener {
		void accountSelected(String jid);
	}

	public static final String TAG = "AccountStatusFragment";

	public static AccountsStatusFragment newInstance() {
		AccountsStatusFragment f = new AccountsStatusFragment();

		return f;
	}

	private final BroadcastReceiver accountModifiedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (view != null && adapter != null)
				view.post(new Runnable() {

					@Override
					public void run() {
						loadData();
						adapter.notifyDataSetChanged();
					}
				});
		}
	};

	private AccountSelectionListener accountSelectionListener;

	private ArrayAdapter<Jaxmpp> adapter;

	private final Listener<JaxmppEvent> connectedListener = new Listener<JaxmppEvent>() {

		@Override
		public void handleEvent(JaxmppEvent be) throws JaxmppException {
			view.post(new Runnable() {

				@Override
				public void run() {
					adapter.notifyDataSetChanged();
				}
			});
		}
	};

	private final Listener<ConnectorEvent> connectorListener = new Listener<Connector.ConnectorEvent>() {

		@Override
		public void handleEvent(ConnectorEvent be) throws JaxmppException {
			// final MultiJaxmpp multi = ((MessengerApplication)
			// getActivity().getApplicationContext()).getMultiJaxmpp();

			// int p = adapter.getPosition((Jaxmpp)
			// multi.get(be.getSessionObject()));

			view.post(new Runnable() {

				@Override
				public void run() {
					adapter.notifyDataSetChanged();
				}
			});

		}
	};

	private BareJID selected;

	private int selectedColor = Color.parseColor("#ADD8E6");

	private View view;

	public AccountsStatusFragment() {
	}

	public void createOptionsMenu(Menu menu, Jaxmpp jaxmpp) {
		if (jaxmpp == null || jaxmpp.isConnected()) {
			menu.add(0, R.string.logoutButton, 0, R.string.logoutButton);
			menu.add(0, R.string.accountVCard, 0, R.string.accountVCard);
			menu.add(0, R.string.pingServer, 0, R.string.pingServer);
			menu.add(0, R.string.account_advanced_preferences, 0, R.string.account_advanced_preferences);
		}
		if (jaxmpp == null || !jaxmpp.isConnected()) {
			menu.add(0, R.string.loginButton, 0, R.string.loginButton);
		}
	}

	private int extractPosition(ContextMenuInfo menuInfo) {
		if (menuInfo instanceof AdapterContextMenuInfo) {
			return ((AdapterContextMenuInfo) menuInfo).position;
		} else {
			return -1;
		}
	}

	public Jaxmpp getSelectedJaxmpp(MenuItem item) {
		Jaxmpp jaxmpp = null;
		if (item != null && item.getMenuInfo() instanceof AdapterContextMenuInfo) {
			int position = ((AdapterContextMenuInfo) item.getMenuInfo()).position;
			if (position >= 0 && position < adapter.getCount()) {
				jaxmpp = adapter.getItem(position);
			}
		}
		if (jaxmpp == null && selected != null) {
			jaxmpp = ((MessengerApplication) getActivity().getApplication()).getMultiJaxmpp().get(selected);
		}
		return jaxmpp;
	}

	private void loadData() {
		adapter.clear();
		for (JaxmppCore jaxmpp : ((MessengerApplication) getActivity().getApplication()).getMultiJaxmpp().get()) {
			adapter.add((Jaxmpp) jaxmpp);
		}
		if (!adapter.isEmpty()) {
			setAccountSelected(adapter.getItem(0));
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return onMenuItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		if (!JaxmppService.isServiceActive()) {
			return;
		}

		final int position = extractPosition(menuInfo);
		Log.v(TAG, "position for context menu element is " + position);
		if (position == -1) {
			return;
		}

		final Jaxmpp jaxmpp = adapter.getItem(position);
		createOptionsMenu(menu, jaxmpp);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		createOptionsMenu(menu, null);

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.setHasOptionsMenu(true);
		// this.view = inflater.inflate(R.layout.account_status, null);
		this.view = super.onCreateView(inflater, container, savedInstanceState);

		this.adapter = new ArrayAdapter<Jaxmpp>(getActivity().getApplicationContext(), R.layout.account_status_item) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = convertView;
				if (v == null) {
					v = inflater.inflate(R.layout.account_status_item, null);
				}

				Jaxmpp jaxmpp = getItem(position);

				final TextView accountName = (TextView) v.findViewById(R.id.account_name);
				final TextView accountDescription = (TextView) v.findViewById(R.id.account_item_description);
				final ImageView accountStatus = (ImageView) v.findViewById(R.id.account_status);
				final ImageView securityEmblem = (ImageView) v.findViewById(R.id.account_secured_emblem);
				final ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.account_status_progress);
				final ImageView accountAvatar = (ImageView) v.findViewById(R.id.imageView1);

				final boolean established = jaxmpp.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID) != null;
				State st = jaxmpp.getSessionObject().getProperty(Connector.CONNECTOR_STAGE_KEY);
				if (st == null)
					st = State.disconnected;
				else if (st == State.connected && !established)
					st = State.connecting;

				boolean secured = st == State.disconnected ? false : jaxmpp.getConnector().isSecure();
				securityEmblem.setVisibility(st != State.disconnected && secured ? View.VISIBLE : View.GONE);

				String errorMessage = jaxmpp.getSessionObject().getProperty("messenger#error");

				if (st == State.disconnected && !TextUtils.isEmpty(errorMessage)) {
					accountDescription.setText("Error: " + errorMessage);
				} else
					accountDescription.setText("" + st);

				accountName.setText(jaxmpp.getSessionObject().getUserBareJid().toString());

				AvatarHelper.setAvatarToImageView(jaxmpp.getSessionObject().getUserBareJid(), accountAvatar);

				if (st == State.connected) {
					accountStatus.setImageResource(R.drawable.user_available);
					accountStatus.setVisibility(View.VISIBLE);
					progressBar.setVisibility(View.GONE);
				} else if (st == State.disconnected) {
					accountStatus.setImageResource(R.drawable.user_offline);
					accountStatus.setVisibility(View.VISIBLE);
					progressBar.setVisibility(View.GONE);
				} else {
					accountStatus.setVisibility(View.GONE);
					progressBar.setVisibility(View.VISIBLE);
				}

				boolean visible = (selected != null && selected.equals(jaxmpp.getSessionObject().getUserBareJid()));
				v.setBackgroundColor(visible ? selectedColor : Color.TRANSPARENT);
				return v;
			}

		};

		// loadData();

		this.setListAdapter(adapter);

		return view;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Jaxmpp jaxmpp = (Jaxmpp) l.getItemAtPosition(position);
		setAccountSelected(jaxmpp);
	}

	public boolean onMenuItemSelected(int id, final Jaxmpp jaxmpp) {
		if (id == R.string.logoutButton) {
			new Thread() {
				@Override
				public void run() {
					try {
						JaxmppService.disable(jaxmpp.getSessionObject(), true);
						jaxmpp.disconnect();
						((MessengerApplication) getActivity().getApplication()).clearPresences(jaxmpp.getSessionObject(), false);
					} catch (JaxmppException ex) {
						Log.e(TAG, "error manually disconnecting account "
								+ jaxmpp.getSessionObject().getUserBareJid().toString(), ex);
					}
				}
			}.start();
			return true;
		} else if (id == R.string.accountVCard) {
			Intent intent = new Intent();
			intent.setAction("org.tigase.mobile.account.personalInfo.EDIT");
			intent.putExtra("account_jid", jaxmpp.getSessionObject().getUserBareJid().toString());
			startActivity(intent);
			return true;
		} else if (id == R.string.pingServer) {
			new Thread() {
				@Override
				public void run() {
					try {
						PingModule.PingAsyncCallback pong = new PingAsyncCallback() {

							@Override
							public void onError(Stanza responseStanza, final ErrorCondition error) throws JaxmppException {
								FragmentActivity activity = getActivity();
								if (activity != null)
									activity.runOnUiThread(new Runnable() {

										@Override
										public void run() {
											Toast toast = Toast.makeText(getActivity(), "Ping error: " + error,
													Toast.LENGTH_LONG);
											toast.show();
										}
									});
							}

							@Override
							protected void onPong(final long time) {
								FragmentActivity activity = getActivity();
								if (activity != null)
									activity.runOnUiThread(new Runnable() {

										@Override
										public void run() {
											Toast toast = Toast.makeText(getActivity(), "Pong: " + time + "ms",
													Toast.LENGTH_LONG);
											toast.show();
										}
									});
							}

							@Override
							public void onTimeout() throws JaxmppException {
								FragmentActivity activity = getActivity();
								if (activity != null)
									activity.runOnUiThread(new Runnable() {

										@Override
										public void run() {
											Toast toast = Toast.makeText(getActivity(), "Ping timeout", Toast.LENGTH_LONG);
											toast.show();
										}
									});
							}
						};
						jaxmpp.getModule(PingModule.class).ping(
								JID.jidInstance(jaxmpp.getSessionObject().getUserBareJid().getDomain()), pong);
					} catch (Exception ex) {
						Log.e(TAG, "error pinging server " + jaxmpp.getSessionObject().getUserBareJid().toString(), ex);
					}
				}
			}.start();
			return true;
		} else if (id == R.string.account_advanced_preferences) {
			Intent intent = new Intent();
			intent.setAction("org.tigase.mobile.account.advancedPreferences.EDIT");
			intent.putExtra("account_jid", jaxmpp.getSessionObject().getUserBareJid().toString());
			startActivity(intent);
			return true;			
		} else if (id == R.string.loginButton) {
			new Thread() {
				@Override
				public void run() {
					try {
						JaxmppService.disable(jaxmpp.getSessionObject(), false);
						jaxmpp.login();
					} catch (JaxmppException ex) {
						Log.e(TAG,
								"error manually connecting account " + jaxmpp.getSessionObject().getUserBareJid().toString(),
								ex);
					}
				}
			}.start();
			return true;
		}
		return false;
	}

	public boolean onMenuItemSelected(MenuItem item) {
		int id = item.getItemId();
		Jaxmpp jaxmpp = getSelectedJaxmpp(item);

		return onMenuItemSelected(id, jaxmpp);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return onMenuItemSelected(item);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
				&& Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			MenuInflater inflater = new MenuInflater(this.getActivity().getApplicationContext());
			onCreateOptionsMenu(menu, inflater);
		}

		Jaxmpp jaxmpp = getSelectedJaxmpp(null);

		Log.v(TAG, "preparing options menu for account = " + (jaxmpp == null ? null : jaxmpp.getSessionObject().getUserBareJid().toString()));
		
		menu.findItem(R.string.loginButton).setVisible(jaxmpp != null && !jaxmpp.isConnected());
		menu.findItem(R.string.logoutButton).setVisible(jaxmpp != null && jaxmpp.isConnected());
		menu.findItem(R.string.accountVCard).setVisible(jaxmpp != null && jaxmpp.isConnected());
		menu.findItem(R.string.pingServer).setVisible(jaxmpp != null && jaxmpp.isConnected());
		menu.findItem(R.string.account_advanced_preferences).setVisible(jaxmpp != null && accountSelectionListener == null && jaxmpp.isConnected());

		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onStart() {
		super.onStart();
		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();

		jaxmpp.addListener(Connector.StateChanged, this.connectorListener);
		jaxmpp.addListener(JaxmppCore.Connected, this.connectedListener);

		getActivity().getApplicationContext().registerReceiver(this.accountModifiedReceiver,
				new IntentFilter(AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION));

		loadData();
		registerForContextMenu(getListView());
	}

	@Override
	public void onStop() {
		final MultiJaxmpp jaxmpp = ((MessengerApplication) getActivity().getApplicationContext()).getMultiJaxmpp();

		getActivity().getApplicationContext().unregisterReceiver(this.accountModifiedReceiver);
		jaxmpp.removeListener(Connector.StateChanged, this.connectorListener);
		jaxmpp.removeListener(JaxmppCore.Connected, this.connectedListener);

		super.onStop();
	}

	protected void setAccountSelected(Jaxmpp jaxmpp) {
		selected = jaxmpp.getSessionObject().getUserBareJid();		
		if (accountSelectionListener != null) {
			accountSelectionListener.accountSelected(jaxmpp.getSessionObject().getUserBareJid().toString());
		}
		adapter.notifyDataSetChanged();		
	}

	public void setAccountSelectedListener(AccountSelectionListener accountSelectionListener) {
		this.accountSelectionListener = accountSelectionListener;
	};
}
