package com.example.contactstest;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CloudPhoneReceiver extends BroadcastReceiver {
    private static final String TAG = "CloudPhoneReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive action=" + intent.getAction());

        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) { // 拨出
            // TODO: 通知云桌面开始拨号
            Log.d(TAG, "new outgoing call");
            TelephonyManager tm = (TelephonyManager) context
                    .getSystemService(Service.TELEPHONY_SERVICE);
            tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    PhoneStateListener listener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                Log.d(TAG, "idle number: " + incomingNumber);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                Log.d(TAG, "offhook number: " + incomingNumber);
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                Log.d(TAG, "incomming number: " + incomingNumber);
                break;
            }
        }

    };

}
