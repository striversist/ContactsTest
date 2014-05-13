package com.example.contactstest;

import java.lang.reflect.Method;

import com.android.internal.telephony.ITelephony;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
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
        call("18602769673");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean success = endCall();
                Log.d(TAG, "endCall success=" + success);
            }
        }, 10000);
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
    
    /**
     * 挂断电话
     * @return 是否挂断成功
     */
    public boolean endCall() {
        try {
            Method method = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
            IBinder binder = (IBinder) method.invoke(null, new Object[]{"phone"});
            ITelephony telephony = ITelephony.Stub.asInterface(binder);
            return telephony.endCall();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
