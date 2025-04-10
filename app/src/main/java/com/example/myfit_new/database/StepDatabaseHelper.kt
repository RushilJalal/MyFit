package com.example.myfit_new.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StepDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    object StepEntry : BaseColumns {
        const val TABLE_NAME = "daily_steps"
        const val COLUMN_DATE = "date"
        const val COLUMN_STEP_COUNT = "step_count"
    }

    companion object {
        const val DATABASE_NAME = "steps.db"
        const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        val SQL_CREATE_ENTRIES = """
            CREATE TABLE ${StepEntry.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY,
                ${StepEntry.COLUMN_DATE} TEXT,
                ${StepEntry.COLUMN_STEP_COUNT} INTEGER
            )
        """.trimIndent()
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${StepEntry.TABLE_NAME}")
        onCreate(db)
    }

    fun saveSteps(date: String, steps: Int) {
        val db = writableDatabase

        Log.d("StepDatabaseHelper", "Saving steps for date: $date, steps: $steps")

        // Check if entry for this date already exists
        val cursor = db.query(
            StepEntry.TABLE_NAME,
            arrayOf(BaseColumns._ID),
            "${StepEntry.COLUMN_DATE} = ?",
            arrayOf(date),
            null, null, null
        )

        val values = ContentValues().apply {
            put(StepEntry.COLUMN_DATE, date)
            put(StepEntry.COLUMN_STEP_COUNT, steps)
        }

        if (cursor.moveToFirst()) {
            // Update existing entry
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
            db.update(
                StepEntry.TABLE_NAME,
                values,
                "${BaseColumns._ID} = ?",
                arrayOf(id.toString())
            )
            Log.d("StepDatabaseHelper", "Updated steps for date: $date, steps: $steps")
        } else {
            // Insert new entry
            db.insert(StepEntry.TABLE_NAME, null, values)
            Log.d("StepDatabaseHelper", "Inserted steps for date: $date, steps: $steps")
        }

        cursor.close()
    }

    fun getStepHistory(days: Int = 7): List<Pair<String, Int>> {
        val db = readableDatabase
        val stepsList = mutableListOf<Pair<String, Int>>()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(days.toLong() - 1)

        val selection = "${StepEntry.COLUMN_DATE} >= ?"
        val selectionArgs = arrayOf(startDate.format(formatter))
        val sortOrder = "${StepEntry.COLUMN_DATE} ASC"

        val cursor = db.query(
            StepEntry.TABLE_NAME,
            arrayOf(StepEntry.COLUMN_DATE, StepEntry.COLUMN_STEP_COUNT),
            selection,
            selectionArgs,
            null, null,
            sortOrder
        )

        while (cursor.moveToNext()) {
            val date = cursor.getString(cursor.getColumnIndexOrThrow(StepEntry.COLUMN_DATE))
            val steps = cursor.getInt(cursor.getColumnIndexOrThrow(StepEntry.COLUMN_STEP_COUNT))
            stepsList.add(Pair(date, steps))
            Log.d("StepDatabaseHelper", "Retrieved steps for date: $date, steps: $steps")
        }

        cursor.close()
        Log.d("StepDatabaseHelper", "Total steps retrieved: ${stepsList.size}")
        return stepsList
    }

//    fun updateSteps(date: String, steps: Int) {
//        val db = writableDatabase
//        val values = ContentValues().apply {
//            put(StepEntry.COLUMN_STEP_COUNT, steps)
//        }
//        db.update(StepEntry.TABLE_NAME, values, "${StepEntry.COLUMN_DATE} = ?", arrayOf(date))
//    }
//
//    fun deleteSteps(date: String) {
//        val db = writableDatabase
//        db.delete(StepEntry.TABLE_NAME, "${StepEntry.COLUMN_DATE} = ?", arrayOf(date))
//    }
}
