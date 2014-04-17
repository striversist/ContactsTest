package com.example.contactstest.data;

import android.text.TextUtils;

public class CloudSms {
	private String 	mId			= "";
	private String 	mThreadId	= "";
	private String 	mAddress 	= "";	// 电话号码
	private String 	mDate		= "";
	private String 	mRead		= "";
	private String 	mStatus		= "";
	private String 	mType		= "";	// 1:接收；2:发送
	private String 	mSubject 	= "";
	private String 	mBody 		= "";
	
	public void setId(String id) {
		mId = id;
	}
	
	public String getId() {
		return mId;
	}
	
	public void setThreadId(String threadId) {
		mThreadId = threadId;
	}
	
	public String getThreadId() {
		return mThreadId;
	}
	
	public void setAddress(String address) {
		mAddress = address;
	}
	
	public String getAddress() {
		return mAddress;
	}
	
	public void setDate(String date) {
		mDate = date;
	}
	
	public String getDate() {
		return mDate;
	}
	
	public void setRead(String read) {
		mRead = read;
	}
	
	public boolean isRead() {
		return TextUtils.equals(mRead, "1");
	}
	
	public void setStatus(String status) {
		mStatus = status;
	}
	
	public String getStatus() {
		return mStatus;
	}
	
	public void setType(String type) {
		mType = type;
	}
	
	public String getType() {
		return mType;
	}
	
	public void setSubject(String subject) {
		mSubject = subject;
	}
	
	public String getSubject() {
		return mSubject;
	}
	
	public void setBody(String body) {
		mBody = body;
	}
	
	public String getBody() {
		return mBody;
	}
}
