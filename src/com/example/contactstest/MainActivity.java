package com.example.contactstest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

import com.example.contactstest.core.CloudSmsProcesser;

public class MainActivity extends Activity {
    
    public static final String TAG = "ContactsTest";
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
//		test();
		testSimpleUriSetGet();
	}
	
	private void test() {
//		CloudContactsProcesser processer = new CloudContactsProcesser(getApplicationContext());
//		processer.runTest();
//		
//		CloudCallsProcesser processer = new CloudCallsProcesser(getApplicationContext());
//		processer.runTest();
//		
		CloudSmsProcesser processer = new CloudSmsProcesser(getApplicationContext());
		processer.runTest();
		
//		CloudMixProcesser processer = new CloudMixProcesser(getApplicationContext());
//		processer.runTest();
		
//		CloudSmsManager smsManager = new CloudSmsManager(getApplicationContext());
//		smsManager.runTest();
	    
//	    CloudCallManager callManager = new CloudCallManager(getApplicationContext());
//	    callManager.runTest();
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
	
	public void testContactPhoto() {
        long rawContactId = 557;
        String srcPhotoPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "photo2.jpg";
        String dstPhotoPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "photo3.jpg";
        
        File srcPhoto = new File(srcPhotoPath);
        new File(dstPhotoPath).delete();
        
        String md5_1 = RSAHelper.md5HexString(srcPhoto);
        byte[] data = PhotoHelper.convertFile2Bytes(srcPhoto);
        String md5_2 = RSAHelper.md5HexString(data);
        
//        PhotoHelper.setContactThumbnail(getApplicationContext(), rawContactId, data);
        PhotoHelper.setDisplayPhoto(getApplicationContext(), rawContactId, srcPhoto);
        
        final Uri outputUri = Uri.withAppendedPath(
                ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId),
                ContactsContract.RawContacts.DisplayPhoto.CONTENT_DIRECTORY);
        data = PhotoHelper.getUriData(getApplicationContext(), outputUri);
        String md5_3 = RSAHelper.md5HexString(data);
        PhotoHelper.convertBytes2File(data, dstPhotoPath);
        
        String msg = md5_1 + "\n" + md5_2 + "\n" + md5_3;
        Log.d(TAG, "" + msg);
    }
    
    public void testSimpleUriSetGet() {
        long rawContactId = 557;
        String srcPhotoPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "photo2.jpg";
        String dstPhotoPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "photo3.jpg";
        File srcPhotoFile = new File(srcPhotoPath);
        File dstPhotoFile = new File(dstPhotoPath);
        dstPhotoFile.delete();
        
        Uri inputUri = Uri.fromFile(srcPhotoFile);
        
        // 测试1. 使用文件Uri
//        final Uri outputUri = Uri.fromFile(new File(dstPhotoPath));
        
        // 测试2: 使用DisplayPhoto Uri
        final Uri outputUri = Uri.withAppendedPath(
                ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId),
                ContactsContract.RawContacts.DisplayPhoto.CONTENT_DIRECTORY);
                
        String md5_input = RSAHelper.md5HexString(srcPhotoFile);
        PhotoHelper.setUriData(getApplicationContext(), inputUri, outputUri);
        String md5_output = RSAHelper.md5HexString(PhotoHelper.getUriData(getApplicationContext(), outputUri));
        String msg = md5_input + "\n" + md5_output;
        Log.d(TAG, "" + msg);
        
        // Android源码做法：JPEG质量75压缩。得到的md5与以上DisplayPhoto Uri md5_output相同
        Bitmap srcBitmap = BitmapFactory.decodeFile(srcPhotoPath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        srcBitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
        String md5_output2 = RSAHelper.md5HexString(baos.toByteArray());
        Log.d(TAG, "" + md5_output2);
    }
}
