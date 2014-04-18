package com.example.contactstest.data;

import java.util.LinkedHashMap;

public class CloudContactSmsThread {
	CloudSmsThread 	mSmsThread;
	LinkedHashMap<String, CloudContact> mContacts = new LinkedHashMap<String, CloudContact>();
	
	public void setSmsThread(CloudSmsThread thread) {
		mSmsThread = thread;
	}
	
	public CloudSmsThread getSmsThread() {
		return mSmsThread;
	}
	
	public void addContact(String number, CloudContact contact) {
		if (number == null || contact == null)
			return;
		
		mContacts.put(number, contact);
	}
	
	public boolean hasContact() {
		return !mContacts.isEmpty();
	}
	
	public boolean hasContact(String number) {
		return mContacts.containsKey(number);
	}
	
	public CloudContact getContact(String number) {
		return mContacts.get(number);
	}
}
