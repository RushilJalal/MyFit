package com.example.myfit;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String PREFS_NAME = "StepCounterPrefs";
    private static final String LAST_RESET_DATE_KEY = "lastResetDate";

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private TextView stepsTextView;
    private Button resetButton;
    private int stepsAtReset = 0;  // Tracks steps when reset is pressed
    private int totalSteps = 0;
    private SharedPreferences sharedPreferences;

    private static final int ACTIVITY_RECOGNITION_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stepsTextView = findViewById(R.id.stepsTextView);
        resetButton = findViewById(R.id.resetButton);

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

        // Reset button functionality
        resetButton.setOnClickListener(v -> {
            stepsAtReset = totalSteps;  // Save the current steps as the reset point
            updateStepDisplay();
        });

//        checkForNewDay();
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
