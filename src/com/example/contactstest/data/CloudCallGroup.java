package com.example.contactstest.data;

import java.util.LinkedHashMap;

public class CloudCallGroup {
	private String mNumber = "";		// id
	private String mContactName = "";
	private LinkedHashMap<Long, CloudCall> mCalls = new LinkedHashMap<Long, CloudCall>();	// key: call-id
	
	public void setNumber(String number) {
		mNumber = number;
	}
	
	public String getNumber() {
		return mNumber;
	}
	
	public void setContactName(String name) {
		mContactName = name;
	}
	
	public String getContactName() {
		return mContactName;
	}
	
	public void addCall(CloudCall call) {
		if (call == null)
			return;
		mCalls.put(call.getId(), call);
	}
	
	public CloudCall removeCall(long id) {
		return mCalls.remove(id);
	}
	
	public int size() {
		return mCalls.size();
	}
	
	public CloudCall getLatestCall() {
		CloudCall latestCall = null;
		for (CloudCall call : mCalls.values()) {
			if (latestCall == null) {
				latestCall = call;
			} else if (latestCall.getDate() < call.getDate()) {
				latestCall = call;
			}
		}
		return latestCall;
	}
}
