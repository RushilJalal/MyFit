package com.example.myfit;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.lzyzsd.circleprogress.CircleProgress;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String PREFS_NAME = "StepCounterPrefs";
    private static final String LAST_RESET_DATE_KEY = "lastResetDate";
    private static final String STEPS_AT_RESET_KEY = "stepsAtReset";
    private static final String STEP_GOAL_KEY = "stepGoal";

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private TextView stepsTextView;
    private int stepsAtReset = 0;  // Tracks steps when reset is pressed
    private int totalSteps = 0;
    private int stepGoal = 10000; // Default step goal
    private SharedPreferences sharedPreferences;
    private Handler handler;
    private Runnable runnable;

    private static final int ACTIVITY_RECOGNITION_PERMISSION_CODE = 100;
    private static final long CHECK_INTERVAL = 60000; // 1 minute

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        stepsTextView = findViewById(R.id.stepsTextView);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load step goal from SharedPreferences
        stepGoal = sharedPreferences.getInt(STEP_GOAL_KEY, 10000);

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

        CircleProgress circleProgress = findViewById(R.id.circle_progress);
        if (stepGoal <= stepsToday) {
            circleProgress.setProgress(100);
        } else {
            circleProgress.setProgress((int) ((stepsToday / (double) stepGoal) * 100)); // Use step goal
        }

        // Update the motivational message
        updateMotivationalMessage(stepsToday);
    }

    private void updateMotivationalMessage(int stepsToday) {
        TextView motivationalMessage = findViewById(R.id.motivationalMessage);

        if (stepsToday < stepGoal / 3) {
            motivationalMessage.setText("You can do it!");
        } else if (stepsToday < 2 * stepGoal / 3) {
            motivationalMessage.setText("You're halfway there!");
        } else {
            motivationalMessage.setText("You're almost there!");
        }
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

    private void saveStepGoal() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(STEP_GOAL_KEY, stepGoal);
        editor.apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTIVITY_RECOGNITION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, do nothing
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_step_goal) {
            showStepGoalDialog();
            return true;
        } else if (itemId == R.id.action_settings) {// Handle settings action
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showStepGoalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Step Goal");

        final NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(1000);
        numberPicker.setMaxValue(50000);
        numberPicker.setValue(stepGoal);
        numberPicker.setWrapSelectorWheel(false);

        builder.setView(numberPicker);

        builder.setPositiveButton("OK", (dialog, which) -> {
            stepGoal = numberPicker.getValue();
            saveStepGoal();
            updateStepDisplay();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }


}