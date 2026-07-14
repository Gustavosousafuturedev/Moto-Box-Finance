package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.Repository

class MainApplication : Application() {
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        repository = Repository(database)
    }
}
