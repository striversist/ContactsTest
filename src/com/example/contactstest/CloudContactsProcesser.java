package com.example.contactstest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import com.example.contactstest.data.CloudContact;
import com.example.contactstest.data.CloudContact.PhoneNumber;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;

public class CloudContactsProcesser {

	private Context mContext;
	
	public CloudContactsProcesser(Context context) {
		assert (context != null);
		mContext = context;
	}
	
	public void runTest() {
		getContactsCount();
		getAllContacts();
		HashMap<Long, CloudContact> recentContacts = getRecentContacts(10);
		List<Long> contactIdList = new ArrayList<Long>();
		for (CloudContact cloudContact : recentContacts.values()) {
			contactIdList.add(cloudContact.getId());
		}
		getPhotos(contactIdList, false);
	}
	
	private void checkInitialized() {
        if (mContext == null) {
        	throw new IllegalStateException("context is null");
        }
    }
	
	public HashMap<Long, CloudContact> getContacts(int startPos, int endPos) {
		if (startPos > endPos)
			return null;
		return getContacts(startPos, endPos - startPos + 1, "sort_key_alt asc");
	}
	
	public int getContactsCount() {
		checkInitialized();
		ContentResolver resolver = mContext.getContentResolver();
		Cursor cursor = resolver.query(Contacts.CONTENT_URI, 
				new String[]{Contacts._ID}, null, null, null);
		if (cursor == null)
			return 0;
		
		int count = cursor.getCount();
		cursor.close();
		return count;
	}
	
	private HashMap<Long, CloudContact> getAllContacts() {
		return getContacts(0, 0, "sort_key_alt asc");	// 名字中英混排，升序
	}
	
	private HashMap<Long, CloudContact> getRecentContacts(int num) {
		return getContacts(0, num, Contacts.LAST_TIME_CONTACTED + " desc");		// 最后联系时间，降序
	}
	
	/**
	 * 获取从startPos开始，num个联系人信息
	 * @param startPos
	 * @param num 获取个数；若为0，则获取从startPos开始至结束的所有联系人信息
	 * @return
	 */
	private HashMap<Long, CloudContact> getContacts(int startPos, int num, String orderBy) {
		checkInitialized();
		if (startPos < 0 || num < 0)
			return null;
		
		LinkedHashMap<Long, CloudContact> contacts = new LinkedHashMap<Long, CloudContact>();
		ContentResolver resolver = mContext.getContentResolver();
		Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, 
				new String[]{Contacts._ID, Contacts.DISPLAY_NAME, Contacts.PHOTO_ID,
					Contacts.TIMES_CONTACTED, Contacts.LAST_TIME_CONTACTED},
				null, null, orderBy);
		if (cursor == null) {
			return null;
		}
		
		try {
			if (cursor.getCount() == 0)	{ // 没有联系人信息
				return contacts;
			}
			
			if (startPos >= cursor.getCount())
				return null;
			
			if (!cursor.moveToPosition(startPos))
				return null;
			
			do {
				CloudContact contact = new CloudContact();
				contact.setId(cursor.getLong(cursor.getColumnIndex(Contacts._ID)));
				contact.setName(cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME)));
				contact.setPhotoId(cursor.getLong(cursor.getColumnIndex(Contacts.PHOTO_ID)));
				contact.setTimesContacted(cursor.getInt(cursor.getColumnIndex(Contacts.TIMES_CONTACTED)));
				contact.setLastTimeContacted(cursor.getLong(cursor.getColumnIndex(Contacts.LAST_TIME_CONTACTED)));
				contacts.put(contact.getId(), contact);
			} while (cursor.moveToNext() && (contacts.size() < num || num == 0));
			
			// TODO: 这里可以加where做优化
			Cursor phoneCursor = resolver.query(CommonDataKinds.Phone.CONTENT_URI,
					new String[]{Phone.CONTACT_ID, Phone.NUMBER, Phone.TYPE, Phone.LABEL}, null, null, null);
			while (phoneCursor.moveToNext()) {
				long id = phoneCursor.getLong(phoneCursor.getColumnIndex(Phone.CONTACT_ID));
				if (contacts.containsKey(id)) {
					PhoneNumber phoneNumber = new PhoneNumber();
					phoneNumber.number = phoneCursor.getString(phoneCursor.getColumnIndex(Phone.NUMBER));
					phoneNumber.type = phoneCursor.getInt(phoneCursor.getColumnIndex(Phone.TYPE));
					phoneNumber.label = phoneCursor.getString(phoneCursor.getColumnIndex(Phone.LABEL));
					contacts.get(id).addPhoneNumber(phoneNumber);
				}
			}
		} finally {
			cursor.close();
		}
		
		return contacts;
	}
	
	public HashMap<Long, Bitmap> getPhotos(List<Long> contactIdList, boolean preferHighres) {
		checkInitialized();
		if (contactIdList == null)
			return null;
		
		HashMap<Long, Long> photoIds = getPhotoIds(contactIdList);
		if (photoIds == null)
			return null;
		
		HashMap<Long, Bitmap> photos = new HashMap<Long, Bitmap>();
		Iterator<Entry<Long, Long>> iter = photoIds.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Long, Long> entry = iter.next();
			Bitmap bitmap = getPhotoThumbnailBitmap(entry.getKey(), entry.getValue());
			photos.put(entry.getKey(), bitmap);
		}
		
		return photos;
	}
	
	private HashMap<Long, Long> getPhotoIds(List<Long> contactIdList) {
		checkInitialized();
		if (contactIdList == null)
			return null;
		
		HashMap<Long, Long> photoIdMap = new HashMap<Long, Long>();
		if (contactIdList.isEmpty()) {
			return photoIdMap;
		}
		
		ContentResolver resolver = mContext.getContentResolver();
		String where = CloudContactUtils.joinWhere(Contacts._ID, contactIdList);
		Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, 
				new String[]{Contacts._ID, Contacts.PHOTO_ID},
				where, null, null);
		if (cursor == null) {
			return null;
		}
		
		while (cursor.moveToNext()) {
			long id = cursor.getLong(cursor.getColumnIndex(Contacts._ID));
			long photoId = cursor.getLong(cursor.getColumnIndex(Contacts.PHOTO_ID));
			photoIdMap.put(id, photoId);
		}
		
		return photoIdMap;
	}
	
	private Bitmap getPhotoThumbnailBitmap(long contactId, long photoId) {
		checkInitialized();
		Bitmap contactPhoto = null;
		if (photoId > 0) {
			Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
			InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(mContext.getContentResolver(), uri);
			if (input != null) {
				contactPhoto = BitmapFactory.decodeStream(input);
			}
		}
		return contactPhoto;
	}
	
	/**
	 * 根据联系人id获取联系人信息
	 * @param idList
	 * @return
	 */
	private HashMap<Long, CloudContact> getContactsById(List<Long> contactIdList) {
		if (contactIdList == null)
			return null;
		
		LinkedHashMap<Long, CloudContact> contacts = new LinkedHashMap<Long, CloudContact>();
		if (contactIdList.isEmpty()) {
			return contacts;
		}
		
		ContentResolver resolver = mContext.getContentResolver();
		String where = CloudContactUtils.joinWhere(Contacts._ID, contactIdList);
		Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, 
				new String[]{Contacts._ID, Contacts.DISPLAY_NAME, Contacts.PHOTO_ID,
					Contacts.TIMES_CONTACTED, Contacts.LAST_TIME_CONTACTED},
				where, null, null);
		if (cursor == null)
			return null;
		
		try {
			while (cursor.moveToNext()) {
				CloudContact contact = new CloudContact();
				contact.setId(cursor.getLong(cursor.getColumnIndex(Contacts._ID)));
				contact.setName(cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME)));
				contact.setPhotoId(cursor.getLong(cursor.getColumnIndex(Contacts.PHOTO_ID)));
				contact.setTimesContacted(cursor.getInt(cursor.getColumnIndex(Contacts.TIMES_CONTACTED)));
				contact.setLastTimeContacted(cursor.getLong(cursor.getColumnIndex(Contacts.LAST_TIME_CONTACTED)));
				contacts.put(contact.getId(), contact);
			}
			
			String phoneWhere = CloudContactUtils.joinWhere(Phone.CONTACT_ID, contactIdList);
			Cursor phoneCursor = resolver.query(CommonDataKinds.Phone.CONTENT_URI,
					new String[]{Phone.CONTACT_ID, Phone.NUMBER, Phone.TYPE, Phone.LABEL}, phoneWhere, null, null);
			while (phoneCursor.moveToNext()) {
				long id = phoneCursor.getLong(phoneCursor.getColumnIndex(Phone.CONTACT_ID));
				if (contacts.containsKey(id)) {
					PhoneNumber phoneNumber = new PhoneNumber();
					phoneNumber.number = phoneCursor.getString(phoneCursor.getColumnIndex(Phone.NUMBER));
					phoneNumber.type = phoneCursor.getInt(phoneCursor.getColumnIndex(Phone.TYPE));
					phoneNumber.label = phoneCursor.getString(phoneCursor.getColumnIndex(Phone.LABEL));
					contacts.get(id).addPhoneNumber(phoneNumber);
				}
			}
		} finally {
			cursor.close();
		}
		
		return contacts;
	}
	
	/**
	 * 通过电话号码获取联系人信息
	 * @param numberList
	 * @return HashMap: key-电话号码
	 */
	public HashMap<String, CloudContact> getContactsByNumber(List<String> numberList) {
		checkInitialized();
		if (numberList == null)
			return null;
		
		LinkedHashMap<String, CloudContact> contacts = new LinkedHashMap<String, CloudContact>();
		if (numberList.isEmpty()) {
			return contacts;
		}
		
		ContentResolver resolver = mContext.getContentResolver();
		String where = CloudContactUtils.joinWhere(Phone.NUMBER, numberList);
		Cursor cursor = resolver.query(Phone.CONTENT_URI, 
				new String[]{Phone.CONTACT_ID, Phone.NUMBER},
				where, null, null);
		if (cursor == null) {
			return null;
		}
		
		List<Long> contactIdList = new ArrayList<Long>();
		try {
			while (cursor.moveToNext()) {
				Long contactId = cursor.getLong(cursor.getColumnIndex(Phone.CONTACT_ID));
				contactIdList.add(contactId);
			}
		} finally {
			cursor.close();
		}

		HashMap<Long, CloudContact> detailContacts = getContactsById(contactIdList);
		if (detailContacts == null) {
			return contacts;
		}
		
		for (String number : numberList) {
			for (CloudContact contact : detailContacts.values()) {
				if (contact.hasNumber(number)) {
					contacts.put(number, contact);
				}
			}
		}
		
		return contacts;
	}
}
