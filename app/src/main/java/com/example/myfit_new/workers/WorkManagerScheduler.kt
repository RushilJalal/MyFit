package com.example.myfit_new.workers

import android.content.Context
import androidx.work.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class WorkManagerScheduler {
    companion object {
        private const val SAVE_STEPS_WORK = "save_steps_work"
        private const val RESET_STEPS_WORK = "reset_steps_work"

        fun scheduleDailyTasks(context: Context) {
            scheduleStepSaving(context)
            scheduleStepReset(context)
        }

        private fun scheduleStepSaving(context: Context) {
            val now = LocalDateTime.now()
            val targetTime = LocalDateTime.of(
                now.toLocalDate(),
                LocalTime.of(23, 59)
            )

            // If it's already past 11:59 PM, schedule for tomorrow
            var timeUntilTarget = Duration.between(now, targetTime)
//            if (timeUntilTarget.isNegative) {
//                timeUntilTarget = Duration.between(
//                    now,
//                    targetTime.plusDays(1)
//                )
//            }

            // Create work request
            val saveWorkRequest = OneTimeWorkRequestBuilder<SaveStepsWorker>()
                .setInitialDelay(timeUntilTarget.toMillis(), TimeUnit.MILLISECONDS)
                .addTag(SAVE_STEPS_WORK)
                .build()

            // Schedule the work
            WorkManager.getInstance(context).enqueueUniqueWork(
                SAVE_STEPS_WORK,
                ExistingWorkPolicy.REPLACE,
                saveWorkRequest
            )

            // Schedule daily repeating work
            val dailySaveRequest = PeriodicWorkRequestBuilder<SaveStepsWorker>(
                1, TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "${SAVE_STEPS_WORK}_daily",
                ExistingPeriodicWorkPolicy.KEEP,
                dailySaveRequest
            )
        }

        private fun scheduleStepReset(context: Context) {
            val currentTime = java.util.Calendar.getInstance()
            val dueTime = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                if (before(currentTime)) add(java.util.Calendar.DAY_OF_MONTH, 1)
            }

            val initialDelay = dueTime.timeInMillis - currentTime.timeInMillis

            val resetWork = PeriodicWorkRequestBuilder<ResetStepsWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                RESET_STEPS_WORK,
                ExistingPeriodicWorkPolicy.REPLACE,
                resetWork
            )
        }
    }
}