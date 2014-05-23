package com.example.contactstest.core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import com.example.contactstest.data.CloudContact;
import com.example.contactstest.data.CloudContactSmsThread;
import com.example.contactstest.data.CloudSmsThread;

import android.content.Context;
import android.util.Log;

public class CloudMixProcesser {
	private static final String TAG = "CloudMixProcesser";
	private Context mContext;
	public CloudMixProcesser(Context context) {
		assert (context != null);
		mContext = context;
	}
	
	private void checkInitialized() {
        if (mContext == null) {
        	throw new IllegalStateException("context is null");
        }
    }
	
	public void runTest() {
//		call("15972072439");
		
		HashMap<String, CloudContactSmsThread> contactThreads = getContactThreads(0, 0);
		for (CloudContactSmsThread contactThread : contactThreads.values()) {
			CloudSmsThread thread = contactThread.getSmsThread();
			for (String number : thread.getNumberList()) {
				Log.d(TAG, "number: " + number);
				if (contactThread.hasContact(number)) {
					Log.d(TAG, "conatact name: " + contactThread.getContact(number).getName());
				}
			}
			String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
							.format(new Date(Long.valueOf(thread.getDate())));
			Log.d(TAG, "date: " + date);
			Log.d(TAG, "snippet: " + contactThread.getSmsThread().getSnippet());
			Log.d(TAG, "--------------------------------------");
		}
	}
	
	/**
	 * 获取最新的联系人会话流（注意：对于陌生的短信对话没有相应的联系人）
	 * @return
	 */
	public CloudContactSmsThread getLatestContactThread() {
	    HashMap<String, CloudContactSmsThread> contactThreads = getContactThreads(0, 1);
	    if (contactThreads == null)
	        return null;
	    
	    for (CloudContactSmsThread contactThread : contactThreads.values()) {
	        if (contactThread != null) {
	            return contactThread;
	        }
	    }
	    return null;
	}
	
	/**
	 * 根据threadId获取联系人会话流（注意：对于陌生的短信对话没有相应的联系人）
	 * @param threadId
	 * @return
	 */
	public CloudContactSmsThread getContactThread(String threadId) {
	    String where = CloudSmsProcesser.COL_ID + "=" + threadId;
	    HashMap<String, CloudContactSmsThread> contactThreads = getContactThreads(0, 1, where);
        if (contactThreads == null)
            return null;
        
        for (CloudContactSmsThread contactThread : contactThreads.values()) {
            if (contactThread != null) {
                return contactThread;
            }
        }
        return null;
	}
	
	/**
	 * 获取联系人会话流
	 * @param startPos
	 * @param num
	 * @return key-threadId
	 */
	public HashMap<String, CloudContactSmsThread> getContactThreads(int startPos, int num) {
	    return getContactThreads(startPos, num, null);
	}
	
	/**
	 * 获取联系人会话流（注意：对于陌生的短信对话没有相应的联系人）
	 * @param 
	 * @return key-threadId
	 */
	private HashMap<String, CloudContactSmsThread> getContactThreads(int startPos, int num, String where) {
		checkInitialized();
		if (startPos < 0 || num < 0)
			return null;
		
		CloudSmsProcesser smsProcesser = new CloudSmsProcesser(mContext);
		CloudContactsProcesser contactsProcesser = new CloudContactsProcesser(mContext);
		HashMap<String, CloudSmsThread> threads = smsProcesser.getSmsThreads(startPos, num, where);
		if (threads == null)
			return null;
		
		List<String> numberList = new ArrayList<String>();
		for (CloudSmsThread thread : threads.values()) {
			for (String number : thread.getNumberList()) {
				numberList.add(number);
			}
		}
		
		// 返回值
		LinkedHashMap<String, CloudContactSmsThread> contactThreads = new LinkedHashMap<String, CloudContactSmsThread>();
		
		// 获取所有号码相关的联系人信息
		HashMap<String, CloudContact> numberContacts = contactsProcesser.getContactsByNumber(numberList);
		for (CloudSmsThread thread : threads.values()) {
			CloudContactSmsThread contactThread = new CloudContactSmsThread();
			contactThread.setSmsThread(thread);
			if (numberContacts != null) {
				for (String number : thread.getNumberList()) {
					contactThread.addContact(number, numberContacts.get(number));
				}
			}
			contactThreads.put(thread.getId(), contactThread);
		}
		
		return contactThreads;
	}
}
