package com.example.caisse.model

import android.app.Application
import androidx.room.Room
import com.example.caisse.data.CaisseDataBase

class App : Application()  {

    lateinit var database: CaisseDataBase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, CaisseDataBase::class.java, "salon_database")
            .allowMainThreadQueries()
            // NF525 Axe C : pas de migration destructive — les migrations sont gérées dans CaisseDataBase
            .addMigrations(*com.example.caisse.data.CaisseDataBase.getMigrations())
            .build()
    }
}