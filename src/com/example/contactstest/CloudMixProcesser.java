package com.example.contactstest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class CloudMixProcesser {

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
		CloudSmsProcesser smsProcesser = new CloudSmsProcesser(mContext);
		HashMap<String, CloudSmsThread> threads = smsProcesser.getSmsThreads(0, 0, null);
		List<String> threadIdList = new ArrayList<String>();
		for (CloudSmsThread thread : threads.values()) {
			threadIdList.add(thread.getId());
		}
		HashMap<String, CloudContact> contacts = getThreadContacts(threadIdList);
		for (CloudContact contact : contacts.values()) {
			Log.d("", contact.toString());
		}
	}
	
	private HashMap<String, CloudContact> getThreadContacts(List<String> threadIdList) {
		checkInitialized();
		if (threadIdList == null)
			return null;
		
		CloudContactsProcesser contactsProcesser = new CloudContactsProcesser(mContext);
		CloudSmsProcesser smsProcesser = new CloudSmsProcesser(mContext);
		
		HashMap<String, String> addresses = smsProcesser.getAddressesByThreadId(threadIdList);
		List<String> numberList = new ArrayList<String>();
		for (String number : addresses.values()) {
			numberList.add(number);
		}
		
		return contactsProcesser.getContactsByNumber(numberList);
	}
}
