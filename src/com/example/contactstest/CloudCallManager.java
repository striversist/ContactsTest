package com.example.contactstest;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CloudCallManager {

    private static final String TAG = "CloudCallManager";
    private Context mContext;

    public CloudCallManager(Context context) {
        assert (context != null);
        mContext = context;
    }

    public void runTest() {
        registerCallStateListener(new PhoneStateListener() {
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
        });
    }

    public void registerCallStateListener(PhoneStateListener listener) {
        if (listener == null)
            return;
        TelephonyManager tm = (TelephonyManager) mContext
                .getSystemService(Service.TELEPHONY_SERVICE);
        tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
    }
    
    public void unregisterCallStateListener(PhoneStateListener listener) {
        if (listener == null)
            return;
        TelephonyManager tm = (TelephonyManager) mContext
                .getSystemService(Service.TELEPHONY_SERVICE);
        tm.listen(listener, PhoneStateListener.LISTEN_NONE);
    }

    public void call(String number) {
        if (number == null)
            return;

        Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
                + number));
        dialIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(dialIntent);
    }
}
