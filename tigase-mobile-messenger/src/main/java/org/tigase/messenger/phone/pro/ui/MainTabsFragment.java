package org.tigase.messenger.phone.pro.ui;

import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.chat.ChatsListFragment;
import org.tigase.messenger.phone.pro.roster.RosterFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainTabsFragment extends Fragment {

	public static final String FRAG_TAG = "MainTabsFragment";
	
	private PagerAdapter mAdapter;
	private ViewPager mPager;
	private TabListener tabListener;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.main_activity_tabs_fragment, container, false);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}
	
	@Override
	public void onResume() {
		super.onResume();		
		ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
		
        mAdapter = new FragmentPagerAdapter(this.getChildFragmentManager()) {

			@Override
			public Fragment getItem(int pos) {
				if (pos == 0) {
					return new ChatsListFragment();
				}
				else if (pos == 1) {
					return new RosterFragment();
				}
				else {
					return null;
				}
			}

			@Override
			public int getCount() {
				return 2;
			}
        	
        };
        
        mPager = (ViewPager) getView().findViewById(R.id.view_pager);
        mPager.setAdapter(mAdapter);
        
        tabListener = new ActionBar.TabListener() {
			
			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction arg1) {
				
			}
			
			@Override
			public void onTabSelected(Tab tab, FragmentTransaction arg1) {
				mPager.setCurrentItem(tab.getPosition());
			}
			
			@Override
			public void onTabReselected(Tab tab, FragmentTransaction arg1) {
				
			}
		};
        
        ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
        if (actionBar.getTabCount() == 0) {
        	actionBar.addTab(actionBar.newTab().setText("Chats").setTabListener(tabListener));
        	actionBar.addTab(actionBar.newTab().setText("Contacts").setTabListener(tabListener));
        }
        //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	}
	
}
