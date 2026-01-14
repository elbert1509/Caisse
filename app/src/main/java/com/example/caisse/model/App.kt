package com.example.piece.model

import android.app.Application
import androidx.room.Room
import com.example.piece.data.CaisseDataBase

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