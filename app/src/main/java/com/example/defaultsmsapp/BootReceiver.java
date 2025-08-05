package com.example.defaultsmsapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Boot receiver triggered with action: " + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            
            // Check if this app is still the default SMS app
            String defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context);
            String currentPackage = context.getPackageName();
            
            if (currentPackage.equals(defaultSmsPackage)) {
                Log.d(TAG, "App is default SMS app, starting monitor service");
                
                // Start the SMS monitor service
                Intent serviceIntent = new Intent(context, SmsMonitorService.class);
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }
                    Log.d(TAG, "SMS Monitor Service started from boot receiver");
                } catch (Exception e) {
                    Log.e(TAG, "Error starting SMS Monitor Service from boot", e);
                }
            } else {
                Log.d(TAG, "App is not default SMS app, not starting service");
            }
        }
    }
}