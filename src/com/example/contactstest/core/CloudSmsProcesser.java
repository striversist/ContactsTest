package com.example.contactstest.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.example.contactstest.data.CloudSms;
import com.example.contactstest.data.CloudSmsThread;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class CloudSmsProcesser {
	public static final String SMS_URI_ALL 		= "content://sms/";
	public static final String SMS_URI_INBOX 	= "content://sms/inbox";
	public static final String SMS_URI_SENT 	= "content://sms/sent";
	public static final String SMS_URI_DRAFT 	= "content://sms/draft";
	public static final String SMS_URI_OUTBOX 	= "content://sms/outbox";
	public static final String SMS_URI_FAILED 	= "content://sms/failed";
	public static final String SMS_URI_QUEUED 	= "content://sms/queued";
	public static final String SMS_URI_CANONICAL_ADDRESSES = "content://mms-sms/canonical-addresses";
	public static final String SMS_URI_THREADS 	= "content://mms-sms/conversations?simple=true";
	
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
		
		boolean result = deleteThread("114");
		Log.d("", "" + result);
	}
	
	public int getSentSmsCount() {
		return getSmsCount(SMS_URI_SENT);
	}
	
	public int getInboxSmsCount() {
		return getSmsCount(SMS_URI_INBOX);
	}
	
	public int getThreadsCount() {
		return getSmsCount(SMS_URI_THREADS);
	}
	
	private int getSmsCount(String uri) {
		checkInitialized();
		if (uri == null)
			return 0;
		
		ContentResolver resolver = mContext.getContentResolver();
		Cursor cursor = resolver.query(Uri.parse(uri), new String[]{COL_ID}, 
				null, null, null);
		if (cursor == null)
			return 0;
		
		int count = cursor.getCount();
		cursor.close();
		return count;
	}
	
	/**
	 * 根据Uri返回最新的短信
	 * @return
	 */
	public CloudSms getLatestSms(String uri) {
		if (uri == null)
			return null;
		
		HashMap<String, CloudSms> smss = getSms(uri, 0, 1, null, null);
		if (smss == null || smss.isEmpty())
			return null;
		
		CloudSms latestSms = null;
		for (CloudSms sms : smss.values()) {
			latestSms = sms;
			if (latestSms != null)
				break;
		}
		
		return latestSms;
	}
	
	/**
	 * 获取最新的一条对话记录
	 * @return 最新的对话记录; or null
	 */
	public CloudSmsThread getLatestSmsThread() {
		HashMap<String, CloudSmsThread> threads = getSmsThreads(0, 1, null);
		if (threads == null || threads.isEmpty())
			return null;
		
		CloudSmsThread latestThread = null;
		for (CloudSmsThread thread : threads.values()) {
			latestThread = thread;
			if (latestThread != null)
				break;
		}
		
		return latestThread;
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
		Cursor cursor = resolver.query(Uri.parse(SMS_URI_THREADS), 
				new String[] { COL_ID, COL_DATE, COL_MSG_COUNT, COL_READ, COL_SNIPPET, COL_RECIPIENT_IDS },
				where, null, "date desc");
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
			} while (cursor.moveToNext() && (threadList.size() < num || num == 0));
			
			// 获取对应的电话号码
			HashMap<String, List<String>> addresses = getAddressesByThread(threadList);
			if (addresses != null) {
				for (CloudSmsThread thread : threadList) {
					if (addresses.containsKey(thread.getId())) {
						thread.setNumberList(addresses.get(thread.getId()));
					}
				}
			}
			
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
		Cursor cursor = resolver.query(Uri.parse(SMS_URI_THREADS), 
				new String[] { COL_ID, COL_DATE, COL_MSG_COUNT, COL_READ, COL_SNIPPET, COL_RECIPIENT_IDS },
				where, null, "date desc");
		if (cursor == null) {
			return null;
		}
		
		List<CloudSmsThread> threadList = new ArrayList<CloudSmsThread>();
		try {
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
		} finally {
		    cursor.close();
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
		
		// 生成结果：thread对应的电话号码列表
		LinkedHashMap<String, List<String>> threadAddresses = new LinkedHashMap<String, List<String>>();
		// 获取接收者对应的电话号码
        HashMap<String, String> recipiendtAddress = getAddressesByRecipientId(recipientIdList);
        if (recipiendtAddress == null) {
            return threadAddresses;
        }
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
	
	/**
	 * 获取指定threadId的最新短信
	 * @param threadId
	 * @return 最新的sm, or null
	 */
	public CloudSms getLatestSmsInThread(String threadId) {
	    HashMap<String, CloudSms> smsMap = getSmsInThread(threadId, 0, 1);
	    if (smsMap == null)
	        return null;
	    for (CloudSms sms : smsMap.values()) {
	        if (sms != null) {
	            return sms;
	        }
	    }
	    return null;
	}
	
	/**
	 * 获取指定threadId的短信
	 * @param threadId
	 * @param startPos
	 * @param num
	 * @return key-"sms id"
	 */
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
	
	public boolean writeSmsToDatabase(String uri, CloudSms sms) {
	    checkInitialized();
	    if (uri == null || sms == null)
	        return false;
	    
	    ContentValues values = new ContentValues();
	    values.put("address", sms.getAddress());
	    values.put("date", sms.getDate());                
	    values.put("read", sms.getRead());
	    values.put("type", sms.getType());
	    values.put("body", sms.getBody());
	    
	    Uri result = mContext.getContentResolver().insert(Uri.parse(uri), values);
	    if (result == null) {
	        return false;
	    }
	    
	    return true;
	}
	
	/**
	 * 删除一条对话流
	 * @param threadId
	 * @return true(success); false(failed)
	 */
	public boolean deleteThread(String threadId) {
	    if (threadId == null)
	        return false;
	    List<String> threadIdList = new ArrayList<String>(1);
	    threadIdList.add(threadId);
	    return deleteThreads(threadIdList) > 0;
	}
	
	/**
	 * 删除会话流
	 * @param threadIdList
	 * @return 删除成功的个数
	 */
	public int deleteThreads(List<String> threadIdList) {
	    checkInitialized();
	    if (threadIdList == null)
	        return 0;
	    
	    ContentResolver resolver = mContext.getContentResolver();
	    int threadDeleted = 0;
	    for (String threadId : threadIdList) {
	        Uri deleteUri = ContentUris.withAppendedId(Uri.parse(SMS_URI_THREADS), Long.valueOf(threadId));
	        int rows = resolver.delete(deleteUri, null, null);
	        if (rows > 0) {    // 删除短信条数
	            threadDeleted++;
	        }
	    }
	    
	    return threadDeleted;
	}
	
	/**
	 * 删除一条短信
	 * @param smsId
	 * @return true(success); false(failed)
	 */
	public boolean deleteSms(String smsId) {
	    if (smsId == null)
	        return false;
	    List<String> smsIdList = new ArrayList<String>(1);
	    smsIdList.add(smsId);
	    return deleteSms(smsIdList) > 0;
	}
	
	/**
	 * 删除短信
	 * @param smsIdList
	 * @return 删除成功的个数
	 */
	public int deleteSms(List<String> smsIdList) {
	    return deleteIdList(SMS_URI_ALL, smsIdList);
	}
	
	/**
	 * 删除指定uri的记录
	 * @param idList
	 * @return
	 */
	private int deleteIdList(String uri, List<String> idList) {
	    checkInitialized();
	    if (uri == null || idList == null)
	        return 0;
	    if (idList.isEmpty())
	        return 0;
	    
	    ContentResolver resolver = mContext.getContentResolver();
        int rowsDeleted = 0;
        for (String id : idList) {
            Uri deleteUri = ContentUris.withAppendedId(Uri.parse(uri), Long.valueOf(id));
            int rows = resolver.delete(deleteUri, null, null);
            rowsDeleted += rows;
        }
        
        return rowsDeleted;
	}
}
