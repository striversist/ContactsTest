package com.example.contactstest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CloudPhoneReceiver extends BroadcastReceiver {
	private static final String TAG = "CloudPhoneReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive action=" + intent.getAction());
		
		if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {	// 拨出
			// TODO: 通知云桌面开始拨号
		}
	}
}
