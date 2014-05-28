package com.example.contactstest.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.contactstest.core.CloudSmsProcesser;
import com.example.contactstest.data.CloudSms;
import com.example.contactstest.data.CloudSmsSearchResultThreadEntry;
import com.example.contactstest.data.CloudSmsThread;

import android.test.AndroidTestCase;

public class CloudSmsProcesserTest extends AndroidTestCase {

    private CloudSmsProcesser mProcesser;
    public CloudSmsProcesserTest() {
    }
    
    @Override
    public void setUp() {
        mProcesser = new CloudSmsProcesser(getContext());
    }
    
    @Override
    public void tearDown() {
        
    }
    
    public void testGetAllSmsCount() {
        int count = mProcesser.getAllSmsCount();
        assertEquals(true, count >= 0);
    }
    
    public void testGetSmsThreads() {
        HashMap<String, CloudSmsThread> threads = mProcesser.getSmsThreads(0, 0, null);
        assertNotNull("threads is null", threads);
    }
    
    public void testGetSmsThreads2() {
        HashMap<String, CloudSmsThread> threads = mProcesser.getSmsThreads(0, 0, null);
        List<String> threadIdList = new ArrayList<String>();
        for (CloudSmsThread thread: threads.values()) {
            threadIdList.add(thread.getId());
        }
        threads = mProcesser.getSmsThreads(threadIdList);
        assertNotNull("threads is null", threads);
    }
    
    public void testSearch() {
        final String keyword = "测试";
        HashMap<String, CloudSmsSearchResultThreadEntry> entries = mProcesser.search(keyword, null, null);
        assertNotNull("entries is null", entries);
        
        for (CloudSmsSearchResultThreadEntry threadEntry : entries.values()) {
            String threadId = threadEntry.getThreadId();
            ArrayList<String> smsIdList = threadEntry.getSmsIdList();
            assertTrue("smsIdList count less than zero", smsIdList.size() > 0);
            
            CloudSmsSearchResultThreadEntry entry = mProcesser.searchInThread(keyword, threadId);
            assertNotNull("entry is null", entry);
            
            String smdId = smsIdList.get(smsIdList.size() / 2);
            HashMap<String, CloudSms> nearbySmss = null;
            nearbySmss = mProcesser.getSmsInThreadNearby(entry.getThreadId(), smdId, 5, true);
            assertNotNull("nearbySmss is null", nearbySmss);
            
            nearbySmss = mProcesser.getSmsInThreadNearby(entry.getThreadId(), smdId, -5, false);
            assertNotNull("nearbySmss is null", nearbySmss);
        }
    }
}
