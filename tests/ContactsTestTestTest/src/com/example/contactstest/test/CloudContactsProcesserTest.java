package com.example.contactstest.test;

import java.util.HashMap;

import com.example.contactstest.core.CloudContactsProcesser;
import com.example.contactstest.data.CloudContact;

import android.test.AndroidTestCase;


public class CloudContactsProcesserTest extends AndroidTestCase {

    private CloudContactsProcesser mProcesser;
    
    public CloudContactsProcesserTest() {
        
    }
    
    @Override
    public void setUp() {
        mProcesser = new CloudContactsProcesser(getContext());
    }
    
    @Override
    public void tearDown() {
        
    }
    
    public void testGetFrequentContacts() {
        HashMap<Long, CloudContact> contacts = mProcesser.getStarredContacts();
        assertNotNull("frequent contacts is null", contacts);
    }
}
