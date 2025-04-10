// SaveStepsWorker.kt
package com.example.myfit_new.workers

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myfit_new.DataStoreSingleton.dataStore
import com.example.myfit_new.database.StepDatabaseHelper
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Kotlin
class SaveStepsWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val preferences = context.dataStore.data.first()
            val currentSteps = preferences[intPreferencesKey("step_count")] ?: 0

            // Get current date and time
            val now = LocalDateTime.now()
            // If the current time is within 10 minutes after midnight,
            // consider it as part of the previous day.
            val adjustedDate = if (now.toLocalTime() < LocalTime.of(0, 13)) {
                LocalDate.now().minusDays(1)
            } else {
                LocalDate.now()
            }
            val formattedDate = adjustedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            val dbHelper = StepDatabaseHelper(context)
            dbHelper.saveSteps(formattedDate, currentSteps)
            dbHelper.close()

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}