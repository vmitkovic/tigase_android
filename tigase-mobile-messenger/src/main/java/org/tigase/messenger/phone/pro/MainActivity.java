package org.tigase.messenger.phone.pro;

import java.util.ArrayList;
import java.util.List;

import org.tigase.messenger.phone.pro.account.AccountAuthenticator;
import org.tigase.messenger.phone.pro.bookmarks.BookmarksFragment;
import org.tigase.messenger.phone.pro.chat.ChatActivity;
import org.tigase.messenger.phone.pro.chat.ChatHistoryFragment;
import org.tigase.messenger.phone.pro.chat.ChatsListFragment;
import org.tigase.messenger.phone.pro.muc.JoinMucDialog;
import org.tigase.messenger.phone.pro.muc.MucRoomFragment;
import org.tigase.messenger.phone.pro.preferences.MessengerPreferenceActivity;
import org.tigase.messenger.phone.pro.roster.ContactFragment;
import org.tigase.messenger.phone.pro.roster.RosterFragment;
import org.tigase.messenger.phone.pro.service.JaxmppService;
import org.tigase.messenger.phone.pro.ui.MainTabsFragment;
import org.tigase.messenger.phone.pro.utils.AvatarHelper;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity implements RosterFragment.OnClickListener {
	
    private class DrawerMenuAdapter extends BaseExpandableListAdapter {

    	private final Context context;
            private final List<DrawerMenuItem> items;
            //private LayoutInflater mInflater;

            public DrawerMenuAdapter(Context context, int textViewResourceId, List<DrawerMenuItem> items) {
                    this.context = context;
                    this.items = items;
                    //mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }

            @Override
            public boolean areAllItemsEnabled() {
                    return false;
            }

//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                    View rowView = inflater.inflate(R.layout.main_left_drawer_item, parent, false);
//                    TextView textView = (TextView) rowView.findViewById(R.id.main_left_drawer_item_text);
//                    ImageView imageView = (ImageView) rowView.findViewById(R.id.main_left_drawer_item_icon);
//
//                    DrawerMenuItem item = items.get(position);
//
//                    textView.setText(item.text);
//                    imageView.setImageResource(item.icon);
//
//                    return rowView;
//            }
//
//            @Override
//            public boolean isEnabled(int pos) {
//                    DrawerMenuItem item = getItem(pos);
//                    boolean connected = false;
//
//                    Account[] accounts = AccountManager.get(MainActivity.this).getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
//                    for (Account account : accounts) {
//                            try {
//								connected |= jaxmppService != null && jaxmppService.isConnected(account.name);
//							} catch (RemoteException e) {
//							}
//                    }
//
//                    return super.isEnabled(pos) && (!item.connectionRequired || connected);
//            }

			@Override
			public int getGroupCount() {
				return items.size();
			}

			@Override
			public int getChildrenCount(int groupPosition) {
				return items.get(groupPosition).size();
			}

			@Override
			public Object getGroup(int groupPosition) {
				return items.get(groupPosition);
			}

			@Override
			public Object getChild(int groupPosition, int childPosition) {
				DrawerMenuItem item = items.get(groupPosition);
				if (item != null) {
					List<DrawerMenuItem> children = item.getChildren();
					if (children != null) {
						return children.get(childPosition);
					}
				}
				return null;
			}

			@Override
			public long getGroupId(int groupPosition) {
				return getGroup(groupPosition).hashCode();
			}

			@Override
			public long getChildId(int groupPosition, int childPosition) {
				return getChild(groupPosition, childPosition).hashCode();
			}

			@Override
			public int getChildType(int groupPosition, int childPosition) {
				final DrawerMenuItem item = (DrawerMenuItem) getChild(groupPosition, childPosition);
				return (item instanceof AccountDrawerMenuItem) ? 1 : 0;
			}
			
			@Override
			public int getChildTypeCount() {
				return 2;
			}

			@Override
			public int getGroupType(int groupPosition) {
				final DrawerMenuItem item = (DrawerMenuItem) getGroup(groupPosition);
				return (item.id == R.id.accounts_flipper) ? 1 : 0;
			}			
			
			@Override
			public int getGroupTypeCount() {
				return 2;
			}
			
			@Override
			public boolean hasStableIds() {
				return true;
			}

			@Override
			public View getGroupView(int groupPosition, boolean isExpanded,
					View convertView, ViewGroup parent) {
                DrawerMenuItem item = (DrawerMenuItem) getGroup(groupPosition);
				int viewId = R.layout.main_left_drawer_item;
				if (item.id == R.id.accounts_flipper) {
					viewId = R.layout.main_left_drawer_item_accounts;
				}
				LayoutInflater mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View rowView = (convertView == null || convertView.getId() != viewId) ? mInflater.inflate(viewId, null) : convertView;
                rowView.setId(viewId);
                TextView textView = (TextView) rowView.findViewById(R.id.main_left_drawer_item_text);
                ImageView imageView = (ImageView) rowView.findViewById(R.id.main_left_drawer_item_icon);

                if (item.text != 0) { 
                	textView.setText(item.text);
                } else {
                	textView.setText(item.textStr);
                }
                imageView.setImageResource(item.icon);

                if (item.id == R.id.accounts_flipper) {
                	CompoundButton state = (CompoundButton) rowView.findViewById(R.id.main_left_drawer_item_switch);
                	state.setFocusable(false);
        			Boolean value = false;
					try {
						value = jaxmppService != null && jaxmppService.isStarted();
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        			state.setChecked(value);
        			state.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(
								CompoundButton buttonView,
								boolean isChecked) {
							try {
								if (isChecked) {
									jaxmppService.connect("");
								}
								else {
									jaxmppService.disconnect("");
								}
							} catch (RemoteException e) {
								e.printStackTrace();
							}				
						}     				
        			});   
                }
                
                return rowView;
			}

			@Override
			public View getChildView(int groupPosition, int childPosition,
					boolean isLastChild, View convertView, ViewGroup parent) {
				int viewId = R.layout.main_left_drawer_item;
                final DrawerMenuItem item = (DrawerMenuItem) getChild(groupPosition, childPosition);
				if (item instanceof AccountDrawerMenuItem) {
					viewId = R.layout.main_left_drawer_item_account;
				}
				LayoutInflater mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View rowView = (convertView == null || convertView.getId() != viewId) ? mInflater.inflate(viewId, null) : convertView;
                rowView.setId(viewId);
                TextView textView = (TextView) rowView.findViewById(R.id.main_left_drawer_item_text);
                ImageView imageView = (ImageView) rowView.findViewById(R.id.main_left_drawer_item_icon);
                
                if (item.text != 0) { 
                	textView.setText(item.text);
                    imageView.setImageResource(item.icon);
                } else {
                	textView.setText(item.textStr);
                	AvatarHelper.setAvatarToImageView(BareJID.bareJIDInstance(item.textStr), imageView);
                }
                if (item instanceof AccountDrawerMenuItem) {
                	AccountManager am = AccountManager.get(MainActivity.this);
                	for (Account account : am.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)) {
                		if (item.textStr.equals(account.name)) {
                			CompoundButton state = (CompoundButton) rowView.findViewById(R.id.main_left_drawer_item_switch);
                			Boolean value = Boolean.valueOf(am.getUserData(account, "DISABLED"));
                			state.setChecked(value == null || !value);
                			state.setOnCheckedChangeListener(new OnCheckedChangeListener() {

								@Override
								public void onCheckedChanged(
										CompoundButton buttonView,
										boolean isChecked) {
									AccountManager am = AccountManager.get(MainActivity.this);
									for (Account account : am.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)) {
				                		if (item.textStr.equals(account.name)) {
				                			am.setUserData(account, "DISABLED", String.valueOf(!isChecked));
				                			String pass = am.getPassword(account);
				                			am.setPassword(account, pass);
				                		}
									}
								}
                				
                			});;
                		}
                	}
                }

                return rowView;
			}

			@Override
			public boolean isChildSelectable(int groupPosition,
					int childPosition) {
				// TODO Auto-generated method stub
				return true;
			}
    }

    private class DrawerMenuItem {
            final boolean connectionRequired;
            final int icon;
            final int id;
            final int text;
            final String textStr;
            List<DrawerMenuItem> children = null;

            public DrawerMenuItem(int id, String text, int icon) {
                this.id = id;
                this.text = 0;
                this.icon = icon;
                this.connectionRequired = false;
                this.textStr = text;
            }
            
            public DrawerMenuItem(int id, int text, int icon) {
                    this(id, text, icon, false);
            }

            public DrawerMenuItem(int id, int text, int icon, boolean connectionRequired) {
                    this.id = id;
                    this.text = text;
                    this.icon = icon;
                    this.connectionRequired = connectionRequired;
                    this.textStr = null;
            }
            
            public List<DrawerMenuItem> getChildren() {
            	return children;
            }
            
            public void addChild(DrawerMenuItem item) {
            	if (children == null)
            		children = new ArrayList<DrawerMenuItem>();
            	children.add(item);
            }
            
            public void removeChild(DrawerMenuItem item) {
            	if (children == null)
            		return;
            	children.remove(item);
            }
            
            public int size() {
            	return children == null ? 0 : children.size();
            }
    }
    
    private class AccountDrawerMenuItem extends DrawerMenuItem {

    	final int viewId;
    	
		public AccountDrawerMenuItem(int id, String text, int icon, int viewId) {
			super(id, text, icon);
			this.viewId = viewId;
		}
    	
    }
	
//	private ServiceConnection messengerConnection = new ServiceConnection() {
//
//		@Override
//		public void onServiceConnected(ComponentName className, IBinder service) {
//			MainActivity.this.messenger = new Messenger(service);
//		}
//
//		@Override
//		public void onServiceDisconnected(ComponentName className) {
//			messenger = null;
//		}
//		
//	};    

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
	
	public static final String NEW_MESSAGE_ACTION = "org.tigase.messenger.phone.pro.NEW_MESSAGE_ACTION";
	
	public static final int SELECT_FOR_SHARE = 2;
	
	protected DrawerLayout drawerLayout;
	protected ExpandableListView drawerList;
	protected DrawerMenuItem accountsDrawerItem;
	protected ActionBarDrawerToggle drawerToggle;
	
	private Messenger messenger;
	private IJaxmppService jaxmppService;
	
	private MainActivityHelper helper = MainActivityHelper.createInstance(this);

	private BroadcastReceiver mucRoomJoinedReceiver;
	
	private boolean showMainWindowTabs = false;
//	@Override
//	public void onBackPressed() {
//	  super.onBackPressed();
//	  //fragmentChanged();
//	}

	private SharedPreferences prefs;
	private SharedPreferences.OnSharedPreferenceChangeListener prefsChanged;
	
	public IJaxmppService getJaxmppService() {
		return jaxmppService;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Debug.startMethodTracing("tigase-MainActivity");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		this.drawerList = (ExpandableListView) findViewById(R.id.left_drawer);
		drawerList.setGroupIndicator(null);
		this.drawerLayout = (DrawerLayout) findViewById(R.id.main_activity);
		
        // creating list of items available in drawer menu
        final List<DrawerMenuItem> drawerMenuItems = new ArrayList<DrawerMenuItem>();
        accountsDrawerItem = new DrawerMenuItem(R.id.accounts_flipper, "Accounts", R.drawable.ic_menu_account_list);
        
        AccountManager am = AccountManager.get(this);
        for (Account account : am.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)) {
        	accountsDrawerItem.addChild(new AccountDrawerMenuItem(account.hashCode(), account.name, R.drawable.user_avatar, R.id.main_left_drawer_item_account));
        }
        drawerMenuItems.add(accountsDrawerItem);
        
        this.prefs = Preferences.getDefaultSharedPreferences(this);
        showMainWindowTabs = prefs.getBoolean(Preferences.MAIN_WINDOW_TABS, true);
        if (!showMainWindowTabs) {
        	drawerMenuItems.add(new DrawerMenuItem(R.id.rosterList, R.string.contacts, R.drawable.ic_menu_allfriends));
        }
//        drawerMenuItems.add(new DrawerMenuItem(R.id.accountsList, R.string.accounts, R.drawable.ic_menu_account_list));
//        drawerMenuItems.add(new DrawerMenuItem(R.id.joinMucRoom, R.string.join_muc_room, R.drawable.group_chat, true));
        drawerMenuItems.add(new DrawerMenuItem(R.id.bookmarksList, R.string.bookmarks, android.R.drawable.star_off, true));
        drawerMenuItems.add(new DrawerMenuItem(R.id.action_settings, R.string.propertiesButton,
                        android.R.drawable.ic_menu_preferences));
//        drawerMenuItems.add(new DrawerMenuItem(R.id.aboutButton, R.string.aboutButton, android.R.drawable.ic_menu_info_details));
        drawerMenuItems.add(new DrawerMenuItem(R.id.appNameText, R.string.app_name, android.R.drawable.ic_menu_info_details));

        this.drawerList.setAdapter(new DrawerMenuAdapter(this.getApplicationContext(), R.layout.main_left_drawer_item,
                        drawerMenuItems));
        this.drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.accept,
                        R.string.accept);
	
        drawerLayout.setDrawerListener(this.drawerToggle);
//        this.drawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
//                @Override
//                public void onItemClick(AdapterView parent, View view, int position, long id) {
//                		ExpandableListAdapter adapter = (ExpandableListAdapter) parent;
//                		long pos = drawerList.getExpandableListPosition(position);
//                		int group = drawerList.getPackedPositionGroup(pos);
//                		int child = drawerList.getPackedPositionChild(pos);
//                        DrawerMenuItem item = (DrawerMenuItem) (child == -1 ? adapter.getGroup(group) : adapter.getChild(group, child));
//                        if (item != null) {
//                                drawerLayout.closeDrawers();
//                                onOptionsItemSelected(item.id);
//                        }
//                }
//        });		

        this.drawerList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v,
					int groupPosition, long id) {
                DrawerMenuItem item = (DrawerMenuItem) parent.getExpandableListAdapter().getGroup(groupPosition);
                if (item != null && item != accountsDrawerItem) {
                        drawerLayout.closeDrawers();
                        onOptionsItemSelected(item.id);
                }
                return false;
			}
        });		
        
        //getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new RosterFragment()).commit();
        if (savedInstanceState == null) {
        	// we need to add this fragment only if we are not being restored from previous state
        	// as in other case we might end up with overlapping fragments
        	if (showMainWindowTabs) {
        		getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new MainTabsFragment(), MainTabsFragment.FRAG_TAG).commit();
        	} else {
        		getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new ChatsListFragment(), ChatsListFragment.FRAG_TAG).commit();
        	}
        }
        
		//startService(new Intent(this, JaxmppService.class));
		Intent intent = new Intent(this, JaxmppService.class);
		intent.putExtra("ID", "AIDL");
		bindService(intent, jaxmppServiceConnection, Context.BIND_AUTO_CREATE);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction("org.tigase.messenger.phone.pro.MUC_ROOM_JOINED");
		mucRoomJoinedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final Bundle arguments = intent.getExtras();
				drawerLayout.post(new Runnable() {
					@Override
					public void run() {
						MucRoomFragment fragment = new MucRoomFragment();
						fragment.setArguments(arguments);
						switchFragments(fragment, "muc-room");						
					}				
				});
			}
			
		};
		this.registerReceiver(mucRoomJoinedReceiver, filter);
		
		this.prefsChanged = new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				if (jaxmppService != null) {
					try {
						jaxmppService.preferenceChanged(key);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}		
		};
		this.prefs.registerOnSharedPreferenceChangeListener(prefsChanged);
		
//		helper.updateActionBar();        
	}
	
	public void onDestroy() {
		super.onDestroy();
		this.prefs.unregisterOnSharedPreferenceChangeListener(prefsChanged);
		this.unregisterReceiver(mucRoomJoinedReceiver);
		unbindService(jaxmppServiceConnection);
		Debug.stopMethodTracing();
	}

	public void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		processNotificationIntent(intent);
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
		if (itemId == R.id.rosterList) {
			Fragment frag = new RosterFragment();
			switchFragments(frag, RosterFragment.FRAG_TAG);
		}
		else if (itemId == R.id.bookmarksList) {
			Fragment frag = new BookmarksFragment();
			switchFragments(frag, BookmarksFragment.FRAG_TAG);
		}
		else if (itemId == R.id.action_settings) {
			Intent intent = new Intent().setClass(this, MessengerPreferenceActivity.class);
			this.startActivityForResult(intent, 0);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Intent intent = new Intent();
		intent.setAction(JaxmppService.CLIENT_FOCUS);
		intent.putExtra("focus", false);
		sendBroadcast(intent);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Intent intent = new Intent();
		intent.setAction(JaxmppService.CLIENT_FOCUS);
		intent.putExtra("focus", true);
		sendBroadcast(intent);		
	}

	@Override
	public void onRosterItemClicked(String action, String account, BareJID jid) {
		//openChat(account, JID.jidInstance(jid));
		if (action == null) {
			Bundle args = new Bundle();
			args.putString("account", account);
			args.putString("jid", jid.toString());
			ContactFragment frag = new ContactFragment();
			frag.setArguments(args);
			switchFragments(frag, ContactFragment.FRAG_TAG);
		}
		else if ("chat".equals(action)) {
			openChat(account, JID.jidInstance(jid));
		}
	}
	
	public void openChat(String account, JID jid) {
		try {
			// creating chat
			jaxmppService.openChat(account, jid.toString());
		
			// back to chat list fragment
			//Fragment frag = getSupportFragmentManager().findFragmentByTag(RosterFragment.FRAG_TAG);
			//getSupportFragmentManager().beginTransaction().remove(frag).commit();
			getSupportFragmentManager().popBackStack(RosterFragment.FRAG_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
						
			// opening activity
//			Intent intent = new Intent(this, ChatActivity.class);
//			intent.putExtra("account", account);
//			intent.putExtra("recipient", jid.toString());
//			startActivity(intent);			
			
			Bundle arguments = new Bundle();
			arguments.putString("account", account);
			arguments.putString("recipient", jid.toString());
			Fragment chatFragment = new ChatHistoryFragment();
			chatFragment.setArguments(arguments);
			switchFragments(chatFragment, ChatHistoryFragment.FRAG_TAG);
			//fragmentChanged(chatFragment);

		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
	}
	
	public boolean isMainView() {
		Fragment frag = getSupportFragmentManager().findFragmentByTag(showMainWindowTabs ? MainTabsFragment.FRAG_TAG : ChatsListFragment.FRAG_TAG);
		return (frag != null && !frag.isHidden() && !frag.isDetached() && frag.isMenuVisible() && !frag.isRemoving());// && frag.isInLayout());
	}
	
	public void fragmentChanged(Fragment frag) {
		helper.updateActionBar(frag);
	}
	
	public void switchFragments(Fragment newFragment, String tag) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		switchFragments(newFragment, tag, ft);
	}
	
	public void switchFragments(Fragment newFragment, String tag, FragmentTransaction ft) {
		//ft.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left);
		ft.setCustomAnimations(R.animator.slide_in_left, R.animator.slide_out_right, R.animator.slide_out_left, R.animator.slide_in_right);
		
		ft.replace(R.id.content_frame, newFragment, tag);
		ft.addToBackStack(RosterFragment.FRAG_TAG);
		ft.commit();		
	}
	
	private void processNotificationIntent(final Intent intent) {
		if (intent == null)
			return;
		
		drawerList.post(new Runnable() {
			@Override
			public void run() {
				if (NEW_MESSAGE_ACTION.equals(intent.getAction())) {
					String type = intent.getStringExtra("type");
					if ("chat".equals(type)) {
						Fragment chatFragment = new ChatHistoryFragment();
						chatFragment.setArguments(intent.getExtras());
						switchFragments(chatFragment, ChatHistoryFragment.FRAG_TAG);	
						return;
					}
					else if ("muc".equals(type)) {
						Fragment mucFragment = new MucRoomFragment();
						mucFragment.setArguments(intent.getExtras());
						switchFragments(mucFragment, MucRoomFragment.FRAG_TAG);
						return;
					}
				}
			}
		});
	}
}
