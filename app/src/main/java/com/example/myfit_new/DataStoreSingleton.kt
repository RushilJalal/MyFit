// DataStoreSingleton.kt
package com.example.myfit_new

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

object DataStoreSingleton {
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "step_preferences")
}