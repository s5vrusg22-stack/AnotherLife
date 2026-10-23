package com.anotherlife.app

import android.app.Application
import androidx.room.Room
import com.anotherlife.app.data.AnotherLifeDatabase
import com.anotherlife.app.data.WorldBackupRestore

class AnotherLifeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WorldBackupRestore.applyOnColdStart(this)
    }
    val database: AnotherLifeDatabase by lazy {
        Room.databaseBuilder(this, AnotherLifeDatabase::class.java, "another_life.db").addMigrations(AnotherLifeDatabase.MIGRATION_1_2, AnotherLifeDatabase.MIGRATION_2_3, AnotherLifeDatabase.MIGRATION_3_4, AnotherLifeDatabase.MIGRATION_4_5, AnotherLifeDatabase.MIGRATION_5_6, AnotherLifeDatabase.MIGRATION_6_7, AnotherLifeDatabase.MIGRATION_7_8, AnotherLifeDatabase.MIGRATION_8_9, AnotherLifeDatabase.MIGRATION_9_10, AnotherLifeDatabase.MIGRATION_10_11, AnotherLifeDatabase.MIGRATION_11_12).build()
    }
}
