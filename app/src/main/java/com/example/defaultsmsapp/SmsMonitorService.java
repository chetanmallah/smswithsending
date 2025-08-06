package com.example.defaultsmsapp;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Telephony;
import android.app.PendingIntent;

public class SmsMonitorService extends Service {
    private static final String TAG = "SmsMonitorService";
    private static final String CHANNEL_ID = "sms_service_channel";
    private static final int NOTIFICATION_ID = 2001;
    
    private SmsContentObserver smsContentObserver;
    private BroadcastReceiver localReceiver;
    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SmsMonitorService created");
        
        mainHandler = new Handler();
        
        createServiceNotificationChannel();
        startForeground(NOTIFICATION_ID, createServiceNotification());
        
        // Set up SMS content observer
        setupSmsContentObserver();
        
        // Set up local broadcast receiver
        setupLocalBroadcastReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SmsMonitorService started");
        
        if (intent != null) {
            String action = intent.getStringExtra("action");
            if ("new_sms".equals(action)) {
                String sender = intent.getStringExtra("sender");
                String message = intent.getStringExtra("message");
                Log.d(TAG, "New SMS notification from service: " + sender);
                
                // Broadcast to update UI immediately
                Intent updateIntent = new Intent("com.example.defaultsmsapp.SMS_RECEIVED");
                updateIntent.putExtra("sender", sender);
                updateIntent.putExtra("message", message);
                sendBroadcast(updateIntent);
            }
        }
        
        // Return START_STICKY to ensure service restarts if killed
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SmsMonitorService destroyed");
        
        // Unregister content observer
        if (smsContentObserver != null) {
            try {
                getContentResolver().unregisterContentObserver(smsContentObserver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering content observer", e);
            }
        }
        
        // Unregister broadcast receiver
        if (localReceiver != null) {
            try {
                unregisterReceiver(localReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering broadcast receiver", e);
            }
        }
    }
    
    private void createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "SMS Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Keeps SMS app updated in background");
            serviceChannel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private android.app.Notification createServiceNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SMS Monitor Active")
                .setContentText("Monitoring SMS messages in background")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setShowWhen(false)
                .build();
    }

    private void setupSmsContentObserver() {
        smsContentObserver = new SmsContentObserver(mainHandler);
        try {
            getContentResolver().registerContentObserver(
                Telephony.Sms.CONTENT_URI, 
                true, 
                smsContentObserver
            );
            Log.d(TAG, "SMS content observer registered");
        } catch (Exception e) {
            Log.e(TAG, "Error registering SMS content observer", e);
        }
    }
    
    private void setupLocalBroadcastReceiver() {
        localReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.defaultsmsapp.SMS_RECEIVED".equals(intent.getAction())) {
                    Log.d(TAG, "Local SMS broadcast received in service");
                    // Additional processing can be done here
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.defaultsmsapp.SMS_RECEIVED");
        registerReceiver(localReceiver, filter);
    }
    
    private class SmsContentObserver extends ContentObserver {
        
        public SmsContentObserver(Handler handler) {
            super(handler);
        }
        
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            Log.d(TAG, "SMS content changed, URI: " + uri);
            
            // Notify MainActivity to refresh immediately
            Intent refreshIntent = new Intent("com.example.defaultsmsapp.SMS_CONTENT_CHANGED");
            sendBroadcast(refreshIntent);
            
            // Small delay to ensure the message is fully written to the database
            mainHandler.postDelayed(() -> {
                Intent delayedRefreshIntent = new Intent("com.example.defaultsmsapp.SMS_RECEIVED");
                sendBroadcast(delayedRefreshIntent);
            }, 50); // Reduced delay for faster updates
        }
    }
}