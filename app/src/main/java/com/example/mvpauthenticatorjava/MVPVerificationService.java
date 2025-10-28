package com.example.mvpauthenticatorjava;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.example.mvpauthenticatorjava.service.ExternalReceiver;

import java.util.List;

public final class MVPVerificationService {
    private static final String TAG = MVPVerificationService.class.getSimpleName();

    private static final String MVP_APP_SCHEME = "bunkerchain";

    public static final String MVP_APP_PACKAGE = "com.bunkerchain.mvp_app";

    public static final String MVP_APP_SPLASH = "com.bunkerchain.mvp_app.main.SplashActivity";

    public static final String MVP_APP_SERVICE = "com.bunkerchain.mvp_app.main.TokenProcessingService";

    /**
     * Check if MVP app is installed
     */
    public static boolean checkMvpAppInstalled(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(MVP_APP_PACKAGE);

        List<ResolveInfo> apps = packageManager.queryIntentActivities(intent, 0);
        if (apps.isEmpty()) {
            Log.w(TAG, "MVP app not found. Check package name and <queries> in manifest.");
            Toast.makeText(context, "MVP app not installed.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    public static void authenticate(Context context, String token) {
        String myPackageName = context.getPackageName();
        Intent intent = new Intent();
        intent.putExtra("token", token);
        intent.putExtra("packageName", myPackageName);

        ComponentName component = new ComponentName(
                MVP_APP_PACKAGE,
                MVP_APP_SERVICE
        );
        intent.setComponent(component);

        try {
            Log.d(TAG, "Attempting to start MVP app service...");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
            Log.d(TAG, "Service intent sent successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start MVP app service.", e);
            Toast.makeText(context, "Could not start MVP app service.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("HardwareIds")
    public static void authenticate(Context context, String imoNumber, String code) {
        String packageName = context.getPackageName();
        String deviceCode = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        var uri = new Uri.Builder().scheme(MVP_APP_SCHEME)
                .authority("verify")
                .appendQueryParameter("appName", "mvpauthenticatorjava")
                .appendQueryParameter("deviceCode", deviceCode)
                .appendQueryParameter("imoNumber", imoNumber)
                .appendQueryParameter("code", code)
                .appendQueryParameter("packageName", packageName)
                .appendQueryParameter("action", ExternalReceiver.ACTION_MVP_RESULT)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addCategory(Intent.CATEGORY_DEFAULT);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ComponentName component = new ComponentName(
                MVP_APP_PACKAGE,
                MVP_APP_SPLASH
        );
        intent.setComponent(component);

        try {
            Log.d(TAG, "Attempting to start MVP app activity...");
            context.startActivity(intent);
            Log.d(TAG, "Activity start intent sent successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start MVP app activity. Is it installed?", e);
            Toast.makeText(context, "Could not start MVP app.", Toast.LENGTH_SHORT).show();
        }
    }
}

