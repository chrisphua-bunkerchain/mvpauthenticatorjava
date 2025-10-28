package com.example.mvpauthenticatorjava.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This receiver acts as the secure entry point for the MVP app's result.
 * Its only job is to receive the result and broadcast it internally to the app's UI.
 */
public class ExternalReceiver extends BroadcastReceiver {

    // In Java, constants are defined as `public static final` fields.
    // They are equivalent to the Kotlin `companion object` constants.
    public static final String ACTION_MVP_RESULT = "com.example.mvpauthenticatorkotlin.ACTION_MVP_RESULT";
    public static final String RESULT = "result";

    @Override
    public void onReceive(Context context, Intent intent) {
        String result = null;
        if (intent != null) {
            result = intent.getStringExtra("result");
        }

        Log.d(
                "MvpResultReceiver", // Using the same log tag for consistency
                "Received result from MVP app. Broadcasting to UI. Result: " + result
        );

        // Create a new intent to send to UI (FirstFragment)
        Intent uiIntent = new Intent(ACTION_MVP_RESULT);

        // Use a null check instead of the Elvis operator
        String resultData = (result != null) ? result : "No result data";
        uiIntent.putExtra(RESULT, resultData);

        // Send the broadcast directly to the UI.
        context.sendBroadcast(uiIntent);
    }
}
