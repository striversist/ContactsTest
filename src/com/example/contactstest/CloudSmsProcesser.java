package com.example.contactstest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class CloudSmsProcesser {
	public static final String SMS_URI_ALL 		= "content://sms/";
	public static final String SMS_URI_INBOX 	= "content://sms/inbox";
	public static final String SMS_URI_SEND 	= "content://sms/sent";
	public static final String SMS_URI_DRAFT 	= "content://sms/draft";
	public static final String SMS_URI_OUTBOX 	= "content://sms/outbox";
	public static final String SMS_URI_FAILED 	= "content://sms/failed";
	public static final String SMS_URI_QUEUED 	= "content://sms/queued";
	public static final String SMS_URI_CANONICAL_ADDRESSES = "content://mms-sms/canonical-addresses";
	
	public static final String COL_ID 			= "_id";
	public static final String COL_THREAD_ID 	= "thread_id";
	public static final String COL_ADDRESS 		= "address";
	public static final String COL_DATE 		= "date";
	public static final String COL_READ 		= "read";
	public static final String COL_STATUS 		= "status";
	public static final String COL_TYPE 		= "type";
	public static final String COL_SUBJECT 		= "subject";
	public static final String COL_BODY 		= "body";
	public static final String COL_MSG_COUNT    = "message_count";
	public static final String COL_RECIPIENT_IDS = "recipient_ids";
	public static final String COL_SNIPPET 		= "snippet";
    
	private Context mContext;
	public CloudSmsProcesser(Context context) {
		assert (context != null);
		mContext = context;
	}
	
	private void checkInitialized() {
        if (mContext == null) {
        	throw new IllegalStateException("context is null");
        }
    }
	
	public void runTest() {
		getSms(SMS_URI_ALL, 0, 0, null, null);
		
		HashMap<String, CloudSmsThread> threads = getSmsThreads(0, 0, null);
		List<String> recipientIdList = new ArrayList<String>();
		for (CloudSmsThread thread : threads.values()) {
			if (thread.getRecipientIds() != null) {
				for (String recipientId : thread.getRecipientIds()) {
					recipientIdList.add(recipientId);
				}
			}
		}
		
		HashMap<String, String> addresses = getAddressesByRecipientId(recipientIdList);
		List<String> numberList = new ArrayList<String>();
		for (String number : addresses.values()) {
			numberList.add(number);
		}
	}
	
	/**
	 * 获取对话信息
	 * @param startPos
	 * @param num
	 * @param where
	 * @return key-threadId
	 */
	public HashMap<String, CloudSmsThread> getSmsThreads(int startPos, int num, String where) {
		checkInitialized();
		if (startPos < 0 || num < 0)
			return null;
		
		LinkedHashMap<String, CloudSmsThread> threads = new LinkedHashMap<String, CloudSmsThread>();
		ContentResolver resolver = mContext.getContentResolver();
		Cursor cursor = resolver.query(Uri.parse(SMS_URI_ALL), new String[] { "* from threads--" },
				where, null, "date desc");	// Note: 这里的orderBy好像不起作用，需要重新排序
		if (cursor == null) {
			return null;
		}
		
		try {
			if (cursor.getCount() == 0)	// 没有通话记录
				return threads;
			
			if (startPos >= cursor.getCount())
				return null;
			
			if (!cursor.moveToPosition(startPos))
				return null;
			
			// Note: 由于orderBy无效，这里重新按date排序
			List<CloudSmsThread> threadList = new ArrayList<CloudSmsThread>();
			do {
				CloudSmsThread thread = new CloudSmsThread();
				thread.setId(cursor.getString(cursor.getColumnIndex(COL_ID)));
				thread.setDate(cursor.getString(cursor.getColumnIndex(COL_DATE)));
				thread.setMessageCount(cursor.getString(cursor.getColumnIndex(COL_MSG_COUNT)));
				thread.setRead(cursor.getString(cursor.getColumnIndex(COL_READ)));
				thread.setSnippet(cursor.getString(cursor.getColumnIndex(COL_SNIPPET)));
				String recipientIds = cursor.getString(cursor.getColumnIndex(COL_RECIPIENT_IDS));
				thread.setRecipientIds(recipientIds.split(" "));
				threadList.add(thread);
			} while (cursor.moveToNext() && (threads.size() < num || num == 0));
			
			Collections.sort(threadList, new Comparator<CloudSmsThread>() {
				@Override
				public int compare(CloudSmsThread lhs, CloudSmsThread rhs) {
					return Long.valueOf(lhs.getDate()).compareTo(Long.valueOf(rhs.getDate())) * (-1);	// 降序
				}
			});
			
			for (CloudSmsThread thread : threadList) {
				threads.put(thread.getId(), thread);
			}
		} finally {
			cursor.close();
		}
		
		return threads;
	}
	
	/**
	 * 通过threadId获取CloudSmsThread
	 * @param threadIdList
	 * @return
	 */
	public HashMap<String, CloudSmsThread> getSmsThreads(List<String> threadIdList) {
		if (threadIdList == null)
			return null;
		
		LinkedHashMap<String, CloudSmsThread> threads = new LinkedHashMap<String, CloudSmsThread>();
		ContentResolver resolver = mContext.getContentResolver();
		String where = CloudContactUtils.joinWhere(COL_ID, threadIdList);
		Cursor cursor = resolver.query(Uri.parse(SMS_URI_ALL), new String[] { "* from threads--" },
				where, null, "date desc");	// Note: 这里的orderBy好像不起作用，需要重新排序
		if (cursor == null) {
			return null;
		}
		
		while (cursor.moveToNext()) {
			CloudSmsThread thread = new CloudSmsThread();
			thread.setId(cursor.getString(cursor.getColumnIndex(COL_ID)));
			thread.setDate(cursor.getString(cursor.getColumnIndex(COL_DATE)));
			thread.setMessageCount(cursor.getString(cursor.getColumnIndex(COL_MSG_COUNT)));
			thread.setRead(cursor.getString(cursor.getColumnIndex(COL_READ)));
			thread.setSnippet(cursor.getString(cursor.getColumnIndex(COL_SNIPPET)));
			String recipientIds = cursor.getString(cursor.getColumnIndex(COL_RECIPIENT_IDS));
			thread.setRecipientIds(recipientIds.split(" "));
			threads.put(thread.getId(), thread);
		}
		
		return threads;
	}
	
	/**
	 * 根据threadId获取对应的电话号码
	 * @param threadIdList
	 * @return key-threadId, or null
	 */
	public HashMap<String, String> getAddressesByThreadId(List<String> threadIdList) {
		if (threadIdList == null)
			return null;
		
		HashMap<String, CloudSmsThread> threads = getSmsThreads(threadIdList);
		if (threads == null)
			return null;
		
		List<String> recipientIdList = new ArrayList<String>();
		for (CloudSmsThread thread : threads.values()) {
			if (thread.getRecipientIds() != null) {
				for (String recipientId : thread.getRecipientIds()) {
					recipientIdList.add(recipientId);
				}
			}
		}
		
		return getAddressesByRecipientId(recipientIdList);
	}
	
	/**
	 * 获取thread id对应的规范化的电话号码
	 * @param recipientIdList
	 * @return key-threadId
	 */
	private HashMap<String, String> getAddressesByRecipientId(List<String> recipientIdList) {
		checkInitialized();
		if (recipientIdList == null)
			return null;
		
		LinkedHashMap<String, String> addresses = new LinkedHashMap<String, String>();
		if (recipientIdList.isEmpty()) {
			return addresses;
		}
		
		ContentResolver resolver = mContext.getContentResolver();
		String where = CloudContactUtils.joinWhere(COL_ID, recipientIdList);
		Cursor cursor = resolver.query(Uri.parse(SMS_URI_CANONICAL_ADDRESSES), new String[] {COL_ID, COL_ADDRESS},
				where, null, null);
		if (cursor == null) {
			return null;
		}
		
		try {
			while (cursor.moveToNext()) {
				String id = cursor.getString(cursor.getColumnIndex(COL_ID));
				String address = cursor.getString(cursor.getColumnIndex(COL_ADDRESS));
				addresses.put(id, address.replace(" ", ""));	// 去除规范化电话号码中的空格：如"10 6579 5555" -> "1065795555"
			}
		} finally {
			cursor.close();
		}
		
		return addresses;
	}
	
	/**
	 * 获取短信信息
	 * @param uri
	 * @param startPos
	 * @param num
	 * @param where
	 * @return 短信信息, or null
	 * key-id
	 */
	private HashMap<String, CloudSms> getSms(String uri, int startPos, int num, String where, String orderBy) {
		checkInitialized();
		if (uri == null || startPos < 0 || num < 0)
			return null;
		
		LinkedHashMap<String, CloudSms> smsMap = new LinkedHashMap<String, CloudSms>();
		ContentResolver resolver = mContext.getContentResolver();
		if (orderBy == null) {
			orderBy = "date desc";
		}
		Cursor cursor = resolver.query(Uri.parse(uri), new String[]{COL_ID, COL_THREAD_ID, COL_ADDRESS,
				COL_DATE, COL_READ, COL_STATUS, COL_TYPE, COL_SUBJECT, COL_BODY},
				where, null, orderBy);
		if (cursor == null) {
			return null;
		}
		
		try {
			if (cursor.getCount() == 0)	// 没有通话记录
				return smsMap;
			
			if (startPos >= cursor.getCount())
				return null;
			
			if (!cursor.moveToPosition(startPos))
				return null;
			
			do {
				CloudSms sms = new CloudSms();
				sms.setId(cursor.getString(cursor.getColumnIndex(COL_ID)));
				sms.setAddress(cursor.getString(cursor.getColumnIndex(COL_ADDRESS)));
				sms.setBody(cursor.getString(cursor.getColumnIndex(COL_BODY)));
				sms.setDate(cursor.getString(cursor.getColumnIndex(COL_DATE)));
				sms.setRead(cursor.getString(cursor.getColumnIndex(COL_READ)));
				sms.setStatus(cursor.getString(cursor.getColumnIndex(COL_STATUS)));
				sms.setSubject(cursor.getString(cursor.getColumnIndex(COL_SUBJECT)));
				sms.setThreadId(cursor.getString(cursor.getColumnIndex(COL_THREAD_ID)));
				sms.setType(cursor.getString(cursor.getColumnIndex(COL_TYPE)));
				smsMap.put(sms.getId(), sms);
			} while (cursor.moveToNext() && (smsMap.size() < num || num == 0));
		} finally {
			cursor.close();
		}
		
		return smsMap;
	}
}
