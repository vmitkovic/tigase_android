package org.tigase.messenger.phone.pro;

import java.util.ArrayList;
import java.util.List;

import org.tigase.messenger.phone.pro.account.AccountAuthenticator;
import org.tigase.messenger.phone.pro.chat.ChatActivity;
import org.tigase.messenger.phone.pro.chat.ChatsListFragment;
import org.tigase.messenger.phone.pro.roster.RosterFragment;

import tigase.jaxmpp.core.client.BareJID;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends FragmentActivity implements RosterFragment.OnClickListener {
	
    private class DrawerMenuAdapter extends ArrayAdapter<DrawerMenuItem> {

    	private final Context context;
            private final List<DrawerMenuItem> items;

            public DrawerMenuAdapter(Context context, int textViewResourceId, List<DrawerMenuItem> items) {
                    super(context, textViewResourceId, items);
                    this.context = context;
                    this.items = items;
            }

            @Override
            public boolean areAllItemsEnabled() {
                    return false;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View rowView = inflater.inflate(R.layout.main_left_drawer_item, parent, false);
                    TextView textView = (TextView) rowView.findViewById(R.id.main_left_drawer_item_text);
                    ImageView imageView = (ImageView) rowView.findViewById(R.id.main_left_drawer_item_icon);

                    DrawerMenuItem item = items.get(position);

                    textView.setText(item.text);
                    imageView.setImageResource(item.icon);

                    return rowView;
            }

            @Override
            public boolean isEnabled(int pos) {
                    DrawerMenuItem item = getItem(pos);
                    boolean connected = false;

                    Account[] accounts = AccountManager.get(MainActivity.this).getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
                    for (Account account : accounts) {
                            try {
								connected |= jaxmppService != null && jaxmppService.isConnected(account.name);
							} catch (RemoteException e) {
							}
                    }

                    return super.isEnabled(pos) && (!item.connectionRequired || connected);
            }
    }

    private class DrawerMenuItem {
            final boolean connectionRequired;
            final int icon;
            final int id;
            final int text;

            public DrawerMenuItem(int id, int text, int icon) {
                    this(id, text, icon, false);
            }

            public DrawerMenuItem(int id, int text, int icon, boolean connectionRequired) {
                    this.id = id;
                    this.text = text;
                    this.icon = icon;
                    this.connectionRequired = connectionRequired;
            }
    }	
	
	private ServiceConnection messengerConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			MainActivity.this.messenger = new Messenger(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			messenger = null;
		}
		
	};    

	private ServiceConnection jaxmppServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			MainActivity.this.jaxmppService = IJaxmppService.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			jaxmppService = null;
		}
		
	};
	
	protected DrawerLayout drawerLayout;
	protected ListView drawerList;
	protected ActionBarDrawerToggle drawerToggle;
	
	private Messenger messenger;
	private IJaxmppService jaxmppService;
	
	private MainActivityHelper helper = MainActivityHelper.createInstance(this);
	
//	@Override
//	public void onBackPressed() {
//	  super.onBackPressed();
//	  //fragmentChanged();
//	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		this.drawerList = (ListView) findViewById(R.id.left_drawer);
		this.drawerLayout = (DrawerLayout) findViewById(R.id.main_activity);
		
        // creating list of items available in drawer menu
        final List<DrawerMenuItem> drawerMenuItems = new ArrayList<DrawerMenuItem>();
//        drawerMenuItems.add(new DrawerMenuItem(R.id.accountsList, R.string.accounts, R.drawable.ic_menu_account_list));
//        drawerMenuItems.add(new DrawerMenuItem(R.id.joinMucRoom, R.string.join_muc_room, R.drawable.group_chat, true));
//        drawerMenuItems.add(new DrawerMenuItem(R.id.bookmarksShow, R.string.bookmarks_show, android.R.drawable.star_off, true));
//        drawerMenuItems.add(new DrawerMenuItem(R.id.propertiesButton, R.string.propertiesButton,
//                        android.R.drawable.ic_menu_preferences));
//        drawerMenuItems.add(new DrawerMenuItem(R.id.aboutButton, R.string.aboutButton, android.R.drawable.ic_menu_info_details));
        drawerMenuItems.add(new DrawerMenuItem(R.id.appNameText, R.string.app_name, android.R.drawable.ic_menu_info_details));

        this.drawerList.setAdapter(new DrawerMenuAdapter(this.getApplicationContext(), R.layout.main_left_drawer_item,
                        drawerMenuItems));
        this.drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.accept,
                        R.string.accept);
	
        drawerLayout.setDrawerListener(this.drawerToggle);
        this.drawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView parent, View view, int position, long id) {
                        DrawerMenuItem item = drawerMenuItems.get(position);
                        if (item != null) {
                                drawerLayout.closeDrawers();
                                onOptionsItemSelected(item.id);
                        }
                }
        });		

        //getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new RosterFragment()).commit();
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new ChatsListFragment(), ChatsListFragment.FRAG_TAG).commit();
        
		startService(new Intent(this, JaxmppService.class));
		Intent intent = new Intent(this, JaxmppService.class);
		intent.putExtra("ID", "AIDL");
		bindService(intent, jaxmppServiceConnection, Context.BIND_AUTO_CREATE);
		
		helper.updateActionBar();        
	}
	
	public void onDestroy() {
		super.onDestroy();
		unbindService(jaxmppServiceConnection);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Pass the event to ActionBarDrawerToggle, if it returns
	    // true, then it has handled the app icon touch event
	    if (drawerToggle.onOptionsItemSelected(item)) {
	      return true;
	    }
	    if (item.getItemId() == android.R.id.home) {
	    	onBackPressed();
	    	return true;
	    }
	    
	    // Handle your other action bar items...

	    return super.onOptionsItemSelected(item);
	}	

	protected void onOptionsItemSelected(int itemId) {
		
	}

	@Override
	public void onRosterItemClicked(String account, BareJID jid) {
		try {
			// creating chat
			jaxmppService.openChat(account, jid.toString());
		
			// back to chat list fragment
			//Fragment frag = getSupportFragmentManager().findFragmentByTag(RosterFragment.FRAG_TAG);
			//getSupportFragmentManager().beginTransaction().remove(frag).commit();
			getSupportFragmentManager().popBackStack(RosterFragment.FRAG_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			
			fragmentChanged();
			
			// opening activity
			Intent intent = new Intent(this, ChatActivity.class);
			intent.putExtra("account", account);
			intent.putExtra("recipient", jid.toString());
			startActivity(intent);			
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public boolean isMainView() {
		Fragment frag = getSupportFragmentManager().findFragmentByTag(ChatsListFragment.FRAG_TAG);
		return (frag != null && !frag.isHidden() && !frag.isDetached() && frag.isMenuVisible() && !frag.isRemoving());// && frag.isInLayout());
	}
	
	public void fragmentChanged() {
		helper.updateActionBar();
	}
}
