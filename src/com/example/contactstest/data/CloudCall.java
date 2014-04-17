package com.example.contactstest.data;

public class CloudCall {
	private long 	mId;
	private String 	mNumber = "";
	private long 	mDate;
	private long 	mDuration;
	private int 	mType;
	private String 	mName = "";
	
	public void setId(long id) {
		mId = id;
	}
	
	public long getId() {
		return mId;
	}
	
	public void setNumber(String number) {
		mNumber = number;
	}
	
	public String getNumber() {
		return mNumber;
	}
	
	public void setDate(long date) {
		mDate = date;
	}
	
	public long getDate() {
		return mDate;
	}
	
	public void setDuration(long duration) {
		mDuration = duration;
	}
	
	public long getDuration() {
		return mDuration;
	}
	
	public void setType(int type) {
		mType = type;
	}
	
	public int getType() {
		return mType;
	}
	
	public void setName(String name) {
		mName = name;
	}
	
	public String getName() {
		return mName;
	}
}
