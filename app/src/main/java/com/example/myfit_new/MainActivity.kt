package com.example.myfit_new

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.myfit_new.ui.theme.BottomNavDemoTheme
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.WorkManager
import com.example.myfit_new.database.StepDatabaseHelper
import com.example.myfit_new.workers.ResetStepsWorker
import com.example.myfit_new.workers.WorkManagerScheduler
import androidx.work.*
import com.example.myfit_new.workers.AlarmReceiver
import java.util.concurrent.TimeUnit
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private val ACTIVITY_RECOGNITION_REQUEST_CODE = 1
    private lateinit var stepTracker: StepTracker

    @SuppressLint("ScheduleExactAlarm")
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stepTracker = StepTracker(this)
        setContent {
            BottomNavDemoTheme {
                MainScreen()
            }
        }

        checkAndRequestActivityRecognitionPermission()

        // Schedule daily tasks(steps saving and resetting)
        WorkManagerScheduler.scheduleDailyTasks(this)

        if (canScheduleExactAlarms()) {
            AlarmScheduler.scheduleMidnightAlarm(this)
        } else {
            // Launch settings page to let user grant exact alarm permission
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }

        // Log the contents of the database
        logDatabaseContents(this)

        // Log the current workers
        logCurrentWorkers(this)

        // Log the current alarms
        logCurrentAlarms()
    }

    private fun canScheduleExactAlarms(): Boolean {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    private fun checkAndRequestActivityRecognitionPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                ACTIVITY_RECOGNITION_REQUEST_CODE
            )
        }
    }

    private fun logCurrentAlarms() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            Log.d("AlarmManager", "Alarm is set with PendingIntent: $pendingIntent")
        } else {
            Log.d("AlarmManager", "No alarms are currently set.")
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    MaterialTheme(
        colorScheme = lightColorScheme()
    )
    {
        Scaffold(
            bottomBar = {
                BottomNavigationBar(navController = navController)
            }
        ) { paddingValues ->
            // Apply padding to the content to avoid overlap with the bottom bar
            Box(modifier = Modifier.padding(paddingValues)) {
                Navigation(navController = navController)
            }
        }
    }
}

fun logDatabaseContents(context: Context) {
    val dbHelper = StepDatabaseHelper(context)
    val db: SQLiteDatabase = dbHelper.readableDatabase

    val cursor: Cursor = db.query(
        StepDatabaseHelper.StepEntry.TABLE_NAME,
        arrayOf(
            StepDatabaseHelper.StepEntry.COLUMN_DATE,
            StepDatabaseHelper.StepEntry.COLUMN_STEP_COUNT
        ),
        null, null, null, null, null
    )

    if (cursor.moveToFirst()) {
        do {
            val date =
                cursor.getString(cursor.getColumnIndexOrThrow(StepDatabaseHelper.StepEntry.COLUMN_DATE))
            val steps =
                cursor.getInt(cursor.getColumnIndexOrThrow(StepDatabaseHelper.StepEntry.COLUMN_STEP_COUNT))
            Log.d("SQLiteDatabase", "Date: $date, Steps: $steps")
        } while (cursor.moveToNext())
    } else {
        Log.d("SQLiteDatabase", "No data found")
    }

    cursor.close()
    db.close()
}

fun logCurrentWorkers(context: Context) {
    val workManager = WorkManager.getInstance(context)

    // Query all work by tag
    val workInfosByTag = workManager.getWorkInfosByTagLiveData("save_steps_work")
    workInfosByTag.observeForever { workInfos ->
        for (workInfo in workInfos) {
            Log.d(
                "WorkManager",
                "Worker ID: ${workInfo.id}, State: ${workInfo.state}, Tags: ${workInfo.tags}"
            )
        }
    }

    // Query unique work
    val workInfosForUniqueWork =
        workManager.getWorkInfosForUniqueWorkLiveData("save_steps_work_daily")
    workInfosForUniqueWork.observeForever { workInfos ->
        for (workInfo in workInfos) {
            Log.d(
                "WorkManager",
                "Worker ID: ${workInfo.id}, State: ${workInfo.state}, Tags: ${workInfo.tags}"
            )
        }
    }

    //log steps reset worker
    val resetWorkInfosByTag = workManager.getWorkInfosByTagLiveData("reset_steps_work")
    resetWorkInfosByTag.observeForever { workInfos ->
        for (workInfo in workInfos) {
            Log.d(
                "WorkManager",
                "Worker ID: ${workInfo.id}, State: ${workInfo.state}, Tags: ${workInfo.tags}"
            )
        }
    }

    //if empty, log
    if (workInfosByTag.value.isNullOrEmpty() && workInfosForUniqueWork.value.isNullOrEmpty()) {
        Log.d("WorkManager", "No workers found")
    }
}

