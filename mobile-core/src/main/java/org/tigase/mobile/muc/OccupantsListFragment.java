package org.tigase.mobile.muc;

import java.util.ArrayList;

import org.tigase.mobile.MessengerApplication;
import org.tigase.mobile.R;
import org.tigase.mobile.RosterDisplayTools;
import org.tigase.mobile.TigaseMobileMessengerActivity;
import org.tigase.mobile.roster.CPresence;
import org.tigase.mobile.utils.AvatarHelper;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Occupant;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.modules.muc.XMucUserElement;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule.MucEvent;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class OccupantsListFragment extends Fragment {

	private class OccupantsAdapter extends BaseAdapter {

		private static final String TAG = "OccupantsAdapter";

		private final LayoutInflater mInflater;

		private final ArrayList<Occupant> occupants = new ArrayList<Occupant>();

		public OccupantsAdapter(Context mContext, Room room) {
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			occupants.addAll(room.getPresences().values());
			notifyDataSetChanged();
		}

		public void add(Occupant occupant) {
			occupants.add(occupant);
			notifyDataSetChanged();
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
			
			try {
				nicknameTextView.setText(occupant.getNickname());
				int colorRes = MucAdapter.getOccupantColor(occupant.getNickname());

				// looks like enabled text is still gray but darker than
				// disabled item
				// but setting color in code fixes color of displayed text
				nicknameTextView.setTextColor(getResources().getColor(colorRes));
				statusTextView.setTextColor(getResources().getColor(android.R.color.primary_text_light));

				String status = occupant.getPresence().getStatus();
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

				final CPresence s = RosterDisplayTools.getShowOf(occupant.getPresence());
				switch (s) {
				case chat:
					occupantPresence.setImageResource(R.drawable.user_free_for_chat);
					break;
				case online:
					occupantPresence.setImageResource(R.drawable.user_available);
					break;
				case away:
					occupantPresence.setImageResource(R.drawable.user_away);
					break;
				case xa:
					occupantPresence.setImageResource(R.drawable.user_extended_away);
					break;
				case dnd:
					occupantPresence.setImageResource(R.drawable.user_busy);
					break;
				case requested:
					occupantPresence.setImageResource(R.drawable.user_ask);
					break;
				case error:
					occupantPresence.setImageResource(R.drawable.user_error);
					break;
				case offline_nonauth:
					occupantPresence.setImageResource(R.drawable.user_noauth);
					break;
				default:
					occupantPresence.setImageResource(R.drawable.user_offline);
					break;
				}

				XMucUserElement xMucUser = XMucUserElement.extract(occupant.getPresence());
				JID userJid = null;
				if (xMucUser != null) {
					userJid = xMucUser.getJID();
				}
				if (userJid != null) {
					AvatarHelper.setAvatarToImageView(userJid.getBareJid(), occupantAvatar);
				}
				else {
					occupantAvatar.setImageResource(R.drawable.user_avatar);
				}
				
			} catch (XMLException e) {
				Log.e(TAG, "Can't show occupant", e);
			}

			return view;
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

	private OccupantsAdapter adapter;
	private View layout;
	private final Listener<MucEvent> mucListener;
	private MucModule mucModule;
	private GridView occupantsList;
	private Room room;

	public OccupantsListFragment() {
		mucListener = new Listener<MucEvent>() {

			@Override
			public void handleEvent(MucEvent be) throws JaxmppException {
				if (be.getRoom() == room && adapter != null)
					onRoomEvent(be);
			}
		};
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (getArguments() != null) {
			long roomId = getArguments().getLong("roomId", -1);

			room = ((MessengerApplication) getActivity().getApplication()).getMultiJaxmpp().getRoomById(roomId).getRoom();
			mucModule = ((MessengerApplication) getActivity().getApplication()).getMultiJaxmpp().get(room.getSessionObject()).getModule(
					MucModule.class);
			mucModule.addListener(mucListener);

			occupantsList = (GridView) layout.findViewById(R.id.occupants_list);
			occupantsList.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
					Occupant occupant = (Occupant) parent.getItemAtPosition(position);

					Intent intent = new Intent();

					intent.setAction(TigaseMobileMessengerActivity.ROSTER_CLICK_MSG);
					try {
						intent.putExtra("nickname", occupant.getNickname());
					} catch (XMLException e) {
					}

					getActivity().setResult(Activity.RESULT_OK, intent);
					getActivity().finish();
				}
			});

			adapter = new OccupantsAdapter(getActivity().getApplicationContext(), room);
			occupantsList.setAdapter(adapter);			
		}
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		layout = inflater.inflate(R.layout.muc_occupants_list, container, false);

		return layout;
	}

	@Override
	public void onDestroy() {
		if (mucModule != null)
			mucModule.removeListener(mucListener);
		super.onDestroy();
	}

	protected void onRoomEvent(final MucEvent be) {
		occupantsList.post(new Runnable() {

			@Override
			public void run() {
				if (be.getType() == MucModule.OccupantComes) {
					adapter.add(be.getOccupant());
				} else if (be.getType() == MucModule.OccupantLeaved) {
					adapter.remove(be.getOccupant());
				} else if (be.getType() == MucModule.OccupantChangedPresence) {
					adapter.update(be.getOccupant());
				} else if (be.getType() == MucModule.OccupantChangedNick) {
					adapter.update(be.getOccupant());
				}

				adapter.notifyDataSetChanged();

			}
		});
	}	
}
