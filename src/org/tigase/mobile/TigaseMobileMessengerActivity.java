package org.tigase.mobile;

import org.tigase.mobile.db.MessengerDatabaseHelper;
import org.tigase.mobile.db.providers.AbstractRosterProvider;

import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ViewFlipper;

public class TigaseMobileMessengerActivity extends Activity {

	private NotificationManager notificationManager;

	private ListView rosterList;

	private float oldTouchValue;

	@Override
	public boolean onTouchEvent(MotionEvent touchevent) {
		switch (touchevent.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			oldTouchValue = touchevent.getX();
			break;
		}

		case MotionEvent.ACTION_UP: {

			float currentX = touchevent.getX();
			if (oldTouchValue < currentX) {
				viewSwitcher.setInAnimation(inFromLeftAnimation());
				viewSwitcher.setOutAnimation(outToRightAnimation());
				viewSwitcher.showNext();
			}
			if (oldTouchValue > currentX) {
				viewSwitcher.setInAnimation(inFromRightAnimation());
				viewSwitcher.setOutAnimation(outToLeftAnimation());
				viewSwitcher.showPrevious();
			}
			break;
		}
		}
		return false;
	}

	private ViewFlipper viewSwitcher;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.roster);

		this.viewSwitcher = (ViewFlipper) findViewById(R.id.viewSwitcher);

		Button bPrev = (Button) findViewById(R.id.prevButton);
		bPrev.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onClickPrev();
			}
		});
		Button bNext = (Button) findViewById(R.id.nextButton);
		bNext.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onClickNext();
			}
		});

		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		this.rosterList = (ListView) findViewById(R.id.rosterList);
		this.rosterList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				CursorWrapper cw = (CursorWrapper) rosterList.getItemAtPosition(position);

				JID jid = JID.jidInstance(cw.getString(1));

				openChatWith(jid);

				// Intent i = new Intent(TigaseMobileMessengerActivity.this,
				// ChatActivity.class);
				// startActivity(i);
			}
		});

		updateConnectionStatus();

		Cursor c = getContentResolver().query(Uri.parse(AbstractRosterProvider.CONTENT_URI), null, null, null, null);
		startManagingCursor(c);
		RosterAdapter adapter = new RosterAdapter(this, R.layout.roster_item, c);

		// final ArrayAdapter<String> adapter = new
		// ArrayAdapter<String>(getApplicationContext(), R.layout.item, item);
		// adapter.setNotifyOnChange(true);
		rosterList.setAdapter(adapter);

		if (!XmppService.jaxmpp().isConnected()) {
			MessengerDatabaseHelper h = new MessengerDatabaseHelper(getApplicationContext());
			h.open();
			h.makeAllOffline();
			h.close();
		}

		XmppService.jaxmpp().addListener(Connector.StateChanged, new Listener<ConnectorEvent>() {

			@Override
			public void handleEvent(ConnectorEvent be) throws JaxmppException {
				updateConnectionStatus();
			}
		});

	}

	protected ChatView findChatView(final JID jid) {
		for (int i = 0; i < viewSwitcher.getChildCount(); i++) {
			View v = viewSwitcher.getChildAt(i);
			if (v instanceof ChatView) {
				if (((ChatView) v).getChat().getJid().getBareJid().equals(jid.getBareJid())) {
					return (ChatView) v;
				}
			}
		}
		return null;
	}

	protected int getViewIndex(final View view) {
		for (int i = 0; i < viewSwitcher.getChildCount(); i++) {
			if (view == viewSwitcher.getChildAt(i))
				return i;
		}
		return -1;
	}

	protected void openChatWith(final JID jid) {
		try {
			ChatView l = findChatView(jid);
			if (l == null) {
				final Chat cc = XmppService.jaxmpp().createChat(jid);
				LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				l = (ChatView) inflater.inflate(R.layout.chat, null);
				l.setChat(cc);
				viewSwitcher.addView(l);
			}

			int idx = getViewIndex(l);
			viewSwitcher.setInAnimation(inFromRightAnimation());
			viewSwitcher.setOutAnimation(outToLeftAnimation());
			viewSwitcher.setDisplayedChild(idx);
		} catch (JaxmppException e) {
			throw new RuntimeException(e);
		}
	}

	protected Animation inFromRightAnimation() {

		Animation inFromRight = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, +1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
		inFromRight.setDuration(500);
		inFromRight.setInterpolator(new AccelerateDecelerateInterpolator());
		return inFromRight;
	}

	protected Animation outToLeftAnimation() {
		Animation outtoLeft = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
		outtoLeft.setDuration(500);
		outtoLeft.setInterpolator(new AccelerateDecelerateInterpolator());
		return outtoLeft;
	}

	protected Animation inFromLeftAnimation() {
		Animation inFromLeft = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
		inFromLeft.setDuration(500);
		inFromLeft.setInterpolator(new AccelerateDecelerateInterpolator());
		return inFromLeft;
	}

	protected Animation outToRightAnimation() {
		Animation outtoRight = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, +1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
		outtoRight.setDuration(500);
		outtoRight.setInterpolator(new AccelerateDecelerateInterpolator());
		return outtoRight;
	}

	protected void onClickNext() {
		viewSwitcher.setInAnimation(inFromRightAnimation());
		viewSwitcher.setOutAnimation(outToLeftAnimation());
		viewSwitcher.showNext();
	}

	protected void onClickPrev() {
		// TODO Auto-generated method stub
		viewSwitcher.setInAnimation(inFromLeftAnimation());
		viewSwitcher.setOutAnimation(outToRightAnimation());
		viewSwitcher.showPrevious();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		MenuItem con = menu.findItem(R.id.connectButton);
		MenuItem dcon = menu.findItem(R.id.disconnectButton);

		Connector.State st = XmppService.jaxmpp().getConnector() == null ? State.disconnected
				: XmppService.jaxmpp().getConnector().getState();

		con.setEnabled(st == State.disconnected);
		dcon.setEnabled(st == State.connected || st == State.connecting);

		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.propertiesButton:
			Intent intent = new Intent().setClass(this, MessengerPreferenceActivity.class);
			this.startActivityForResult(intent, 0);
			break;
		case R.id.disconnectButton:
			stopService(new Intent(TigaseMobileMessengerActivity.this, JaxmppService.class));
			break;
		case R.id.connectButton:
			// Toast.makeText(getApplicationContext(), "Connecting...",
			// Toast.LENGTH_LONG).show();

			SharedPreferences prefs = getSharedPreferences("org.tigase.mobile_preferences", 0);
			JID jid = JID.jidInstance(prefs.getString("user_jid", null));
			String password = prefs.getString("user_password", null);
			String hostname = prefs.getString("hostname", null);

			XmppService.jaxmpp().getProperties().setUserProperty(SocketConnector.SERVER_HOST, hostname);
			XmppService.jaxmpp().getProperties().setUserProperty(SessionObject.USER_JID, jid);
			XmppService.jaxmpp().getProperties().setUserProperty(SessionObject.PASSWORD, password);

			startService(new Intent(TigaseMobileMessengerActivity.this, JaxmppService.class));

			// try {
			// XmppService.jaxmpp().login(false);
			// } catch (JaxmppException e) {
			// Log.e("messenger", "Can't connect", e);
			// Toast.makeText(getApplicationContext(), "Connection error!",
			// Toast.LENGTH_LONG).show();
			// }
		default:
			break;
		}
		return true;
	}

	private void updateConnectionStatus() {
		final ImageView connectionStatus = (ImageView) findViewById(R.id.connection_status);

		final Connector.State st = XmppService.jaxmpp().getConnector() == null ? State.disconnected
				: XmppService.jaxmpp().getConnector().getState();

		connectionStatus.post(new Runnable() {

			@Override
			public void run() {
				if (st == State.connected) {
					connectionStatus.setImageResource(R.drawable.user_available);
				} else if (st == State.disconnected) {
					connectionStatus.setImageResource(R.drawable.user_offline);
				} else {
					connectionStatus.setImageResource(R.drawable.user_extended_away);
				}
			}
		});

	}
}