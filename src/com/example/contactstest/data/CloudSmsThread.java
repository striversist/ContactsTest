package com.example.contactstest.data;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;

public class CloudSmsThread {
	private String mId		= "";
	private String mDate	= "";
	private String mMessageCount = "";
	private String[] mRecipientIds;
	private ArrayList<String> mNumberList = new ArrayList<String>();
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
	
	public boolean hasRecipientId(String recipientid) {
		if (mRecipientIds == null)
			return false;
		
		for (String id : mRecipientIds) {
			if (TextUtils.equals(id, recipientid)) {
				return true;
			}
		}
		return false;
	}
	
	public void setNumberList(List<String> numberList) {
		if (numberList == null)
			return;
		mNumberList.clear();
		mNumberList.addAll(numberList);
	}
	
	public void addNumber(String list) {
		mNumberList.add(list);
	}
	
	public ArrayList<String> getNumberList() {
		return mNumberList;
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
	
	public String getRead() {
	    return mRead;
	}
	
	public boolean isRead() {
		return TextUtils.equals(mRead, "1");
	}
}
