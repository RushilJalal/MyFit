package com.example.myfit; // Replace with your package name

import android.app.Application;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Schedule the worker to run daily at midnight
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0); // Midnight
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        long delay = calendar.getTimeInMillis() - System.currentTimeMillis();
        if (delay < 0) {
            delay += 24 * 60 * 60 * 1000; // Add 24 hours if the time has already passed
        }

        PeriodicWorkRequest dateCheckWorkRequest = new PeriodicWorkRequest.Builder(
                DateCheckWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(this).enqueue(dateCheckWorkRequest);
    }
}