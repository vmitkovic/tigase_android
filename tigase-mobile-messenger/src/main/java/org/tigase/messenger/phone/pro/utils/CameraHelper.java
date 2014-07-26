/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package org.tigase.messenger.phone.pro.utils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;

public class CameraHelper {

	public static File createImageFile() throws IOException {
		// Create an image file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String imageFileName = "JPEG_" + timeStamp + "_";
	    File storageDir = Environment.getExternalStoragePublicDirectory(
	            Environment.DIRECTORY_PICTURES);
	    File image = File.createTempFile(
	        imageFileName,  /* prefix */
	        ".jpg",         /* suffix */
	        storageDir      /* directory */
	    );

	    return image;		
	}
	
	public static File takePhoto(Activity activity, Fragment fragment, int actionForResult) {
        File capturedPhotoFile = null;
	    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    // Ensure that there's a camera activity to handle the intent
	    if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
	        // Create the File where the photo should go
	        try {
	        	capturedPhotoFile = createImageFile();
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	        // Continue only if the File was successfully created
	        if (capturedPhotoFile != null) {
	            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
	                    Uri.fromFile(capturedPhotoFile));
	            if (fragment != null) {
	            	fragment.startActivityForResult(takePictureIntent, actionForResult);
	            } else {
	            	activity.startActivityForResult(takePictureIntent, actionForResult);
	            }
	        }
	    }
	    return capturedPhotoFile;
	}
	
	public static void takeVideo(Activity activity, Fragment fragment, int actionForResult) {
	    Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
	    if (takeVideoIntent.resolveActivity(activity.getPackageManager()) != null) {
	    	if (fragment != null) {
	    		fragment.startActivityForResult(takeVideoIntent, actionForResult);
	    	} else {
	    		activity.startActivityForResult(takeVideoIntent, actionForResult);
	    	}
	    }
	}
}
