package com.example.contactstest.data;

import android.text.TextUtils;

public class CloudGroup {
    private long mId;
    private String mTitle;
    private String mNotes;
    private String mSystemId;
    private int mSummaryCount;
    
    public void setId(long id) {
        mId = id;
    }
    
    public long getId() {
        return mId;
    }
    
    public void setTitle(String title) {
        mTitle = title;
    }
    
    public String getTitle() {
        return mTitle;
    }
    
    public void setNotes(String notes) {
        mNotes = notes;
    }
    
    public String getNotes() {
        return mNotes;
    }
    
    public void setSystemId(String systemId) {
        mSystemId = systemId;
    }
    
    public String getSystemId() {
        return mSystemId;
    }
    
    public boolean hasSystemId() {
        return TextUtils.isEmpty(mSystemId);
    }
    
    public void setSummaryCount(int count) {
        mSummaryCount = count;
    }
    
    public int getSummaryCount() {
        return mSummaryCount;
    }
}
