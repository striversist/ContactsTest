package com.example.contactstest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import com.example.contactstest.data.CloudCall;
import com.example.contactstest.data.CloudCallGroup;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.util.Log;

public class CloudCallsProcesser {
	private static final String TAG = "CloudCallsProcesser";
	public static final int CALL_TYPE_ALL			= 0;
	public static final int CALL_TYPE_INCOMMING 	= Calls.INCOMING_TYPE;
	public static final int CALL_TYPE_OUTCOMMING 	= Calls.OUTGOING_TYPE;
	public static final int CALL_TYPE_MISSED 		= Calls.MISSED_TYPE;
	private Context mContext;
	
	public CloudCallsProcesser(Context context) {
		assert (context != null);
		mContext = context;
	}
	
	public void runTest() {
		getCallsCount(0);
		getCalls(0, 0, 0, null, null);
		HashMap<String, CloudCallGroup> groups = getGroups();
		for (CloudCallGroup group : groups.values()) {
			Log.d(TAG, "number=" + group.getNumber());
			Log.d(TAG, "name=" + group.getContactName());
			Log.d(TAG, "size=" + group.size());
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(group.getLatestCall().getDate());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
			Log.d(TAG, "latestTime=" + sdf.format(calendar.getTime()));
		}
		CloudCallGroup group = getGroup("13986282955");
		Log.d(TAG, "group.size=" + group.size());
	}
	
	private void checkInitialized() {
        if (mContext == null) {
        	throw new IllegalStateException("context is null");
        }
    }
	
	/**
	 * 获取对应type记录条数
	 * @param type (拨入、拨出、未接); 若为0，则为所有
	 * @return total calls' count
	 */
	private int getCallsCount(int type) {
		checkInitialized();
		if (type < 0 || type > 3)
			return 0;
		
		ContentResolver resolver = mContext.getContentResolver();
		String where = "";
		if (type != 0) {
			where = "type=" + type;
		}
		Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, 
				new String[]{Calls._ID}, where, null, null);
		if (cursor == null) {
			return 0;
		}
		
		int count = cursor.getCount();
		cursor.close();
		return count;
	}
	
	/**
	 * 获取通话记录
	 * @param type (拨入、拨出、未接); 若为0，则为所有
	 * @param startPos
	 * @param num
	 * @param where 过滤条件，若传入不为null，则type将无效
	 * @param orderBy
	 * @return CloudCall map, or null
	 */
	public HashMap<Long, CloudCall> getCalls(int type, int startPos, int num, String where, String orderBy) {
		checkInitialized();
		if (type < 0 || type > 3 || startPos < 0 || num < 0)
			return null;
		
		LinkedHashMap<Long, CloudCall> calls = new LinkedHashMap<Long, CloudCall>();
		ContentResolver resolver = mContext.getContentResolver();
		if (where == null && type != 0) {
			where = "type=" + type;
		}
		if (orderBy == null) {
			orderBy = Calls.DEFAULT_SORT_ORDER;
		}
		Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, 
				new String[]{Calls._ID, Calls.NUMBER, Calls.DATE,
					Calls.DURATION, Calls.TYPE, Calls.CACHED_NAME},
				where, null, orderBy);
		if (cursor == null) {
			return null;
		}
		
		try {
			if (cursor.getCount() == 0)	// 没有通话记录
				return calls;
			
			if (startPos >= cursor.getCount())
				return null;
			
			if (!cursor.moveToPosition(startPos))
				return null;
			
			do {
				CloudCall call = new CloudCall();
				call.setId(cursor.getLong(cursor.getColumnIndex(Calls._ID)));
				call.setNumber(cursor.getString(cursor.getColumnIndex(Calls.NUMBER)));
				call.setDate(cursor.getLong(cursor.getColumnIndex(Calls.DATE)));
				call.setDuration(cursor.getInt(cursor.getColumnIndex(Calls.DURATION)));
				call.setType(cursor.getInt(cursor.getColumnIndex(Calls.TYPE)));
				call.setName(cursor.getString(cursor.getColumnIndex(Calls.CACHED_NAME)));
				calls.put(call.getId(), call);
			} while (cursor.moveToNext() && (calls.size() < num || num == 0));
		} finally {
			cursor.close();
		}
		
		return calls;
	}
	
	/**
	 * 根据某个号码，获取对应的通话记录分组
	 * @param number
	 * @return CloudCallGroup, or null
	 */
	private CloudCallGroup getGroup(String number) {
		checkInitialized();
		if (number == null)
			return null;
		
		CloudCallGroup group = new CloudCallGroup();
		HashMap<Long, CloudCall> calls = getCalls(0, 0, 0, Calls.NUMBER + "=" + number, null);
		if (calls == null)
			return null;
		
		for (CloudCall call : calls.values()) {
			group.addCall(call);
		}
		return group;
	}
	
	/**
	 * 返回通话记录分组（按电话号码分组）
	 * @return Group map, or null
	 */
	private HashMap<String, CloudCallGroup> getGroups() {
		checkInitialized();
		
		LinkedHashMap<String, CloudCallGroup> groups = new LinkedHashMap<String, CloudCallGroup>();
		HashMap<Long, CloudCall> allCalls = getCalls(0, 0, 0, null, null);
		if (allCalls == null)
			return null;
		
		for (CloudCall call : allCalls.values()) {
			if (!groups.containsKey(call.getNumber())) {	// 若不存在，则新建
				CloudCallGroup group = new CloudCallGroup();
				group.setNumber(call.getNumber());
				group.setContactName(call.getName());
				group.addCall(call);
				groups.put(group.getNumber(), group);
			} else {
				groups.get(call.getNumber()).addCall(call);
			}
		}
		
		return groups;
	}
	
	/**
	 * 删除一条通话记录
	 * @param callId
	 * @return true(success); false(failed)
	 */
	public boolean deleteCall(Long callId) {
	    if (callId == null)
	        return false;
	    
	    List<Long> callIdList = new ArrayList<Long>(1);
	    callIdList.add(callId);
	    return deleteCalls(callIdList) > 0;
	}
	
	/**
	 * 删除通话记录
	 * @param idList
	 * @return 成功删除的条数
	 */
	public int deleteCalls(List<Long> idList) {
		checkInitialized();
		if (idList == null)
			return 0;
		
		if (idList.isEmpty())
			return 0;
		
		ContentResolver resolver = mContext.getContentResolver();
		String where = CloudContactUtils.joinWhere(Calls._ID, idList);
		
		int rows = resolver.delete(CallLog.Calls.CONTENT_URI, where, null);
		return rows;
	}
}
