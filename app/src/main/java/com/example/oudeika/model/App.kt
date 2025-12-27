package com.example.oudeika.model

import android.app.Application
import androidx.room.Room
import com.example.oudeika.data.CaisseDataBase

class App : Application()  {

    lateinit var database: CaisseDataBase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, CaisseDataBase::class.java, "salon_database").allowMainThreadQueries()
            .fallbackToDestructiveMigration(false)
            .build()
    }
}