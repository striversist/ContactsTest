package com.example.contactstest;

import android.text.TextUtils;

public class CloudSmsThread {
	private String mId		= "";
	private String mDate	= "";
	private String mMessageCount = "";
	private String[] mRecipientIds;
	private String mSnippet	= "";
	private String mRead	= "";
	
	public void setId(String id) {
		mId = id;
	}
	
	public String getId() {
		return mId;
	}
	
	public void setDate(String date) {
		mDate = date;
	}
	
	public String getDate() {
		return mDate;
	}
	
	public void setMessageCount(String messageCount) {
		mMessageCount = messageCount;
	}
	
	public String getMessageCount() {
		return mMessageCount;
	}
	
	public void setRecipientIds(String[] ids) {
		mRecipientIds = ids;
	}
	
	public String[] getRecipientIds() {
		return mRecipientIds;
	}
	
	public void setSnippet(String snippet) {
		mSnippet = snippet;
	}
	
	public String getSnippet() {
		return mSnippet;
	}
	
	public void setRead(String read) {
		mRead = read;
	}
	
	public boolean isRead() {
		return TextUtils.equals(mRead, "1");
	}
}
