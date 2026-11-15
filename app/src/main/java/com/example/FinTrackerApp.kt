package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.FinTrackerRepository
import com.example.data.UserPreferencesRepository
import com.example.ml.DiasporaMlClassifier

class FinTrackerApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val prefsRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val classifier: DiasporaMlClassifier by lazy { DiasporaMlClassifier() }
    val repository: FinTrackerRepository by lazy {
        FinTrackerRepository(
            context = this,
            db = database,
            prefs = prefsRepository,
            classifier = classifier,
        )
    }
}
