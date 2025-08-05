// package com.example.defaultsmsapp;

// import android.Manifest;
// import android.app.role.RoleManager;
// import android.content.BroadcastReceiver;
// import android.content.Context;
// import android.content.Intent;
// import android.content.IntentFilter;
// import android.content.pm.PackageManager;
// import android.database.Cursor;
// import android.net.Uri;
// import android.os.Build;
// import android.os.Bundle;
// import android.provider.Telephony;
// import android.util.Log;
// import android.widget.Toast;

// import androidx.annotation.NonNull;
// import androidx.appcompat.app.AlertDialog;
// import androidx.appcompat.app.AppCompatActivity;
// import androidx.core.app.ActivityCompat;
// import androidx.core.content.ContextCompat;
// import androidx.recyclerview.widget.LinearLayoutManager;
// import androidx.recyclerview.widget.RecyclerView;

// import com.google.android.material.floatingactionbutton.FloatingActionButton;

// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// public class MainActivity extends AppCompatActivity {
//     private static final String TAG = "MainActivity";
//     private static final int PERMISSION_REQUEST_CODE = 100;
//     private static final int DEFAULT_SMS_REQUEST_CODE = 101;
    
//     private RecyclerView recyclerView;
//     private SmsAdapter smsAdapter;
//     private List<SmsModel> smsList;
//     private FloatingActionButton fabCompose;
    
//     private BroadcastReceiver smsRefreshReceiver;
    
//     private final String[] REQUIRED_PERMISSIONS = {
//         Manifest.permission.READ_SMS,
//         Manifest.permission.SEND_SMS,
//         Manifest.permission.RECEIVE_SMS,
//         Manifest.permission.READ_CONTACTS,
//         Manifest.permission.READ_PHONE_STATE
//     };

//     @Override
//     protected void onCreate(Bundle savedInstanceState) {
//         super.onCreate(savedInstanceState);
//         setContentView(R.layout.activity_main);
        
//         initViews();
//         setupSmsRefreshReceiver();
//         checkPermissionsAndSetupApp();
//     }
    
//     private void initViews() {
//         recyclerView = findViewById(R.id.recyclerViewSms);
//         fabCompose = findViewById(R.id.fabCompose);
        
//         smsList = new ArrayList<>();
//         smsAdapter = new SmsAdapter(this, smsList);
        
//         recyclerView.setLayoutManager(new LinearLayoutManager(this));
//         recyclerView.setAdapter(smsAdapter);
        
//         fabCompose.setOnClickListener(v -> {
//             Intent intent = new Intent(this, ComposeActivity.class);
//             startActivity(intent);
//         });
//     }
    
//     private void setupSmsRefreshReceiver() {
//         smsRefreshReceiver = new BroadcastReceiver() {
//             @Override
//             public void onReceive(Context context, Intent intent) {
//                 Log.d(TAG, "SMS refresh broadcast received");
//                 loadSmsMessages();
//             }
//         };
        
//         IntentFilter filter = new IntentFilter();
//         filter.addAction("com.example.defaultsmsapp.SMS_RECEIVED");
//         filter.addAction("com.example.defaultsmsapp.MMS_RECEIVED");
//         registerReceiver(smsRefreshReceiver, filter);
//     }
    
//     private void checkPermissionsAndSetupApp() {
//         if (!hasAllPermissions()) {
//             requestPermissions();
//         } else {
//             checkDefaultSmsApp();
//         }
//     }
    
//     private boolean hasAllPermissions() {
//         for (String permission : REQUIRED_PERMISSIONS) {
//             if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
//                 return false;
//             }
//         }
//         return true;
//     }
    
//     private void requestPermissions() {
//         ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
//     }
    
//     @Override
//     public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//         super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
//         if (requestCode == PERMISSION_REQUEST_CODE) {
//             boolean allGranted = true;
//             for (int result : grantResults) {
//                 if (result != PackageManager.PERMISSION_GRANTED) {
//                     allGranted = false;
//                     break;
//                 }
//             }
            
//             if (allGranted) {
//                 checkDefaultSmsApp();
//             } else {
//                 showPermissionDeniedDialog();
//             }
//         }
//     }
    
//     private void showPermissionDeniedDialog() {
//         new AlertDialog.Builder(this)
//             .setTitle("Permissions Required")
//             .setMessage("This app needs SMS and Contacts permissions to function properly.")
//             .setPositiveButton("Grant", (dialog, which) -> requestPermissions())
//             .setNegativeButton("Exit", (dialog, which) -> finish())
//             .setCancelable(false)
//             .show();
//     }
    
//     private void checkDefaultSmsApp() {
//         if (!isDefaultSmsApp()) {
//             showSetDefaultSmsDialog();
//         } else {
//             loadSmsMessages();
//         }
//     }
    
//     private boolean isDefaultSmsApp() {
//         return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
//     }
    
//     private void showSetDefaultSmsDialog() {
//         new AlertDialog.Builder(this)
//             .setTitle("Set as Default SMS App")
//             .setMessage("To receive and send SMS messages, this app needs to be set as your default SMS app.")
//             .setPositiveButton("Set Default", (dialog, which) -> requestDefaultSmsApp())
//             .setNegativeButton("Cancel", (dialog, which) -> {
//                 Toast.makeText(this, "App requires default SMS permission to function", Toast.LENGTH_LONG).show();
//                 loadSmsMessages(); // Still allow reading existing messages
//             })
//             .setCancelable(false)
//             .show();
//     }
    
//     private void requestDefaultSmsApp() {
//         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//             RoleManager roleManager = getSystemService(RoleManager.class);
//             if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
//                 Intent roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
//                 startActivityForResult(roleRequestIntent, DEFAULT_SMS_REQUEST_CODE);
//             }
//         } else {
//             Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
//             intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
//             startActivityForResult(intent, DEFAULT_SMS_REQUEST_CODE);
//         }
//     }
    
//     @Override
//     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//         super.onActivityResult(requestCode, resultCode, data);
        
//         if (requestCode == DEFAULT_SMS_REQUEST_CODE) {
//             if (isDefaultSmsApp()) {
//                 Toast.makeText(this, "App is now the default SMS app", Toast.LENGTH_SHORT).show();
//             } else {
//                 Toast.makeText(this, "App was not set as default SMS app", Toast.LENGTH_SHORT).show();
//             }
//             loadSmsMessages();
//         }
//     }
    
//     private void loadSmsMessages() {
//         smsList.clear();
        
//         if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
//             Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show();
//             return;
//         }
        
//         Uri smsUri = Uri.parse("content://sms/");
//         String[] projection = {
//             Telephony.Sms._ID,
//             Telephony.Sms.ADDRESS,
//             Telephony.Sms.BODY,
//             Telephony.Sms.DATE,
//             Telephony.Sms.TYPE,
//             Telephony.Sms.READ
//         };
        
//         try (Cursor cursor = getContentResolver().query(
//             smsUri, 
//             projection, 
//             null, 
//             null, 
//             Telephony.Sms.DATE + " DESC"
//         )) {
            
//             if (cursor != null && cursor.moveToFirst()) {
//                 Map<String, List<SmsModel>> conversations = new HashMap<>();
                
//                 do {
//                     long id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
//                     String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
//                     String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
//                     long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
//                     int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));
//                     boolean isRead = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1;
                    
//                     String contactName = getContactName(address);
                    
//                     SmsModel sms = new SmsModel(id, address, body, date, type, isRead, contactName);
                    
//                     // Group by address for conversations
//                     conversations.computeIfAbsent(address, k -> new ArrayList<>()).add(sms);
                    
//                 } while (cursor.moveToNext());
                
//                 // Add the latest message from each conversation
//                 for (List<SmsModel> conversation : conversations.values()) {
//                     if (!conversation.isEmpty()) {
//                         // Sort by date and take the latest
//                         Collections.sort(conversation, (a, b) -> Long.compare(b.getDate(), a.getDate()));
//                         smsList.add(conversation.get(0));
//                     }
//                 }
                
//                 // Sort all conversations by latest message date
//                 Collections.sort(smsList, (a, b) -> Long.compare(b.getDate(), a.getDate()));
//             }
//         } catch (Exception e) {
//             Log.e(TAG, "Error loading SMS messages", e);
//             Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show();
//         }
        
//         smsAdapter.notifyDataSetChanged();
//         Log.d(TAG, "Loaded " + smsList.size() + " SMS conversations");
//     }
    
//     private String getContactName(String phoneNumber) {
//         if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
//             return phoneNumber;
//         }
        
//         Uri uri = Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, 
//             Uri.encode(phoneNumber));
//         String[] projection = {android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME};
        
//         try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
//             if (cursor != null && cursor.moveToFirst()) {
//                 return cursor.getString(0);
//             }
//         } catch (Exception e) {
//             Log.e(TAG, "Error getting contact name", e);
//         }
        
//         return phoneNumber;
//     }
    
//     @Override
//     protected void onResume() {
//         super.onResume();
//         if (hasAllPermissions()) {
//             loadSmsMessages();
//         }
//     }
    
//     @Override
//     protected void onDestroy() {
//         super.onDestroy();
//         if (smsRefreshReceiver != null) {
//             unregisterReceiver(smsRefreshReceiver);
//         }
//     }
    
//     public void refreshMessages() {
//         loadSmsMessages();
//     }
// }


// uppar wala ok hai but new modification laa rhae hai . uppar wala old defautl sms k liye ok hai 


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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int DEFAULT_SMS_REQUEST_CODE = 101;
    private static final String PREFS_NAME = "SmsAppPrefs";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    
    private RecyclerView recyclerView;
    private SmsAdapter smsAdapter;
    private List<SmsModel> smsList;
    private FloatingActionButton fabCompose;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private BroadcastReceiver smsRefreshReceiver;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    
    private final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupSmsRefreshReceiver();
        setupSwipeRefresh();
        checkPermissionsAndSetupApp();
        
        // Start the monitoring service
        startSmsMonitorService();
        
        // Setup periodic refresh as backup
        setupPeriodicRefresh();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewSms);
        fabCompose = findViewById(R.id.fabCompose);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        smsList = new ArrayList<>();
        smsAdapter = new SmsAdapter(this, smsList);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(smsAdapter);
        
        fabCompose.setOnClickListener(v -> {
            Intent intent = new Intent(this, ComposeActivity.class);
            startActivity(intent);
        });
    }
    
    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadSmsMessages();
                swipeRefreshLayout.setRefreshing(false);
            });
            
            swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            );
        }
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
                    
                    // Add small delay to ensure database is updated
                    new Handler().postDelayed(() -> {
                        loadSmsMessages();
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }, 200);
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
    
    private void setupPeriodicRefresh() {
        refreshHandler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (hasAllPermissions() && isDefaultSmsApp()) {
                    loadSmsMessages();
                }
                // Refresh every 30 seconds as backup
                refreshHandler.postDelayed(this, 30000);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 30000);
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
        // Add notification permission for Android 13+
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
                loadSmsMessages(); // Still show existing messages
            }
        } else {
            loadSmsMessages();
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
                loadSmsMessages(); // Still allow reading existing messages
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
            loadSmsMessages();
        }
    }
    
    private void loadSmsMessages() {
        new Thread(() -> {
            List<SmsModel> newSmsList = loadSmsFromDatabase();
            
            runOnUiThread(() -> {
                smsList.clear();
                smsList.addAll(newSmsList);
                smsAdapter.notifyDataSetChanged();
                
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                
                Log.d(TAG, "UI updated with " + smsList.size() + " SMS conversations");
            });
        }).start();
    }
    
    private List<SmsModel> loadSmsFromDatabase() {
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
            Telephony.Sms.DATE + " DESC LIMIT 1000"  // Limit for performance
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
    
    @Override
    protected void onResume() {
        super.onResume();
        if (hasAllPermissions()) {
            loadSmsMessages();
        }
        
        // Handle incoming intent (from notification)
        handleIncomingIntent();
    }
    
    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("sender")) {
            String sender = intent.getStringExtra("sender");
            Log.d(TAG, "Opened from notification for sender: " + sender);
            
            // Optionally scroll to the conversation or highlight it
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
        
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Don't stop the service when pausing - we want background monitoring
    }
    
    public void refreshMessages() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        loadSmsMessages();
    }
    
    // Method to handle deep linking or external intents
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent();
    }
}