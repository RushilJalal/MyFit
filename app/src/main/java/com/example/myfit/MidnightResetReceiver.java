package com.example.myfit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class MidnightResetReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Reset step count here
        SharedPreferences prefs = context.getSharedPreferences("StepCounterPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("initialSteps", 0);
        editor.putLong("lastResetTime", System.currentTimeMillis());
        editor.apply();

        // You might also want to update your UI or database here
    }
}
