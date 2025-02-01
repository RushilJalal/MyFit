package com.example.myfit;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String PREFS_NAME = "StepCounterPrefs";
    private static final String LAST_RESET_DATE_KEY = "lastResetDate";
    private static final String STEPS_AT_RESET_KEY = "stepsAtReset";

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private TextView stepsTextView;
    private Button resetButton;
    private int stepsAtReset = 0;  // Tracks steps when reset is pressed
    private int totalSteps = 0;
    private SharedPreferences sharedPreferences;
    private Handler handler;
    private Runnable runnable;

    private static final int ACTIVITY_RECOGNITION_PERMISSION_CODE = 100;
    private static final long CHECK_INTERVAL = 60000; // 1 minute

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stepsTextView = findViewById(R.id.stepsTextView);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // Check if step counter sensor is available
        if (stepCounterSensor == null) {
            Toast.makeText(this, "Step Counter Sensor Not Available", Toast.LENGTH_LONG).show();
            return;
        }

        // Request Activity Recognition Permission for Android 10+
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION},
                    ACTIVITY_RECOGNITION_PERMISSION_CODE);
        }

        // Load the last reset steps and date from SharedPreferences
        stepsAtReset = sharedPreferences.getInt(STEPS_AT_RESET_KEY, 0);
        String lastResetDate = sharedPreferences.getString(LAST_RESET_DATE_KEY, null);

        // Check if it's a new day
        checkForNewDay(lastResetDate);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            totalSteps = (int) event.values[0];
            if (stepsAtReset == 0) {
                stepsAtReset = totalSteps;  // Initialize on first sensor event
                saveStepsAtReset();
            }
            updateStepDisplay();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used for this implementation
    }

    private void updateStepDisplay() {
        int stepsToday = totalSteps - stepsAtReset;
        stepsTextView.setText(String.valueOf(stepsToday));
    }

    private void checkForNewDay(String lastResetDate) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (lastResetDate == null || !lastResetDate.equals(currentDate)) {
            // It's a new day, reset steps
            stepsAtReset = totalSteps;
            saveStepsAtReset();
            saveLastResetDate(currentDate);
            updateStepDisplay();
        }
    }

    private void saveStepsAtReset() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(STEPS_AT_RESET_KEY, stepsAtReset);
        editor.apply();
    }

    private void saveLastResetDate(String date) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LAST_RESET_DATE_KEY, date);
        editor.apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTIVITY_RECOGNITION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}