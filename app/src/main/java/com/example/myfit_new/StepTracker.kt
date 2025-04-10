// StepTracker.kt
package com.example.myfit_new

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.myfit_new.DataStoreSingleton.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StepTracker(private val context: Context) : SensorEventListener {
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    init {
        if (stepSensor == null) {
            Log.e("StepTracker", "Step sensor not available on this device")
        }
    }

    private val _stepCount = MutableStateFlow(0)
    val stepCount = _stepCount.asStateFlow()

    private val stepCountKey = intPreferencesKey("step_count")
    private val initialStepKey = intPreferencesKey("initial_step")

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                context.dataStore.data.collect() { preferences ->
                    val savedSteps = preferences[stepCountKey] ?: 0
                    val savedInitialSteps = preferences[initialStepKey] ?: 0
                    _stepCount.value = savedSteps

                    Log.d("StepTracker", "Loaded saved steps: $savedSteps")
                    Log.d("StepTracker", "Loaded initial steps: $savedInitialSteps")
                }
            } catch (e: Exception) {
                Log.e("StepTracker", "Error loading saved steps", e)
            }
        }
    }

    fun startTracking() {
        stepSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d("StepTracker", "Step sensor registered")
        } ?: Log.e("StepTracker", "Step sensor not available on this device")
    }

    fun stopTracking() {
        sensorManager.unregisterListener(this)
        Log.d("StepTracker", "Step sensor unregistered")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val totalSteps = it.values[0].toInt()
                CoroutineScope(Dispatchers.IO).launch {
                    processStepCount(totalSteps)
                }
            }
        }
    }

//    private suspend fun processStepCount(totalSteps: Int) {
//        try {
//            val preferences = context.dataStore.data.first()
//            val savedInitialSteps = preferences[initialStepKey]
//
//            if (savedInitialSteps == null) {
//                context.dataStore.edit { prefs ->
//                    prefs[initialStepKey] = totalSteps
//                }
//                _stepCount.value = 0
//                Log.d("StepTracker", "Initialized step counter with baseline: $totalSteps")
//            } else {
//                val currentSteps = totalSteps - savedInitialSteps
//                _stepCount.value = currentSteps
//
//                context.dataStore.edit { prefs ->
//                    prefs[stepCountKey] = currentSteps
//                    //prefs[initialStepKey] = 43944
//                }
//                Log.d("StepTracker", "Current Step count: $currentSteps")
//                Log.d("StepTracker", "Total steps: $totalSteps")
//                Log.d("StepTracker", "Initial steps: $savedInitialSteps")
//                Log.d("StepTracker", "Updated step count: $currentSteps (saved to DataStore)")
//            }
//        } catch (e: Exception) {
//            Log.e("StepTracker", "Error processing step count", e)
//        }
//    }

    private suspend fun processStepCount(totalSteps: Int) {
        val preferences = context.dataStore.data.first()
        val savedInitialSteps = preferences[initialStepKey] ?: 0

        // Reset saved initial steps if current sensor reading is less
        if (totalSteps < savedInitialSteps) {
            context.dataStore.edit { prefs ->
                prefs[initialStepKey] = totalSteps
            }
            _stepCount.value = 0
            Log.d("StepTracker", "Reinitialized step counter with new baseline: $totalSteps")
            return
        }

        val currentSteps = totalSteps - savedInitialSteps
        _stepCount.value = currentSteps
        context.dataStore.edit { prefs ->
            prefs[stepCountKey] = currentSteps
        }
        Log.d("StepTracker", "Updated step count: \$currentSteps")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("StepTracker", "Accuracy changed: $accuracy")
    }

    fun resetSteps() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
//                val totalSteps = context.dataStore.data.first()[initialStepKey] ?: return@launch

                val totalSteps = _stepCount.value + (context.dataStore.data.first()[initialStepKey] ?: 0)
                context.dataStore.edit { preferences ->
                    preferences[initialStepKey] = totalSteps
                    preferences[stepCountKey] = 0
                }

                _stepCount.value = 0

                Log.d("StepTracker", "Step counter reset")


            } catch (e: Exception) {
                Log.e("StepTracker", "Error resetting steps", e)
            }
        }
    }
}