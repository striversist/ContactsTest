package com.example.contactstest.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import com.example.contactstest.data.CloudContact;
import com.example.contactstest.data.CloudGroup;
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
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Pair;

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
		getGroups();
		getContactsIdInGroup(4);
		getContactsIdInGroup(5);
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
			if (phoneCursor != null) {
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
    			phoneCursor.close();
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
		
		try {
    		while (cursor.moveToNext()) {
    			long id = cursor.getLong(cursor.getColumnIndex(Contacts._ID));
    			long photoId = cursor.getLong(cursor.getColumnIndex(Contacts.PHOTO_ID));
    			photoIdMap.put(id, photoId);
    		}
		} finally {
		    cursor.close();
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
	
	@SuppressWarnings("unused")
    private CloudContact getContactById(Long contactId) {
	    if (contactId == null)
	        return null;
	    
	    List<Long> contactIdList = new ArrayList<Long>(1);
	    contactIdList.add(contactId);
	    HashMap<Long, CloudContact> contacts = getContactsById(contactIdList);
	    if (contacts == null)
	        return null;
	    
	    return contacts.get(contactId);
	}
	
	/**
	 * 根据联系人id获取联系人信息
	 * @param idList
	 * @return
	 */
	public HashMap<Long, CloudContact> getContactsById(List<Long> contactIdList) {
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
			if (phoneCursor != null) {
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
    			phoneCursor.close();
			}
		} finally {
			cursor.close();
		}
		
		return contacts;
	}
	
	/**
	 * 通过电话号码获取联系人信息
	 * @param numberList
	 * @return HashMap: key-电话号码 (
	 * FIXME: 目前只处理一个号码对应一个联系人的情况，将来解决
	 */
	public HashMap<String, CloudContact> getContactsByNumber(List<String> numberList) {
		checkInitialized();
		if (numberList == null)
			return null;
		
		LinkedHashMap<String, CloudContact> numberContacts = new LinkedHashMap<String, CloudContact>(); 
		if (numberList.isEmpty()) {
			return numberContacts;
		}
		
		ContentResolver resolver = mContext.getContentResolver();
		List<Pair<String, Long>> numberIdPairList = new ArrayList<Pair<String,Long>>();
		List<Long> contactIdList = new ArrayList<Long>();
		for (String number : numberList) {    // 注：这个number可能是normalized number，也可能是普通的number，所以需要通过PhoneLookup查询
		    Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		    Cursor cursor = resolver.query(uri, new String[]{PhoneLookup._ID}, null, null, null);
		    if (cursor == null)
		        continue;
		    
		    if (cursor.moveToFirst()) {   // FIXME: 目前只处理一个号码对应一个联系人的情况
		        Long contactId = cursor.getLong(cursor.getColumnIndex(PhoneLookup._ID));
		        numberIdPairList.add(new Pair<String, Long>(number, contactId));
		        if (!contactIdList.contains(contactId)) {     // 去重：有可能多个号码对应一个联系人
    		        contactIdList.add(contactId);
		        }
		    }
		    cursor.close();
		}
		
		HashMap<Long, CloudContact> contacts = getContactsById(contactIdList);
		if (contacts == null) {
		    return numberContacts;
		}
		for (int i=0; i<numberIdPairList.size(); ++i) {
		    String number = numberIdPairList.get(i).first;
		    Long contactId = numberIdPairList.get(i).second;
		    if (contacts.containsKey(contactId)) {
		        if (!numberContacts.containsKey(number)) {    // 不考虑一个号码对应多个联系人的情况，所以只取一个联系人记录
		            numberContacts.put(number, contacts.get(contactId));
		        }
		    }
		}
		
		return numberContacts;
	}
	
	public HashMap<Long, CloudGroup> getGroups() {
	    checkInitialized();
	    LinkedHashMap<Long, CloudGroup> groups = new LinkedHashMap<Long, CloudGroup>();
	    
	    ContentResolver resolver = mContext.getContentResolver();
	    Cursor cursor = resolver.query(ContactsContract.Groups.CONTENT_SUMMARY_URI, 
                new String[]{Groups._ID, Groups.TITLE, Groups.NOTES,
	            Groups.SYSTEM_ID, Groups.SUMMARY_COUNT},
                null, null, null);
        if (cursor == null) {
            return null;
        }
        
        try {
            if (cursor.getCount() == 0) { // 没有联系人信息
                return groups;
            }
            
            while (cursor.moveToNext()) {
                CloudGroup group = new CloudGroup();
                group.setId(cursor.getLong(cursor.getColumnIndex(Groups._ID)));
                group.setNotes(cursor.getString(cursor.getColumnIndex(Groups.NOTES)));
                group.setSummaryCount(cursor.getInt(cursor.getColumnIndex(Groups.SUMMARY_COUNT)));
                group.setSystemId(cursor.getString(cursor.getColumnIndex(Groups.SYSTEM_ID)));
                group.setTitle(cursor.getString(cursor.getColumnIndex(Groups.TITLE)));
                groups.put(group.getId(), group);
            }
        } finally {
            cursor.close();
        }
	    
	    return groups;
	}
	
	public ArrayList<Long> getContactsIdInGroup(long groupId) {
	    checkInitialized();
	    if (groupId < 0)
	        return null;
	    
	    ArrayList<Long> contactsId = new ArrayList<Long>();
	    ContentResolver resolver = mContext.getContentResolver();
	    String where = Data.MIMETYPE + "=" + "'" + GroupMembership.CONTENT_ITEM_TYPE + "'" + 
	            " AND " + GroupMembership.GROUP_ROW_ID + "=" + String.valueOf(groupId);
	    Cursor cursor = resolver.query(Data.CONTENT_URI, new String[]{GroupMembership.CONTACT_ID, GroupMembership.GROUP_ROW_ID},
	            where, null, null);
	    if (cursor == null)
	        return contactsId;
	    
	    try {
	        while (cursor.moveToNext()) {
	            long contactId = cursor.getLong(cursor.getColumnIndex(GroupMembership.CONTACT_ID));
	            contactsId.add(contactId);
	        }
	    } finally {
	        cursor.close();
	    }
	    
	    return contactsId;
	}
}
