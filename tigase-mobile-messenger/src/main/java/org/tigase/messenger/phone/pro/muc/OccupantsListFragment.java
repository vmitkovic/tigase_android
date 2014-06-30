package org.tigase.messenger.phone.pro.muc;

import java.util.ArrayList;

import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.roster.RosterAdapterHelper;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;

import tigase.jaxmpp.core.client.JID;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class OccupantsListFragment extends Fragment {

	private class OccupantsAdapter extends BaseAdapter {

		private static final String TAG = "OccupantsAdapter";

		private final LayoutInflater mInflater;

		private final ArrayList<Occupant> occupants = new ArrayList<Occupant>();

		public OccupantsAdapter(Context mContext, String account, String room) {
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return occupants.size();
		}

		@Override
		public Object getItem(int arg0) {
			Occupant o = occupants.get(arg0);
			return o;
		}

		@Override
		public long getItemId(int position) {
			return occupants.get(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView == null) {
				view = mInflater.inflate(R.layout.muc_occupants_list_item, parent, false);
			} else {
				view = convertView;
			}

			final Occupant occupant = (Occupant) getItem(position);

			final TextView nicknameTextView = (TextView) view.findViewById(R.id.occupant_nickname);
			final TextView statusTextView = (TextView) view.findViewById(R.id.occupant_status_description);
			final ImageView occupantIcon = (ImageView) view.findViewById(R.id.occupant_icon);
			final ImageView occupantPresence = (ImageView) view.findViewById(R.id.occupant_presence);
			final ImageView occupantAvatar = (ImageView) view.findViewById(R.id.occupant_avatar);

			// try {
			nicknameTextView.setText(occupant.getNickname());
			// int colorRes =
			// MucAdapter.getOccupantColor(occupant.getNickname());

			// looks like enabled text is still gray but darker than
			// disabled item
			// but setting color in code fixes color of displayed text
			// nicknameTextView.setTextColor(getResources().getColor(colorRes));
			statusTextView.setTextColor(getResources().getColor(android.R.color.primary_text_light));

			String status = occupant.getPresence().getDescription();
			statusTextView.setText(status == null ? "" : status);
			switch (occupant.getRole()) {
			case moderator:
				occupantIcon.setImageResource(R.drawable.occupant_moderator);
				occupantIcon.setVisibility(View.VISIBLE);
				break;
			default:
				occupantIcon.setVisibility(View.INVISIBLE);
				break;
			}

			int st = occupant.getPresence().getStatus();
			int resource = RosterAdapterHelper.cPresenceToImageResource(st);

			occupantPresence.setImageResource(resource);

			JID userJid = occupant.getJid();

			if (userJid != null) {
				AvatarHelper.setAvatarToImageView(userJid.getBareJid(), occupantAvatar);
			} else {
				occupantAvatar.setImageResource(R.drawable.user_avatar);
			}

			// } catch (XMLException e) {
			// Log.e(TAG, "Can't show occupant", e);
			// }

			return view;
		}

		public void refresh() {
			// IJaxmppService jaxmppService = ((MainActivity)
			// getActivity()).getJaxmppService();
			try {
				Occupant[] occupants = jaxmppService.getRoomOccupants(account, room);
				for (Occupant occupant : occupants) {
					this.occupants.add(occupant);
				}
				notifyDataSetChanged();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void remove(Occupant occupant) {
			occupants.remove(occupant);
			notifyDataSetChanged();
		}

		public void update(Occupant occupant) {
			occupants.remove(occupant);
			occupants.add(occupant);
			notifyDataSetChanged();
		}
	}

	private String account;

	private OccupantsAdapter adapter;
	private IJaxmppService jaxmppService;
	private View layout;

	// private final Listener<MucEvent> mucListener;
	// private MucModule mucModule;
	private GridView occupantsList;
	// private Room room;
	private String room;

	public OccupantsListFragment() {
		// mucListener = new Listener<MucEvent>() {
		//
		// @Override
		// public void handleEvent(MucEvent be) throws JaxmppException {
		// if (be.getRoom() == room && adapter != null)
		// onRoomEvent(be);
		// }
		// };
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (getArguments() != null) {
			account = getArguments().getString("account");
			room = getArguments().getString("jid");

			occupantsList = (GridView) layout.findViewById(R.id.occupants_list);
			occupantsList.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
					Occupant occupant = (Occupant) parent.getItemAtPosition(position);

					Intent intent = new Intent(getActivity(), MainActivity.class);

					// intent.setAction(TigaseMobileMessengerActivity.ROSTER_CLICK_MSG);
					// try {
					intent.putExtra("nickname", occupant.getNickname());
					// } catch (XMLException e) {
					// }

					getActivity().setResult(Activity.RESULT_OK, intent);
					getActivity().finish();
				}
			});

			adapter = new OccupantsAdapter(getActivity().getApplicationContext(), account, room);
			occupantsList.setAdapter(adapter);
			if (jaxmppService != null) {
				adapter.refresh();
			}
		}
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		layout = inflater.inflate(R.layout.muc_occupants_list, container, false);

		return layout;
	}

	@Override
	public void onDestroy() {
		// if (mucModule != null)
		// mucModule.removeListener(mucListener);
		super.onDestroy();
	}

	public void setJaxmppService(IJaxmppService jaxmppService) {
		this.jaxmppService = jaxmppService;
		if (adapter != null) {
			adapter.refresh();
		}
	}

	// protected void onRoomEvent(final MucEvent be) {
	// occupantsList.post(new Runnable() {
	//
	// @Override
	// public void run() {
	// if (be.getType() == MucModule.OccupantComes) {
	// adapter.add(be.getOccupant());
	// } else if (be.getType() == MucModule.OccupantLeaved) {
	// adapter.remove(be.getOccupant());
	// } else if (be.getType() == MucModule.OccupantChangedPresence) {
	// adapter.update(be.getOccupant());
	// } else if (be.getType() == MucModule.OccupantChangedNick) {
	// adapter.update(be.getOccupant());
	// }
	//
	// adapter.notifyDataSetChanged();
	//
	// }
	// });
	// }
}
