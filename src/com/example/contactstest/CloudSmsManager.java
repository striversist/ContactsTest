package com.example.contactstest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.example.contactstest.data.CloudSms;
import com.example.contactstest.data.CloudSmsThread;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

public class CloudSmsManager {
	private static final String TAG = "CloudSmsManager";
	private static final String SENT_SMS_ACTION = "SENT_SMS_ACTION";
	private static final String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
	private static final String INTENT_USER_CODE = "user_code";
	private static final String KEY_ADDRESS = "address";
	private static final String KEY_BODY = "body";
	
	private Context mContext;
	private Handler mHandler;
	private int mSentSmsCount;
	private int mInboxSmsCount;
	private int mSmsThreadsCount;
	private CloudSmsProcesser mProcesser;
	private SparseArray<SendSmsCallback> mSendCallbacks = new SparseArray<CloudSmsManager.SendSmsCallback>();
	private SparseArray<DeliverSmsCallback> mDeliverCallbacks = new SparseArray<CloudSmsManager.DeliverSmsCallback>();
	private SparseArray<SmsObserver> mObservers = new SparseArray<CloudSmsManager.SmsObserver>();
	
	/**
	 * The caller thread should already Looper.prepare()
	 * @param context
	 */
	public CloudSmsManager(Context context) {
	    init(context, null);
	}
	
	public CloudSmsManager(Context context, Handler handler) {
		init(context, handler);
	}
	
	private void init(Context context, Handler handler) {
	    assert (context != null);
        mContext = context;
        if (handler != null) {
            mHandler = handler;
        } else {
            mHandler = new Handler();
        }
        mProcesser = new CloudSmsProcesser(mContext);
        
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int userCode = intent.getIntExtra(INTENT_USER_CODE, 0);
                Log.d(TAG, "message sent, user code=" + userCode);
                if (getResultCode() == Activity.RESULT_OK) {    // 发送成功，需要写入数据库
                    String address = intent.getStringExtra(KEY_ADDRESS);
                    String body = intent.getStringExtra(KEY_BODY);
                    if (!TextUtils.isEmpty(address) && !TextUtils.isEmpty(body)) {
                        CloudSms sms = new CloudSms();
                        sms.setAddress(address);
                        sms.setDate(String.valueOf(System.currentTimeMillis()));
                        sms.setRead("1");
                        sms.setType("2");
                        sms.setBody(body);
                        
                        new CloudSmsProcesser(mContext).writeSmsToDatabase(CloudSmsProcesser.SMS_URI_SENT, sms);
                    }
                }
                SendSmsCallback callback = mSendCallbacks.get(userCode);
                if (callback != null) {
                    mSendCallbacks.remove(userCode);
                    callback.onResult(userCode, intent.getExtras(), getResultCode());
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
                    callback.onResult(userCode, intent.getExtras(), getResultCode());
                }
            }
        }, new IntentFilter(DELIVERED_SMS_ACTION));
        
        startObserver();
	}
	
	public void runTest() {
		addSmsObserver(111, new SmsObserver() {
			@Override
			public void onNewThread(int userCode, CloudSmsThread thread) {
				Log.d(TAG, "onNewThread userCode=" + userCode);
				Log.d(TAG, "thread: date= " + thread.getDate() + ", address=" 
						+ thread.getNumberList() + ", snippet=" + thread.getSnippet());
			}

			@Override
			public void onNewSms(int userCode, CloudSms sms) {
				Log.d(TAG, "onNewSms userCode=" + userCode);
				Log.d(TAG, "onNewSms: date=" + sms.getDate() + ", address=" + sms.getAddress() + ", body=" + sms.getBody());
			} 
		});
		
		String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
				.format(new Date());
		sendTextMessage("18602769673", date + ": 测试", 123, null, new SendSmsCallback() {
			@Override
			public void onResult(int userCode, Bundle data, int resultCode) {
				Log.d(TAG, "runTest SendSmsCallback:" + " userCode=" + userCode + " resultCode=" + resultCode);
			}
		}, new DeliverSmsCallback() {
			@Override
			public void onResult(int userCode, Bundle data, int resultCode) {
				Log.d(TAG, "runTest DeliverSmsCallback:" + " userCode=" + userCode + " resultCode=" + resultCode);
			}
		});
	}
	
	/**
	 * 监控新会话、新短信（若新建一条会话，则onNewThread会被调用，而onNewSms不会被调用）
	 * @author aarontang
	 *
	 */
	public interface SmsObserver {
		public void onNewThread(int userCode, CloudSmsThread thread);
		public void onNewSms(int userCode, CloudSms sms);
	}
	
	public interface SendSmsCallback {
		public void onResult(int userCode, Bundle userData, int resultCode);
		public static final int RESULT_OK = Activity.RESULT_OK;
		public static final int RESULT_ERROR_GENERIC_FAILURE = SmsManager.RESULT_ERROR_GENERIC_FAILURE;
		public static final int RESULT_ERROR_RADIO_OFF = SmsManager.RESULT_ERROR_RADIO_OFF;
		public static final int RESULT_ERROR_NULL_PDU = SmsManager.RESULT_ERROR_NULL_PDU;
		public static final int RESULT_ERROR_NO_SERVICE = SmsManager.RESULT_ERROR_NO_SERVICE;
	}
	
	public interface DeliverSmsCallback {
		public void onResult(int userCode, Bundle userData, int resultCode);
		public static final int RESULT_OK = Activity.RESULT_OK;
		public static final int RESULT_FAILED = Activity.RESULT_OK + 1;
	}
	
	public boolean sendTextMessage(String number, String content, int userCode,
			Bundle userData, SendSmsCallback sendCallback, DeliverSmsCallback deliverCallback) {
		return sendTextMessage(number, null, content, userCode, userData, sendCallback, deliverCallback);
	}
	
	public boolean sendTextMessage(String number, String scAddress, String content, int userCode, 
			Bundle userData, SendSmsCallback sendCallback, DeliverSmsCallback deliverCallback) {
		if (number == null || TextUtils.isEmpty(number.trim()) || content == null)
			return false;
		
		SmsManager smsManager = SmsManager.getDefault();
		if (smsManager == null)
			return false;
		
		// 需要将callback注册提前，以防broadcast时callback还没来得及注册
		// FIXME: 目前暂定为若发送多条短信（字数>70），只回一次callback
		if (sendCallback != null) {
            mSendCallbacks.put(userCode, sendCallback);
        }
        if (deliverCallback != null) {
            mDeliverCallbacks.put(userCode, deliverCallback);
        }
		
        ArrayList<String> texts = smsManager.divideMessage(content);  //拆分短信
        for (String text : texts) {
            try {
                Intent sentIntent = new Intent(SENT_SMS_ACTION);
                sentIntent.putExtra(KEY_ADDRESS, number);
                sentIntent.putExtra(KEY_BODY, text);
                sentIntent.putExtra(INTENT_USER_CODE, userCode);
                if (userData != null) {
                    sentIntent.putExtras(userData);
                }
                PendingIntent sentPIntent = PendingIntent.getBroadcast(mContext, userCode, sentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                
                Intent deliveredIntent = new Intent(DELIVERED_SMS_ACTION);
                deliveredIntent.putExtra(KEY_ADDRESS, number);
                deliveredIntent.putExtra(KEY_BODY, text);
                deliveredIntent.putExtra(INTENT_USER_CODE, userCode);
                if (userData != null) {
                    deliveredIntent.putExtras(userData);
                }
                PendingIntent deliveryPIntent = PendingIntent.getBroadcast(mContext, userCode, deliveredIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                
                smsManager.sendTextMessage(number, scAddress, text, sentPIntent, deliveryPIntent);
            } catch (Exception ex) {
                mSendCallbacks.remove(userCode);
                mDeliverCallbacks.remove(userCode);
                return false;
            }
        }
        
        return true; 
	}
	
	public void addSmsObserver(int userCode, SmsObserver observer) {
		if (observer == null)
			return;
		
		mObservers.put(userCode, observer);
	}
	
	public void removeSmsObserver(int userCode) {
		mObservers.remove(userCode);
	}
	
	private void startObserver() {
		ContentResolver resolver = mContext.getContentResolver();
		mSentSmsCount = mProcesser.getSentSmsCount();
		mInboxSmsCount = mProcesser.getInboxSmsCount();
		mSmsThreadsCount = mProcesser.getThreadsCount();
		Log.d(TAG, "startObserver: sent=" + mSentSmsCount + ", inbox=" + mInboxSmsCount + ", threads=" + mSmsThreadsCount);
		
		// 对话流监听
		resolver.registerContentObserver(Uri.parse(CloudSmsProcesser.SMS_URI_THREADS), true, new ContentObserver(mHandler){
			@Override
			public void onChange(boolean selfChange) {
				super.onChange(selfChange);
				Log.d(TAG, "threads change");
				int sentSmsCount = mProcesser.getSentSmsCount();
				int inboxSmsCount = mProcesser.getInboxSmsCount();
				int threadsCount = mProcesser.getThreadsCount();
				Log.d(TAG, "onChange: sent=" + sentSmsCount + ", inbox=" + inboxSmsCount + ", threads=" + threadsCount);
				
				if (threadsCount > mSmsThreadsCount) {		// 收到新对话
					CloudSmsThread thread = mProcesser.getLatestSmsThread();
					if (thread != null) {
						for (int i=0; i<mObservers.size(); ++i) {
							int userCode = mObservers.keyAt(i);
							SmsObserver observer = mObservers.valueAt(i);
							observer.onNewThread(userCode, thread);
						}
					}
				} else if (threadsCount == mSmsThreadsCount) {
					CloudSms latestSms = null;
					if (sentSmsCount > mSentSmsCount) {		// 发送新短信
						latestSms = mProcesser.getLatestSms(CloudSmsProcesser.SMS_URI_SENT);
					}
					if (inboxSmsCount > mInboxSmsCount) {	// 接收新短信
						latestSms = mProcesser.getLatestSms(CloudSmsProcesser.SMS_URI_INBOX);
					}
					if (latestSms != null) {
						for (int i=0; i<mObservers.size(); ++i) {
							int userCode = mObservers.keyAt(i);
							SmsObserver observer = mObservers.valueAt(i);
							observer.onNewSms(userCode, latestSms);
						}
					}
				}
				
				mSentSmsCount = sentSmsCount;
				mInboxSmsCount = inboxSmsCount;
				mSmsThreadsCount = threadsCount;
			}
		});
	}
}
