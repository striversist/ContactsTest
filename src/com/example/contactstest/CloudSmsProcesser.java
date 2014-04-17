package com.example.contactstest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.example.contactstest.data.CloudSms;
import com.example.contactstest.data.CloudSmsThread;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

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
		
		List<String> threadIdList = new ArrayList<String>();
		threadIdList.add("94");
		threads = getSmsThreads(threadIdList);
		
		HashMap<String, String> addresses = getAddressesByRecipientId(recipientIdList);
		List<String> numberList = new ArrayList<String>();
		for (String number : addresses.values()) {
			numberList.add(number);
		}
		
		int count = getSmsCountInThread(SMS_URI_ALL, "94");
		Log.d("", "" + count);
		
		HashMap<String, CloudSms> smss = getSmsInThread(SMS_URI_ALL, "94", 0, 0);
		for (CloudSms sms : smss.values()) {
			Log.d("", sms.toString());
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
			List<String> threadIdList = new ArrayList<String>();
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
				threadIdList.add(thread.getId());
			} while (cursor.moveToNext() && (threads.size() < num || num == 0));
			
			// 获取对应的电话号码
			HashMap<String, List<String>> addresses = getAddressesByThread(threadList);
			if (addresses != null) {
				for (CloudSmsThread thread : threadList) {
					if (addresses.containsKey(thread.getId())) {
						thread.setNumberList(addresses.get(thread.getId()));
					}
				}
			}
			
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
		
		List<CloudSmsThread> threadList = new ArrayList<CloudSmsThread>();
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
			threadList.add(thread);
		}
		
		// 获取对应的电话号码
		HashMap<String, List<String>> addresses = getAddressesByThread(threadList);
		if (addresses != null) {
			for (CloudSmsThread thread : threadList) {
				if (addresses.containsKey(thread.getId())) {
					thread.setNumberList(addresses.get(thread.getId()));
				}
			}
		}
		
		return threads;
	}
	
	/**
	 * 根据threadId获取对应的电话号码
	 * @param threadList
	 * @return key-threadId, value-numberList or null
	 */
	private HashMap<String, List<String>> getAddressesByThread(List<CloudSmsThread> threadList) {
		if (threadList == null)
			return null;
		
		// 获取所有的接收者id
		List<String> recipientIdList = new ArrayList<String>();
		for (CloudSmsThread thread : threadList) {
			if (thread.getRecipientIds() != null) {
				for (String recipientId : thread.getRecipientIds()) {
					recipientIdList.add(recipientId);
				}
			}
		}
		
		// 获取接收者对应的电话号码
		HashMap<String, String> recipiendtAddress = getAddressesByRecipientId(recipientIdList);
		
		// 生成结果：thread对应的电话号码列表
		LinkedHashMap<String, List<String>> threadAddresses = new LinkedHashMap<String, List<String>>();
		for (CloudSmsThread thread : threadList) {
			if (thread.getRecipientIds() != null) {
				List<String> numberList = new ArrayList<String>();
				for (String recipientId : thread.getRecipientIds()) {
					if (recipiendtAddress.containsKey(recipientId)) {
						numberList.add(recipiendtAddress.get(recipientId));
					}
				}
				threadAddresses.put(thread.getId(), numberList);
			}
		}
		
		return threadAddresses;
	}
	
	/**
	 * 获取recipientId对应的规范化的电话号码
	 * @param recipientIdList
	 * @return key-recipientId, value-电话号码
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
	
	public int getSmsCountInThread(String threadId) {
		return getSmsCountInThread(SMS_URI_ALL, threadId);
	}
	
	/**
	 * 获取一个sms thread中包含多少条短信记录
	 * @param uri
	 * @param threadId
	 * @return 短信条数
	 */
	private int getSmsCountInThread(String uri, String threadId) {
		checkInitialized();
		if (threadId == null)
			return 0;
		
		ContentResolver resolver = mContext.getContentResolver();
		String where = COL_THREAD_ID + "=" + threadId;
		Cursor cursor = resolver.query(Uri.parse(uri), new String[]{COL_ID}, 
				where, null, null);
		if (cursor == null)
			return 0;
		
		int count = cursor.getCount();
		cursor.close();
		return count;
	}
	
	public HashMap<String, CloudSms> getSmsInThread(String threadId, int startPos, int num) {
		return getSmsInThread(SMS_URI_ALL, threadId, startPos, num);
	}
	
	/**
	 * 获取指定threadId的短信记录（按时间顺序有新到老的顺序）
	 * @param uri
	 * @param threadId
	 * @param startPos
	 * @param num
	 * @return sms record; or null
	 * key-id
	 */
	private HashMap<String, CloudSms> getSmsInThread(String uri, String threadId, int startPos, int num) {
		checkInitialized();
		if (uri == null || threadId == null || startPos < 0 || num < 0)
			return null;
		
		return getSms(uri, startPos, num, COL_THREAD_ID + "=" + threadId, null);
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
