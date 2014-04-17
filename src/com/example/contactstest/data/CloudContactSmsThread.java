package com.example.contactstest.data;

public class CloudContactSmsThread {
	// FIXME: 这里暂时不考虑对话中包含多个联系人的情况
	CloudSmsThread 	mSmsThread;
	CloudContact	mContact;
	
	public void setSmsThread(CloudSmsThread thread) {
		mSmsThread = thread;
	}
	
	public CloudSmsThread getSmsThread() {
		return mSmsThread;
	}
	
	public void setContact(CloudContact contact) {
		mContact = contact;
	}
	
	public boolean hasContact() {
		return mContact != null;
	}
	
	public CloudContact getContact() {
		return mContact;
	}
}
