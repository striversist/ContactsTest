package com.example.contactstest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

public class CloudSmsManager {
	private static final String TAG = "CloudSmsManager";
	private static final String SENT_SMS_ACTION = "SENT_SMS_ACTION";
	private static final String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
	private static final String INTENT_USER_CODE = "user_code";
	
	private Context mContext;
	private SparseArray<SendSmsCallback> mSendCallbacks = new SparseArray<CloudSmsManager.SendSmsCallback>();
	private SparseArray<DeliverSmsCallback> mDeliverCallbacks = new SparseArray<CloudSmsManager.DeliverSmsCallback>();
	
	public CloudSmsManager(Context context) {
		assert (context != null);
		mContext = context;
		
		mContext.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int userCode = intent.getIntExtra(INTENT_USER_CODE, 0);
				Log.d(TAG, "message sent, user code=" + userCode);
				SendSmsCallback callback = mSendCallbacks.get(userCode);
				if (callback != null) {
					mSendCallbacks.remove(userCode);
					callback.onResult(userCode, getResultCode());
				}
			}
		}, new IntentFilter(SENT_SMS_ACTION));
		
		mContext.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int userCode = intent.getIntExtra(INTENT_USER_CODE, 0);
				Log.d(TAG, "message delivered, user code=" + userCode);
				DeliverSmsCallback callback = mDeliverCallbacks.get(userCode);
				if (callback != null) {
					mDeliverCallbacks.remove(userCode);
					callback.onResult(userCode, getResultCode());
				}
			}
		}, new IntentFilter(DELIVERED_SMS_ACTION));
	}
	
	public void runTest() {
		String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
				.format(new Date());
		sendTextMessage("18602769673", date + ": 测试", 123, new SendSmsCallback() {
			@Override
			public void onResult(int userCode, int resultCode) {
				Log.d(TAG, "runTest SendSmsCallback:" + " userCode=" + userCode + " resultCode=" + resultCode);
			}
		}, new DeliverSmsCallback() {
			@Override
			public void onResult(int userCode, int resultCode) {
				Log.d(TAG, "runTest DeliverSmsCallback:" + " userCode=" + userCode + " resultCode=" + resultCode);
			}
		});
	}
	
	public interface SendSmsCallback {
		public void onResult(int userCode, int resultCode);
		public static final int RESULT_OK = Activity.RESULT_OK;
		public static final int RESULT_ERROR_GENERIC_FAILURE = SmsManager.RESULT_ERROR_GENERIC_FAILURE;
		public static final int RESULT_ERROR_RADIO_OFF = SmsManager.RESULT_ERROR_RADIO_OFF;
		public static final int RESULT_ERROR_NULL_PDU = SmsManager.RESULT_ERROR_NULL_PDU;
		public static final int RESULT_ERROR_NO_SERVICE = SmsManager.RESULT_ERROR_NO_SERVICE;
	}
	
	public interface DeliverSmsCallback {
		public void onResult(int userCode, int resultCode);
		public static final int RESULT_OK = Activity.RESULT_OK;
		public static final int RESULT_FAILED = Activity.RESULT_OK + 1;
	}
	
	public boolean sendTextMessage(String number, String content, int userCode,
			SendSmsCallback sendCallback, DeliverSmsCallback deliverCallback) {
		return sendTextMessage(number, null, content, userCode, sendCallback, deliverCallback);
	}
	
	public boolean sendTextMessage(String number, String scAddress, String content, int userCode, 
			SendSmsCallback sendCallback, DeliverSmsCallback deliverCallback) {
		if (number == null || TextUtils.isEmpty(number.trim()) || content == null)
			return false;
		
		SmsManager smsManager = SmsManager.getDefault();
		if (smsManager == null)
			return false;
		

		Intent sentIntent = new Intent(SENT_SMS_ACTION);
		sentIntent.putExtra(INTENT_USER_CODE, userCode);
		PendingIntent sentPIntent = PendingIntent.getBroadcast(mContext, userCode, sentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		Intent deliveredIntent = new Intent(DELIVERED_SMS_ACTION);
		deliveredIntent.putExtra(INTENT_USER_CODE, userCode);
		PendingIntent deliveryPIntent = PendingIntent.getBroadcast(mContext, userCode, deliveredIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
        if (content.length() > 70) {
            ArrayList<String> texts = smsManager.divideMessage(content);  //拆分短信
            for (String text : texts) {
                try {
                    smsManager.sendTextMessage(number, scAddress, text, sentPIntent, deliveryPIntent);
                } catch (Exception ex) {
                    return false;
                }
            }
        } else {
            try {
                smsManager.sendTextMessage(number, scAddress, content, sentPIntent, deliveryPIntent);
            } catch (Exception ex) {
                return false;
            }
        }
        
        if (sendCallback != null) {
        	mSendCallbacks.put(userCode, sendCallback);
        }
        if (deliverCallback != null) {
        	mDeliverCallbacks.put(userCode, deliverCallback);
        }
        return true; 
	}
}
