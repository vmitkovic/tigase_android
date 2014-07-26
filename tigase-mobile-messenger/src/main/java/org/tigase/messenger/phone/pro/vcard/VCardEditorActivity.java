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
package org.tigase.messenger.phone.pro.vcard;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.tigase.messenger.phone.pro.IJaxmppService;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.account.AccountAuthenticator;
import org.tigase.messenger.phone.pro.db.VCardsCacheTableMetaData;
import org.tigase.messenger.phone.pro.db.providers.RosterProvider;
import org.tigase.messenger.phone.pro.roster.RosterUpdateCallback;
import org.tigase.messenger.phone.pro.service.AsyncXmppCallback;
import org.tigase.messenger.phone.pro.service.JaxmppService;
import org.tigase.messenger.phone.pro.utils.CameraHelper;

import tigase.jaxmpp.android.xml.ParcelableElement;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.Base64;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCard;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule.VCardAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.jaxmpp.j2se.connectors.socket.StreamListener;
import tigase.jaxmpp.j2se.connectors.socket.XMPPDomBuilderHandler;
import tigase.jaxmpp.j2se.xml.J2seElement;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class VCardEditorActivity extends Activity {

	private static final int ERROR_TOAST = 3;

	private static final int PICK_ACCOUNT = 2;
	private static final int PICK_IMAGE = 1;
	private static final int TAKE_PHOTO = 3;

	private static final int PUBLISHED_TOAST = 1;
	private static final String TAG = "VCardEditorActivity";
	private static final int TIMEOUT_TOAST = 2;
	
	abstract class AddressItem {
		abstract int type();

		abstract CharSequence getStreet();
		abstract CharSequence getLocality();
		abstract CharSequence getCountry();
		abstract CharSequence getPostalCode();
		abstract CharSequence getState();
		
		abstract void setStreet(String value);
		abstract void setLocality(String value);
		abstract void setCountry(String value);
		abstract void setPostalCode(String value);
		abstract void setState(String value);
		
		@Override
		public String toString() {
			String address = "";
			if (vcard != null) {
				if (getStreet() != null)
					address += getStreet();
				if (getPostalCode() != null && getPostalCode().length() > 0) {
					if (address.length() > 0)
						address += ", ";
					address += getPostalCode();
					if (getLocality() != null && getLocality().length() > 0) {
						address += " " + getLocality();
					}
				}
				else if (getLocality() != null && getLocality().length() > 0) {
					if (address.length() > 0)
						address += ", ";
					address += getLocality();
				}
				if (getCountry() != null && getCountry().length() > 0) {
					if (address.length() > 0)
						address += ", ";
					address += getCountry();
				}
			}
			return address;
		}
	}
	
	/**
	 * Fill activity for editing vcard from vcard instance
	 * 
	 * @param activity
	 * @param contentResolver
	 * @param resources
	 * @param jid
	 * @param vcard
	 */
	public void fillFields(final Activity activity, final ContentResolver contentResolver, final Resources resources,
			final JID jid, final VCard vcard) {
		((TextView) activity.findViewById(R.id.fullname)).setText(vcard.getFullName());
		((TextView) activity.findViewById(R.id.nickname)).setText(vcard.getNickName());
		((TextView) activity.findViewById(R.id.birthday)).setText(vcard.getBday());
		((TextView) activity.findViewById(R.id.email)).setText(vcard.getHomeEmail());
		((TextView) activity.findViewById(R.id.homeTel)).setText(vcard.getHomeTelVoice());
		((TextView) activity.findViewById(R.id.workTel)).setText(vcard.getWorkTelVoice());
		((TextView) activity.findViewById(R.id.homepage)).setText(vcard.getUrl());

		ImageView avatar = (ImageView) activity.findViewById(R.id.avatarButton);
		Bitmap bmp;
		try {
			if (vcard.getPhotoVal() != null && vcard.getPhotoVal().length() > 0) {
				String val = vcard.getPhotoVal();
				byte[] buffer = Base64.decode(val);

				bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
				((VCardEditorActivity) activity).bitmap = bmp;
				ContentValues values = new ContentValues();
				values.put(VCardsCacheTableMetaData.FIELD_DATA, buffer);
				contentResolver.insert(Uri.parse(RosterProvider.VCARD_URI + "/" + Uri.encode(jid.getBareJid().toString())),
						values);
			} else {
				bmp = null;
			}
		} catch (Exception e) {
			Log.e("tigase", "WTF?", e);
			bmp = null;
		}

		Bitmap x = BitmapFactory.decodeResource(resources, R.drawable.user_avatar);
		if (bmp != null) {
			x = bmp;
		}
		avatar.setImageBitmap(x);
		this.vcard = vcard;

		addressAdapter.notifyDataSetChanged();
	}

	private OnItemClickListener addressOnClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Log.v(TAG, "item clicked = " + view.getId());
			AddressItem item = addressAdapter.getItem(position);
			createAddressDialog(item);
		}
		
	};
	
	private ArrayAdapter<AddressItem> addressAdapter = null; 
	private ImageView avatar;

	private Bitmap bitmap = null;
	private File capturedPhotoFile = null;
	
	private IJaxmppService jaxmppService = null;
	private ServiceConnection jaxmppServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			jaxmppService = IJaxmppService.Stub.asInterface(service);
			if (jid != null)
				setAccountJid(jid);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			jaxmppService = null;
		}
		
	};
	
	private JID jid;

	private VCard vcard;

	/**
	 * Serialize bitmap instance to byte array encoded in PNG format
	 * 
	 * @param bmp
	 * @return
	 */
	private byte[] bitmapToByteArray(Bitmap bmp) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.PNG, 0 /* ignored for PNG */, bos);
		return bos.toByteArray();
	}

	/**
	 * Starts activity to select picture for avatar
	 */
	private void chooseAvatar() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, "Select picture"), PICK_IMAGE);
	}

	private void createAddressDialog(final AddressItem item) {
		final View view = this.getLayoutInflater().inflate(R.layout.vcard_address, null);
		((TextView) view.findViewById(R.id.street)).setText(item.getStreet());
		((TextView) view.findViewById(R.id.locality)).setText(item.getLocality());
		((TextView) view.findViewById(R.id.country)).setText(item.getCountry());
		((TextView) view.findViewById(R.id.postalCode)).setText(item.getPostalCode());
		((TextView) view.findViewById(R.id.state)).setText(item.getState());
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
			.setTitle(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(item.type()))
			.setView(view).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					item.setStreet(((TextView) view.findViewById(R.id.street)).getText().toString());
					item.setLocality(((TextView) view.findViewById(R.id.locality)).getText().toString());
					item.setCountry(((TextView) view.findViewById(R.id.country)).getText().toString());
					item.setPostalCode(((TextView) view.findViewById(R.id.postalCode)).getText().toString());
					item.setState(((TextView) view.findViewById(R.id.state)).getText().toString());
					fillFields(VCardEditorActivity.this, getContentResolver(), getResources(), jid, vcard);
				}		
			});
		builder.create().show();
	}
	
	/**
	 * Create progress dialog based on id of resource string
	 * 
	 * @param resourceString
	 * @return
	 */
	private ProgressDialog createProgress(int resourceString) {
		final ProgressDialog dialog = ProgressDialog.show(VCardEditorActivity.this, "",
				getResources().getString(resourceString), true);
		dialog.setCancelable(true);
		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				Intent result = new Intent();
				setResult(Activity.RESULT_CANCELED, result);
				finish();
			}
		});
		return dialog;
	}

	/**
	 * Starts requesting vcard
	 */
	private void downloadVCard() {
		final ProgressDialog dialog = createProgress(R.string.vcard_retrieving);
		final TextView fullName = (TextView) findViewById(R.id.fullname);
		new Thread() {
			public void run() {
		try {
			jaxmppService.retrieveVCard(jid.getBareJid().toString(), jid.getBareJid().toString(), new AsyncXmppCallback(new VCardModule.VCardAsyncCallback() {
				
				@Override
				public void onTimeout() throws JaxmppException {
					dialog.dismiss();
					showToast(TIMEOUT_TOAST);	
				}
				
				@Override
				public void onError(Stanza responseStanza, ErrorCondition error)
						throws JaxmppException {
					dialog.dismiss();
					showToast(ERROR_TOAST);	
				}
				
				@Override
				protected void onVCardReceived(final VCard vcard) throws XMLException {
					fullName.post(new Runnable() {
						@Override
						public void run() {
							dialog.dismiss();
							fillFields(VCardEditorActivity.this, getContentResolver(), getResources(), jid, vcard);
						}
					});
				}
			}));
			dialog.show();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			showToast(ERROR_TOAST);	
		}
		}
		}.start();
	}

	/**
	 * Returns image loaded from file and scaled to 128
	 * 
	 * @param path
	 *            - path to image
	 * @return scaled image
	 */
	private Bitmap getScaledImage(Uri uri) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, o);

			// The new size we want to scale to
			final int REQUIRED_SIZE = 128;

			// Find the correct scale value. It should be the power of 2.
			int scale = 1;
			while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE)
				scale *= 2;

			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, o2);
		} catch (FileNotFoundException e) {
		}
		return null;
	}

	@TargetApi(11)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_IMAGE && data != null && data.getData() != null) {
			Uri uri = data.getData();
			setAvatarFromUri(uri);
		} else if (requestCode == PICK_ACCOUNT) {
			if (data == null || data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) == null) {
				this.finish();
				return;
			}
			String accName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			setAccountJid(JID.jidInstance(accName));
			this.invalidateOptionsMenu();
		} else if (requestCode == TAKE_PHOTO) {
			if (capturedPhotoFile != null && capturedPhotoFile.exists()) {
				Uri uri = Uri.fromFile(capturedPhotoFile);
				setAvatarFromUri(uri);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vcard_editor);

		avatar = (ImageView) findViewById(R.id.avatarButton);
		avatar.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showAvatarActionDialog();
			}
		});
			
		addressAdapter = new ArrayAdapter<AddressItem>(this, R.layout.contact_fragment_item) {
			
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view;
				if (convertView == null) {
					view = VCardEditorActivity.this.getLayoutInflater().inflate(R.layout.contact_fragment_item, parent, false);
				} else {
					view = convertView;
				}		
				
				TextView value = (TextView) view.findViewById(R.id.value);
				TextView type = (TextView) view.findViewById(R.id.type);
				
				AddressItem item = getItem(position);
				value.setText(item.toString());
				type.setText(ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabelResource(item.type()));
				view.setId(item.type());
								
				return view;
			}
			
		};
		
		addressAdapter.add(new AddressItem() {
			int type() {
				return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME;
			}

			@Override
			String getStreet() {
				return vcard.getHomeAddressStreet();
			}

			@Override
			CharSequence getLocality() {
				return vcard.getHomeAddressLocality();
			}

			@Override
			CharSequence getCountry() {
				return vcard.getHomeAddressCtry();
			}

			@Override
			CharSequence getPostalCode() {
				return vcard.getHomeAddressPCode();
			}

			@Override
			CharSequence getState() {
				return vcard.getHomeAddressRegion();
			}

			@Override
			void setStreet(String value) {
				vcard.setHomeAddressStreet(value);
			}

			@Override
			void setLocality(String value) {
				vcard.setHomeAddressLocality(value);
			}

			@Override
			void setCountry(String value) {
				vcard.setHomeAddressCtry(value);
			}

			@Override
			void setPostalCode(String value) {
				vcard.setHomeAddressPCode(value);
			}

			@Override
			void setState(String value) {
				vcard.setHomeAddressRegion(value);
			}
		});
		addressAdapter.add(new AddressItem() {
			int type() {
				return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK;
			}
			
			@Override
			String getStreet() {
				return vcard.getWorkAddressStreet();
			}

			@Override
			CharSequence getLocality() {
				return vcard.getWorkAddressLocality();
			}

			@Override
			CharSequence getCountry() {
				return vcard.getWorkAddressCtry();
			}

			@Override
			CharSequence getPostalCode() {
				return vcard.getWorkAddressPCode();
			}

			@Override
			CharSequence getState() {
				return vcard.getWorkAddressRegion();
			}

			@Override
			void setStreet(String value) {
				vcard.setWorkAddressStreet(value);
			}

			@Override
			void setLocality(String value) {
				vcard.setWorkAddressLocality(value);
			}

			@Override
			void setCountry(String value) {
				vcard.setWorkAddressCtry(value);
			}

			@Override
			void setPostalCode(String value) {
				vcard.setHomeAddressPCode(value);
			}

			@Override
			void setState(String value) {
				vcard.setHomeAddressRegion(value);
			}
		});
		
		Intent intent = new Intent(this, JaxmppService.class);
		intent.putExtra("ID", "AIDL");
		bindService(intent, jaxmppServiceConnection, Context.BIND_AUTO_CREATE);

		JID jid = null;
		Account account = (Account) getIntent().getParcelableExtra("account");
		Log.v(TAG, "got intent = " + getIntent());
		if (account != null) {
			jid = JID.jidInstance(account.name);
		} else if (getIntent().hasExtra("account_jid")) {
			jid = JID.jidInstance(getIntent().getStringExtra("account_jid"));
		}

		if (jid == null) {
			startChooseAccountIceCream(account);
		} else {
			setAccountJid(jid);
		}
		
		((ListView) findViewById(R.id.addressListLayout)).setAdapter(addressAdapter);
		((ListView) findViewById(R.id.addressListLayout)).setOnItemClickListener(addressOnClickListener);
	}
	
	@Override
	protected void onDestroy() {
		unbindService(jaxmppServiceConnection);
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.vcard_editor_refresh) {
			Log.v(TAG, "downloading vcard");
			downloadVCard();
		} else if (item.getItemId() == R.id.vcard_editor_publish) {
			Log.v(TAG, "publishing vcard");
			try {
				publishVCard();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		menu.clear();
		if (jid != null && jaxmppService != null) {
			try {
				if (jaxmppService.isConnected(jid.getBareJid().toString())) {
					inflater.inflate(R.menu.vcard_editor_menu, menu);
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}

	/**
	 * Starts publishing vcard
	 * @throws RemoteException 
	 */
	private void publishVCard() throws RemoteException {
		if (jaxmppService.isConnected(jid.getBareJid().toString())) {
			String fullname = ((TextView) findViewById(R.id.fullname)).getText().toString();
			String nick = ((TextView) findViewById(R.id.nickname)).getText().toString();
			String bday = ((TextView) findViewById(R.id.birthday)).getText().toString();
			String email = ((TextView) findViewById(R.id.email)).getText().toString();
			String homeTel = ((TextView) findViewById(R.id.homeTel)).getText().toString();
			String workTel = ((TextView) findViewById(R.id.workTel)).getText().toString();
			String homepage = ((TextView) findViewById(R.id.homepage)).getText().toString();
			vcard.setFullName(fullname);
			vcard.setNickName(nick);
			vcard.setHomeEmail(email);
			vcard.setBday(bday);
			vcard.setHomeTelVoice(homeTel);
			vcard.setWorkTelVoice(workTel);
			vcard.setUrl(homepage);

			byte[] buffer = bitmap == null ? null : bitmapToByteArray(bitmap);
			if (buffer != null) {
				vcard.setPhotoVal(Base64.encode(buffer));
				vcard.setPhotoType("image/png");
			} else {
				vcard.setPhotoVal(null);
				vcard.setPhotoType(null);
			}

			final ProgressDialog dialog = createProgress(R.string.vcard_publishing);

			(new Thread() {
				@Override
				public void run() {

					try {
						jaxmppService.publishVCard(jid.getBareJid().toString(), ParcelableElement.fromElement(vcard.makeElement()),
								new AsyncXmppCallback(new AsyncCallback() {

							@Override
							public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
								dialog.dismiss();
								showToast(ERROR_TOAST);
							}

							@Override
							public void onSuccess(Stanza responseStanza) throws JaxmppException {
								dialog.dismiss();
								showToast(PUBLISHED_TOAST);
							}

							@Override
							public void onTimeout() throws JaxmppException {
								dialog.dismiss();
								showToast(TIMEOUT_TOAST);
							}

						}));
					} catch (Exception ex) {
						Log.v(TAG, "problems with publishing vcard", ex);
					}
				}
			}).start();

			dialog.show();
		}
	}

	protected void setAccountJid(JID jid_) {
		this.jid = jid_;

		if (jaxmppService == null)
			return;
		
		final Cursor cursor = getContentResolver().query(
				Uri.parse(RosterProvider.VCARD_URI + "/" + Uri.encode(jid.getBareJid().toString())), null, null, null, null);
		try {
			cursor.moveToNext();
			byte[] buffer = cursor.getBlob(cursor.getColumnIndex(VCardsCacheTableMetaData.FIELD_DATA));
			Bitmap bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
			avatar.setImageBitmap(bmp);
		} catch (Exception ex) {

		} finally {
			cursor.close();
		}

		downloadVCard();
		
		boolean enabled = false;
		try {
			enabled = jaxmppService.isConnected(jid.getBareJid().toString());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		((TextView) findViewById(R.id.fullname)).setEnabled(enabled);
		((TextView) findViewById(R.id.nickname)).setEnabled(enabled);
		((TextView) findViewById(R.id.birthday)).setEnabled(enabled);
		((TextView) findViewById(R.id.email)).setEnabled(enabled);
	}

	protected void showAvatarActionDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setItems(R.array.vcard_avatar_action,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							takePictureForAvatar();
							break;
						case 1:
						default:
							chooseAvatar();
							break;
						}
					}
				});
		builder.create().show();
	}
	
	/**
	 * Show toast based on type
	 * 
	 * @param type
	 *            - type of message to present
	 */
	protected void showToast(final int type) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Context context = getApplicationContext();
				CharSequence text = null;
				switch (type) {
				case PUBLISHED_TOAST:
					text = getResources().getString(R.string.vcard_published_toast);
					break;
				case TIMEOUT_TOAST:
					text = getResources().getString(R.string.vcard_timeout_toast);
					break;
				case ERROR_TOAST:
					text = getResources().getString(R.string.vcard_error_toast);
					break;
				}
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
			}
		});
	}

	@TargetApi(14)
	private void startChooseAccountIceCream(final Account account) {
		Intent intentChooser = AccountManager.newChooseAccountIntent(account, null, new String[] { AccountAuthenticator.ACCOUNT_TYPE },
				false, null, null, null, null);
		this.startActivityForResult(intentChooser, PICK_ACCOUNT);
	}
	
	private void setAvatarFromUri(Uri uri) {
		if (uri != null) {
			// Link to the image
			Bitmap bmp = getScaledImage(uri);
			avatar.setImageBitmap(bmp);

			byte[] buffer = bitmapToByteArray(bmp);
			bitmap = bmp;
			ContentValues values = new ContentValues();
			values.put(VCardsCacheTableMetaData.FIELD_DATA, buffer);
			getContentResolver().insert(
					Uri.parse(RosterProvider.VCARD_URI + "/" + Uri.encode(jid.getBareJid().toString())), values);
		}		
	}
	
	private void takePictureForAvatar() {
		capturedPhotoFile = CameraHelper.takePhoto(this, null, TAKE_PHOTO);
	}
}
