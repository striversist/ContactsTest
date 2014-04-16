package com.example.contactstest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MainActivity extends Activity {
	private static final String[] PHONES_PROJECTION = new String[] {
			Phone.DISPLAY_NAME, Phone.NUMBER, Photo.PHOTO_ID, Phone.CONTACT_ID };

	private List<String> mContactsNameList = new ArrayList<String>();
	private List<String> mContactsNumberList = new ArrayList<String>();
	private List<Bitmap> mContactsPhotoList = new ArrayList<Bitmap>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

//		getContacts();
		test();
	}
	
	private void test() {
//		CloudContactsProcesser processer = new CloudContactsProcesser();
//		processer.setContext(getApplicationContext());
//		processer.runTest();
		
//		CloudCallsProcesser processer = new CloudCallsProcesser();
//		processer.setContext(getApplicationContext());
//		processer.runTest();
		
//		CloudSmsProcesser processer = new CloudSmsProcesser(getApplicationContext());
//		processer.runTest();
		
		CloudMixProcesser processer = new CloudMixProcesser(getApplicationContext());
		processer.runTest();
	}

	private void getContacts() {
		ContentResolver resolver = getContentResolver();

		// 获取手机联系人
		Cursor phoneCursor = resolver.query(Phone.CONTENT_URI,
				PHONES_PROJECTION, null, null, null);

		if (phoneCursor != null) {
			while (phoneCursor.moveToNext()) {

				int ContactsIdIndex = phoneCursor.getColumnIndex(Contacts._ID);
				int DisplayNameIndex = phoneCursor.getColumnIndex(Contacts.DISPLAY_NAME);
				int PhotoIdIndex = phoneCursor.getColumnIndex(Contacts.PHOTO_ID);
				
				
				// //得到手机号码
				// String phoneNumber =
				// phoneCursor.getString(phoneCursor.getColumnIndex(Contacts.));
				// //当手机号码为空的或者为空字段 跳过当前循环
				// if (TextUtils.isEmpty(phoneNumber))
				// continue;

				// 得到联系人ID
				Long contactid = (ContactsIdIndex != -1 ? phoneCursor.getLong(ContactsIdIndex) : null);
				
				// 联系人名称
				String contactName = (ContactsIdIndex != -1 ? phoneCursor.getString(DisplayNameIndex) : null);

				// 得到联系人头像ID
				Long photoid = (PhotoIdIndex != -1 ? phoneCursor.getLong(PhotoIdIndex) : null);

				// 得到联系人头像Bitamp
				Bitmap contactPhoto = null;
				// photoid 大于0 表示联系人有头像 如果没有给此人设置头像则给他一个默认的
				if (photoid > 0) {
					Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactid);
					InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uri);
					contactPhoto = BitmapFactory.decodeStream(input);
				} else {
					contactPhoto = BitmapFactory.decodeResource(getResources(),	R.drawable.ic_launcher);
				}

				mContactsNameList.add(contactName);
				// mContactsNumberList.add(phoneNumber);
				mContactsPhotoList.add(contactPhoto);
			}

			phoneCursor.close();
		}
	}
}
