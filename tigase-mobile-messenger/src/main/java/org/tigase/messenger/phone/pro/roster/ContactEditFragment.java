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

import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.providers.RosterProvider;

import tigase.jaxmpp.android.roster.RosterItemsCacheTableMetaData;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

public class ContactEditFragment extends Fragment {

	private static final String TAG = "tigase";
	public static final String FRAG_TAG = "ContactEditFragment";
	
	private Spinner groupEdit;
	private ArrayList<String> groups;
	private EditText jabberIdEdit;
	private EditText nameEdit;
	private CheckBox requestAuth;

	private long id = -1;
	private String account = null;
	private JID jid = null;
	
	@Override
	//public void onCreate(Bundle savedInstanceState) {
	// super.onCreate(savedInstanceState);
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.contact_edit, container, false);
		this.jabberIdEdit = (EditText) view.findViewById(R.id.ce_jabberid);
		this.nameEdit = (EditText) view.findViewById(R.id.ce_name);
		this.groupEdit = (Spinner) view.findViewById(R.id.ce_group);
		
		this.requestAuth = (CheckBox) view.findViewById(R.id.authRequest);
		final Button saveButton = (Button) view.findViewById(R.id.ce_saveButton);
		saveButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateItem();
			}
		});

		final Button cancelButton = (Button) view.findViewById(R.id.ce_cancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//finish();
				getActivity().onBackPressed();
			}
		});
		
		//setContentView(R.layout.contact_edit);
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (getArguments() != null) {
			id = getArguments().getLong("itemId", -1);
			account = getArguments().getString("account");

			if (getArguments().getString("jid") != null) {
				jid = JID.jidInstance(getArguments().getString("jid"));
			}			
		}
		else {
			id = -1;
			account = null;
			jid = null;
		}

	
		Cursor c = null;
		try {
		if (id != -1) {
			Uri uri = Uri.parse(RosterProvider.CONTENT_URI + "/" + id);
			c = getActivity().getContentResolver().query(uri, null, null, null, null);
			if (!c.moveToNext()) {
				c.close();
				c = null;
			}
		}
		else if (jid != null) {
			Uri uri = Uri.parse(RosterProvider.CONTENT_URI + "/" + Uri.encode(jid.getBareJid().toString()));
			c = getActivity().getContentResolver().query(uri, null, null, null, null);
			if (!c.moveToNext()) {
				c.close();
				c = null;
			}
		}

		if (c != null)
			jabberIdEdit.setText(c.getString(c.getColumnIndex(RosterItemsCacheTableMetaData.FIELD_JID)));
		jabberIdEdit.setEnabled(c == null);

		if (c != null)
			nameEdit.setText(c.getString(c.getColumnIndex(RosterItemsCacheTableMetaData.FIELD_NAME)));

		this.groups = new ArrayList<String>();//jaxmpp.getRoster().getGroups());
		Collections.sort(groups);
		groups.add(0, "- none -");

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item,
				groups.toArray(new String[] {}));
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		groupEdit.setAdapter(adapter);

		int position = 0;
//		if (rosterItem != null && rosterItem.getGroups().size() > 0) {
//			String x = rosterItem.getGroups().get(0);
//			position = groups.indexOf(x);
//		}
		groupEdit.setSelection(position);

		if (c == null) {
			requestAuth.setChecked(true);
			requestAuth.setVisibility(View.VISIBLE);
		} else {
			requestAuth.setChecked(false);
			requestAuth.setVisibility(View.INVISIBLE);
		}
		} finally {
			if (c != null)
				c.close();
		}
	}

	protected void updateItem() {
		final BareJID jid = BareJID.bareJIDInstance(jabberIdEdit.getText().toString());
		final String name = nameEdit.getText().toString();
		final ArrayList<String> g = new ArrayList<String>();
		int p = groupEdit.getSelectedItemPosition();
		if (p > 0) {
			g.add(groups.get(p));
		}

//		if (jid == null || jid.toString().length() == 0) {
//			WarningDialog.showWarning(ContactEditActivity.this, R.string.contact_edit_wrn_jid_cant_be_empty);
//			return;
//		}
//
//		if (jid.getLocalpart() == null || jid.getDomain() == null) {
//			WarningDialog.showWarning(ContactEditActivity.this, R.string.contact_edit_wrn_wrong_jid);
//			return;
//		}
//
//		if (name == null || name.length() == 0) {
//			WarningDialog.showWarning(ContactEditActivity.this, R.string.contact_edit_wrn_name_cant_be_empty);
//			return;
//		}

		final ProgressDialog dialog = ProgressDialog.show(getActivity(), "",
				getResources().getString(R.string.contact_edit_info_updating), true);
		
		
		IJaxmppService jaxmppService = ((MainActivity) getActivity()).getJaxmppService();
		
		try {
			jaxmppService.updateRosterItem(account, jid.toString(), name, g, requestAuth.getVisibility() == View.VISIBLE && requestAuth.isChecked(),
				new RosterUpdateCallback.Stub() {

					@Override
					public void onSuccess(String msg) throws RemoteException {
						// TODO Auto-generated method stub
						dialog.cancel();
						Log.v(TAG, "Roster item " + jid + " updated successfully, rolling back to roster fragment - " + msg);
						try {
							// this commented code below is not working, below new code
							//getActivity().onBackPressed();
							jabberIdEdit.post(new Runnable() {
								@Override
								public void run() {
									getActivity().getSupportFragmentManager().popBackStackImmediate();
								}
							});
						}
						catch (Exception ex) {
							ex.printStackTrace();
						}
					}

					@Override
					public void onFailure(String msg) throws RemoteException {
						// TODO Auto-generated method stub
						dialog.cancel();	
						Log.v(TAG, "Roster item " + jid + " update failed, error: " + msg);
					}
				
			});
		} catch (RemoteException e) {
			dialog.cancel();
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
