package com.example.contactstest;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;

public class CloudContact {
	private long 		mId;
	private String 		mName 		= "";
	private long 		mPhotoId;
	private int 		mTimesContacted;
	private long		mLastTimeContacted;
	private List<PhoneNumber> mPhoneNumberList = new ArrayList<CloudContact.PhoneNumber>();
	
	public static class PhoneNumber {
		String number;
		int	   type;
		String label;
	}
	
	public void setId(long id) {
		mId = id;
	}
	
	public long getId() {
		return mId;
	}
	
	public void setName(String name) {
		mName = name;
	}
	
	public String getName() {
		return mName;
	}
	
	public void setPhotoId(long photoId) {
		mPhotoId = photoId;
	}
	
	public long getPhotoId() {
		return mPhotoId;
	}
	
	public boolean hasNumber(String number) {
		if (!hasPhoneNumber())
			return false;
		
		for (PhoneNumber phoneNumber : mPhoneNumberList) {
			if (TextUtils.equals(phoneNumber.number, number))
				return true;
		}
		
		return false;
	}
	
	public boolean hasPhoneNumber() {
		return (mPhoneNumberList != null && !mPhoneNumberList.isEmpty());
	}
	
	public void addPhoneNumber(PhoneNumber phoneNumber) {
		if (mPhoneNumberList == null) {
			mPhoneNumberList = new ArrayList<CloudContact.PhoneNumber>();
		}
		mPhoneNumberList.add(phoneNumber);
	}
	
	public void setPhoneNumberList(List<PhoneNumber> phoneNumberList) {
		mPhoneNumberList.clear();
		mPhoneNumberList.addAll(phoneNumberList);
	}
	
	public List<PhoneNumber> getPhoneNumberList() {
		return mPhoneNumberList;
	}
	
	public void setTimesContacted(int timesContacted) {
		mTimesContacted = timesContacted;
	}
	
	public int getTimesContacted() {
		return mTimesContacted;
	}
	
	public void setLastTimeContacted(long lastTimeContacted) {
		mLastTimeContacted = lastTimeContacted;
	}
	
	public long getLastTimeContacted() {
		return mLastTimeContacted;
	}
}
