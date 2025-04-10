package com.example.myfit_new.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Enqueue the reset worker when the alarm fires
        val resetWorkerRequest = OneTimeWorkRequestBuilder<ResetStepsWorker>().build()
        WorkManager.getInstance(context).enqueue(resetWorkerRequest)
    }
}