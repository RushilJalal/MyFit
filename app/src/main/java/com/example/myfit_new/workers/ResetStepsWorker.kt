// ResetStepsWorker.kt
package com.example.myfit_new.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myfit_new.StepTracker

class ResetStepsWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val stepTracker = StepTracker(context)

            //fetch datastore and reset the step count
            stepTracker.resetSteps()

            Result.success()
        } catch (e: Exception) {
            //log the exception
            Log.e("ResetStepsWorker", "Error resetting steps", e)
            Result.failure()
        }
    }
}