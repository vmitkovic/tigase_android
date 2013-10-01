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
package org.tigase.mobile;

import org.tigase.mobile.MultiJaxmpp.ChatWrapper;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.view.MenuItem;

public class TigaseMobileMessengerActivityHelper {

	private static boolean xlarge = false;
	
	public static TigaseMobileMessengerActivityHelper createInstance() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return new TigaseMobileMessengerActivityHelperHoneycomb();
		} else {
			return new TigaseMobileMessengerActivityHelper();
		}
	}

	protected TigaseMobileMessengerActivityHelper() {
	}

	public void invalidateOptionsMenu(Activity activity) {
	}

	public void updateIsXLarge(boolean isXLarge) {
		xlarge = isXLarge;
	}
	
	public boolean isXLarge() {
		return xlarge;
	};

	public void setShowAsAction(MenuItem item, int value) {
	};

	public void updateActionBar(Activity activity, final ChatWrapper c) {
	}
	
}
