package com.example.mvpauthenticatorjava.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.mvpauthenticatorjava.R;

public class MyService extends Service {

    public static final String TAG = MyService.class.getSimpleName();

    public static final String NOTIFICATION_CHANNEL_ID = "mvp_result_channel";

    public static final String ACTION_MVP_RESULT = "com.example.mvpauthenticatorjava.MVP_RESULT";

    public static final String EXTRA_STATUS = "status";

    /**
     * Called by the system when the service is first created.
     * This is where you should do one-time setup and start the foreground process.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created.");
        // Immediately promote the service to a foreground service to avoid crashes.
        startForegroundWithNotification();
    }

    /**
     * Called by the system every time a client starts the service.
     * This is where the service does its main work.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand received.");

        if (intent != null) {
            String status = intent.getStringExtra(EXTRA_STATUS);
            Log.d(TAG, "Received verification result from MVP app: " + status);

            // 1. Broadcast the result to any listening UI components.
            sendResultBroadcast(status);

            // 2. Update the notification and stop the service.
            String contentText = (status != null) ? "Result received: " + status : "Result received: No status";
            updateNotification(contentText);
        }

        // START_NOT_STICKY tells the system not to recreate the service if it's killed.
        return START_NOT_STICKY;
    }

    /**
     * This service is "started", not "bound", so this method returns null.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called by the system when the service is no longer used and is being destroyed.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service Destroyed.");
        super.onDestroy();
    }

    /**
     * Sends the received status via a broadcast.
     */
    private void sendResultBroadcast(String status) {
        Intent broadcastIntent = new Intent(ACTION_MVP_RESULT);
        broadcastIntent.putExtra(EXTRA_STATUS, (status != null) ? status : "No result data");
        sendBroadcast(broadcastIntent);
        Log.d(TAG, "Result broadcast sent.");
    }

    /**
     * Creates the notification and promotes the service to the foreground.
     */
    @SuppressLint("ForegroundServiceType")
    private void startForegroundWithNotification() {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Authenticator App")
                .setContentText("Waiting for MVP app result...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        // The foregroundServiceType must also be declared in the AndroidManifest.xml for this service.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1, notification);
        }
    }

    /**
     * Updates the persistent notification with the final result and then stops the service.
     */
    private void updateNotification(String contentText) {
        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("MVP Result")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, notification);
        }
        stopSelf();
    }

    /**
     * Creates the Notification Channel required for Android 8.0 (Oreo) and above.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "MVP App Results",
                    NotificationManager.IMPORTANCE_HIGH // Use HIGH to make it pop up
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}

