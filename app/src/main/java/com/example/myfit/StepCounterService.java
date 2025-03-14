package com.example.myfit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.util.Calendar;

public class StepCounterService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        prefs = getSharedPreferences("StepCounterPrefs", MODE_PRIVATE);

        // Register the step counter sensor
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Set up midnight alarm for reset
        setMidnightAlarm();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            long steps = (long) event.values[0];
            long lastResetTime = prefs.getLong("lastResetTime", 0);
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastResetTime > 24 * 60 * 60 * 1000) {
                // More than 24 hours since last reset, perform reset
                resetStepCount(steps);
            } else {
                updateStepCount(steps);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void resetStepCount(long initialSteps) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("initialSteps", initialSteps);
        editor.putLong("lastResetTime", System.currentTimeMillis());
        editor.apply();
    }

    private void updateStepCount(long currentSteps) {
        long initialSteps = prefs.getLong("initialSteps", 0);
        long stepsSinceReset = currentSteps - initialSteps;
        // Update your UI or database with stepsSinceReset
    }

    private void setMidnightAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MidnightResetReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Set calendar to next midnight
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        // Schedule the alarm to repeat every day
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);
    }

}
