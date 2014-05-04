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

import android.content.res.Configuration;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.view.MenuItem;

public class MainActivityHelper {

	public static MainActivityHelper createInstance(MainActivity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return new MainActivityHelperHoneycomb(activity);
		} else {
			return new MainActivityHelper(activity);
		}
	}

	protected final MainActivity activity;

	protected MainActivityHelper(MainActivity activity) {
		this.activity = activity;
	}

	public void invalidateOptionsMenu() {
	}

	public boolean isXLarge() {
		return (activity.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4;
		// return getResources().getConfiguration().screenLayout >= 0x04 &&
		// Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	};

	public void setShowAsAction(MenuItem item, int value) {
	};

	public void updateActionBar(Fragment frag) {
	}

	public void updateActionBar(int itemHashCode) {
	}

}
