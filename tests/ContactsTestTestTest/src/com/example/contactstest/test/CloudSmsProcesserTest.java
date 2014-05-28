package com.example.contactstest.test;

import com.example.contactstest.core.CloudSmsProcesser;

import android.test.AndroidTestCase;

public class CloudSmsProcesserTest extends AndroidTestCase {

    public CloudSmsProcesserTest() {
    }
    
    @Override
    public void setUp() {
        
    }
    
    @Override
    public void tearDown() {
        
    }
    
    public void testGetAllSmsCount() {
        CloudSmsProcesser processer = new CloudSmsProcesser(getContext());
        int count = processer.getAllSmsCount();
        assertEquals(true, count >= 0);
    }
}
