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
package org.tigase.messenger.phone.pro;

import android.annotation.TargetApi;
import android.view.MenuItem;

@TargetApi(11)
public class MainActivityHelperHoneycomb extends MainActivityHelper {

	protected static final String TAG = "MainActivityHelperHoneycomb";

	protected MainActivityHelperHoneycomb(MainActivity activity) {
		super(activity);
	}

	@Override
	public void invalidateOptionsMenu() {
		activity.invalidateOptionsMenu();
	}

	@Override
	public void setShowAsAction(MenuItem item, int actionEnum) {
		item.setShowAsAction(actionEnum);
	}

	// Moved to MainActivityHelper and adjusted to use ActionBar from support-v7 library
//	@Override
//	public void updateActionBar(Fragment frag) {
//
//		boolean isMain = activity.isMainView();
//
//		Log.v(TAG, "updating ActionBar - isMain: " + isMain);
//
//		ActionBar actionBar = activity.getActionBar();
//		actionBar.setDisplayHomeAsUpEnabled(true);// isMain);
//		if (isMain) {
//			activity.drawerLayout.setDrawerListener(activity.drawerToggle);
//		} else {
//			activity.drawerLayout.setDrawerListener(null);
//		}
//
//		// // if (currentPage != 1 && !isXLarge()) {
//		// activity.drawerLayout.setDrawerListener(null);
//		// activity.drawerToggle.setDrawerIndicatorEnabled(false);
//		// } else {
//		// activity.drawerLayout.setDrawerListener(activity.drawerToggle);
//		activity.drawerToggle.setDrawerIndicatorEnabled(isMain);
//		// }
//		actionBar.setBackgroundDrawable(activity.getResources().getDrawable(
//				R.drawable.actionbar_background));
//
//		if (frag instanceof CustomHeader) {
//			CustomHeader cheader = (CustomHeader) frag;
//			View cview = actionBar.getCustomView();
//			actionBar.setDisplayShowCustomEnabled(true);
//			int id = cheader.getHeaderViewId();
//			if (cview == null || cview.getId() != id) {
//				actionBar.setCustomView(id);
//				cview = actionBar.getCustomView();
//			}
//			cheader.updateHeaderView(cview);
//		} else {
//			actionBar.setDisplayShowCustomEnabled(false);
//		}
//	}

	@Override
	public void updateActionBar(int itemHashCode) {
//		List<ChatWrapper> chats = activity.getChatList();
//		for (int i = 0; i < chats.size(); i++) {
//			ChatWrapper chat = chats.get(i);
//			if (chat.hashCode() == itemHashCode) {
//				updateActionBar();
//				return;
//			}
//		}
	}
}
