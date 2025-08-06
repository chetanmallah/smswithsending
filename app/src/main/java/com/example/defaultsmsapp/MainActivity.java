package com.example.defaultsmsapp;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;
import android.content.SharedPreferences;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int DEFAULT_SMS_REQUEST_CODE = 101;
    private static final String PREFS_NAME = "SmsAppPrefs";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final int PAGE_SIZE = 50;
    
    private RecyclerView recyclerView;
    private SmsAdapter smsAdapter;
    private List<SmsModel> smsList;
    private FloatingActionButton fabCompose;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View layoutEmpty;
    
    private BroadcastReceiver smsRefreshReceiver;
    private Handler mainHandler;
    private ExecutorService executorService;
    
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private int currentOffset = 0;
    private SmsCache smsCache;
    
    private final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.INTERNET
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeComponents();
        setupViews();
        setupSmsRefreshReceiver();
        setupSwipeRefresh();
        setupPagination();
        checkPermissionsAndSetupApp();
        
        // Start the monitoring service
        startSmsMonitorService();
    }
    
    private void initializeComponents() {
        mainHandler = new Handler();
        executorService = Executors.newFixedThreadPool(3);
        smsCache = new SmsCache(this);
        smsList = new ArrayList<>();
    }
    
    private void setupViews() {
        recyclerView = findViewById(R.id.recyclerViewSms);
        fabCompose = findViewById(R.id.fabCompose);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        
        smsAdapter = new SmsAdapter(this, smsList);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(smsAdapter);
        
        fabCompose.setOnClickListener(v -> {
            Intent intent = new Intent(this, ComposeActivity.class);
            startActivity(intent);
        });
    }
    
    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshMessages();
            });
            
            swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            );
        }
    }
    
    private void setupPagination() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && hasMoreData) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        loadMoreMessages();
                    }
                }
            }
        });
    }
    
    private void setupSmsRefreshReceiver() {
        smsRefreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "SMS refresh broadcast received: " + action);
                
                if ("com.example.defaultsmsapp.SMS_RECEIVED".equals(action) ||
                    "com.example.defaultsmsapp.SMS_CONTENT_CHANGED".equals(action) ||
                    "com.example.defaultsmsapp.MMS_RECEIVED".equals(action)) {
                    
                    // Immediate UI update for new messages
                    mainHandler.post(() -> {
                        refreshMessages();
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.defaultsmsapp.SMS_RECEIVED");
        filter.addAction("com.example.defaultsmsapp.SMS_CONTENT_CHANGED");
        filter.addAction("com.example.defaultsmsapp.MMS_RECEIVED");
        filter.setPriority(1000);
        
        registerReceiver(smsRefreshReceiver, filter);
    }
    
    private void startSmsMonitorService() {
        if (hasAllPermissions() && isDefaultSmsApp()) {
            Intent serviceIntent = new Intent(this, SmsMonitorService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "SMS Monitor Service started");
        }
    }
    
    private void checkPermissionsAndSetupApp() {
        if (!hasAllPermissions()) {
            requestPermissions();
        } else {
            checkDefaultSmsApp();
        }
    }
    
    private boolean hasAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsToRequest.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                checkDefaultSmsApp();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }
    
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs SMS and Contacts permissions to function properly. Please grant all permissions.")
            .setPositiveButton("Grant", (dialog, which) -> requestPermissions())
            .setNegativeButton("Exit", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }
    
    private void checkDefaultSmsApp() {
        if (!isDefaultSmsApp()) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
            
            if (isFirstLaunch) {
                showSetDefaultSmsDialog();
                prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
            } else {
                loadInitialMessages(); // Load messages immediately
            }
        } else {
            loadInitialMessages(); // Load messages immediately
            startSmsMonitorService();
        }
    }
    
    private boolean isDefaultSmsApp() {
        return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
    }
    
    private void showSetDefaultSmsDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Set as Default SMS App")
            .setMessage("To receive and send SMS messages automatically, this app needs to be set as your default SMS app.")
            .setPositiveButton("Set Default", (dialog, which) -> requestDefaultSmsApp())
            .setNegativeButton("Not Now", (dialog, which) -> {
                Toast.makeText(this, "You can set as default later in Settings", Toast.LENGTH_LONG).show();
                loadInitialMessages(); // Still load existing messages
            })
            .setCancelable(false)
            .show();
    }
    
    private void requestDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                Intent roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
                startActivityForResult(roleRequestIntent, DEFAULT_SMS_REQUEST_CODE);
            }
        } else {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
            startActivityForResult(intent, DEFAULT_SMS_REQUEST_CODE);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == DEFAULT_SMS_REQUEST_CODE) {
            if (isDefaultSmsApp()) {
                Toast.makeText(this, "App is now the default SMS app", Toast.LENGTH_SHORT).show();
                startSmsMonitorService();
            } else {
                Toast.makeText(this, "App was not set as default SMS app", Toast.LENGTH_SHORT).show();
            }
            loadInitialMessages();
        }
    }
    
    private void loadInitialMessages() {
        currentOffset = 0;
        hasMoreData = true;
        
        // First try to load from cache for instant display
        List<SmsModel> cachedMessages = smsCache.getCachedMessages(PAGE_SIZE);
        if (!cachedMessages.isEmpty()) {
            mainHandler.post(() -> {
                smsList.clear();
                smsList.addAll(cachedMessages);
                smsAdapter.notifyDataSetChanged();
                updateEmptyState();
                Log.d(TAG, "Loaded " + cachedMessages.size() + " cached messages");
            });
        }
        
        // Then load fresh data in background
        loadSmsMessages(true);
    }
    
    private void refreshMessages() {
        currentOffset = 0;
        hasMoreData = true;
        loadSmsMessages(true);
    }
    
    private void loadMoreMessages() {
        if (!isLoading && hasMoreData) {
            currentOffset += PAGE_SIZE;
            loadSmsMessages(false);
        }
    }
    
    private void loadSmsMessages(boolean isRefresh) {
        if (isLoading) return;
        
        isLoading = true;
        
        if (swipeRefreshLayout != null && isRefresh) {
            swipeRefreshLayout.setRefreshing(true);
        }
        
        executorService.execute(() -> {
            List<SmsModel> newMessages = loadSmsFromDatabase(currentOffset, PAGE_SIZE);
            
            mainHandler.post(() -> {
                isLoading = false;
                
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                
                if (isRefresh) {
                    smsList.clear();
                    currentOffset = 0;
                }
                
                if (newMessages.isEmpty()) {
                    hasMoreData = false;
                } else {
                    smsList.addAll(newMessages);
                    
                    // Cache the messages for faster loading next time
                    if (isRefresh) {
                        smsCache.cacheMessages(newMessages);
                    }
                }
                
                smsAdapter.notifyDataSetChanged();
                updateEmptyState();
                
                Log.d(TAG, "Loaded " + newMessages.size() + " messages, total: " + smsList.size());
            });
        });
    }
    
    private List<SmsModel> loadSmsFromDatabase(int offset, int limit) {
        List<SmsModel> messages = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "SMS permission not granted");
            return messages;
        }
        
        Uri smsUri = Uri.parse("content://sms/");
        String[] projection = {
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ
        };
        
        try (Cursor cursor = getContentResolver().query(
            smsUri, 
            projection, 
            null, 
            null, 
            Telephony.Sms.DATE + " DESC LIMIT " + limit + " OFFSET " + offset
        )) {
            
            if (cursor != null && cursor.moveToFirst()) {
                Map<String, List<SmsModel>> conversations = new HashMap<>();
                
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));
                    boolean isRead = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1;
                    
                    if (address == null) address = "Unknown";
                    if (body == null) body = "";
                    
                    String contactName = getContactName(address);
                    
                    SmsModel sms = new SmsModel(id, address, body, date, type, isRead, contactName);
                    
                    // Group by address for conversations
                    conversations.computeIfAbsent(address, k -> new ArrayList<>()).add(sms);
                    
                } while (cursor.moveToNext());
                
                // Add the latest message from each conversation
                for (List<SmsModel> conversation : conversations.values()) {
                    if (!conversation.isEmpty()) {
                        // Sort by date and take the latest
                        Collections.sort(conversation, (a, b) -> Long.compare(b.getDate(), a.getDate()));
                        messages.add(conversation.get(0));
                    }
                }
                
                // Sort all conversations by latest message date
                Collections.sort(messages, (a, b) -> Long.compare(b.getDate(), a.getDate()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading SMS messages", e);
        }
        
        return messages;
    }
    
    private String getContactName(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return phoneNumber;
        }
        
        Uri uri = Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, 
            Uri.encode(phoneNumber));
        String[] projection = {android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME};
        
        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact name", e);
        }
        
        return phoneNumber;
    }
    
    private void updateEmptyState() {
        if (layoutEmpty != null) {
            if (smsList.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (hasAllPermissions()) {
            // Only refresh if we don't have recent data
            if (smsList.isEmpty() || System.currentTimeMillis() - smsCache.getLastUpdateTime() > 30000) {
                loadInitialMessages();
            }
        }
        
        handleIncomingIntent();
    }
    
    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("sender")) {
            String sender = intent.getStringExtra("sender");
            Log.d(TAG, "Opened from notification for sender: " + sender);
            
            // Clear the intent extras to prevent repeated handling
            intent.removeExtra("sender");
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (smsRefreshReceiver != null) {
            try {
                unregisterReceiver(smsRefreshReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver", e);
            }
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent();
    }
    
    public void refreshMessages() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        refreshMessages();
    }
}