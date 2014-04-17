package com.example.contactstest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.example.contactstest.data.CloudContact;
import com.example.contactstest.data.CloudContactSmsThread;
import com.example.contactstest.data.CloudSmsThread;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
			if (contactThread.hasContact()) {
				Log.d(TAG, "conatact name: " + contactThread.getContact().getName());
			}
			Log.d(TAG, "number: " + contactThread.getSmsThread().getNumberList().get(0));
			Log.d(TAG, "snippet: " + contactThread.getSmsThread().getSnippet());
			Log.d(TAG, "--------------------------------------");
		}
	}
	
	/**
	 * 根据对话信息获取相应的联系人（注意：对于陌生的短信对话没有相应的联系人）
	 * @param threadIdList
	 * @return key-threadId
	 */
	private HashMap<String, CloudContactSmsThread> getContactThreads(int startPos, int num) {
		checkInitialized();
		if (startPos < 0 || num < 0)
			return null;
		
		CloudSmsProcesser smsProcesser = new CloudSmsProcesser(mContext);
		CloudContactsProcesser contactsProcesser = new CloudContactsProcesser(mContext);
		HashMap<String, CloudSmsThread> threads = smsProcesser.getSmsThreads(startPos, num, null);
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
		HashMap<String, CloudContact> numberContact = contactsProcesser.getContactsByNumber(numberList);
		for (CloudSmsThread thread : threads.values()) {
			CloudContactSmsThread contactThread = new CloudContactSmsThread();
			contactThread.setSmsThread(thread);
			if (!thread.getNumberList().isEmpty()) {
				// 暂时不考虑一条对话对应多个联系人的情况
				contactThread.setContact(numberContact.get(thread.getNumberList().get(0)));
			}
			contactThreads.put(thread.getId(), contactThread);
		}
		
		return contactThreads;
	}
	
	public void call(String number) {
		if (number == null)
			return;
		
		Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
		dialIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(dialIntent);
	}
}
