package com.example.contactstest.data;

import java.util.ArrayList;

public class CloudSmsSearchResultThreadEntry {
    private String mThreadId;
    private ArrayList<String> mSmsIdList = new ArrayList<String>();
    
    public void setThreadId(String threadId) {
        mThreadId = threadId;
    }
    
    public String getThreadId() {
        return mThreadId;
    }
    
    public void setSmsIdList(ArrayList<String> smsIdList) {
        if (smsIdList == null)
            return;
        mSmsIdList.clear();
        mSmsIdList.addAll(smsIdList);
    }
    
    public ArrayList<String> getSmsIdList() {
        return mSmsIdList;
    }
    
    public void addSmsId(String smsId) {
        mSmsIdList.add(smsId);
    }
    
    public boolean hasSmsId(String smsId) {
        if (smsId == null)
            return false;
        return mSmsIdList.contains(smsId);
    }
}
