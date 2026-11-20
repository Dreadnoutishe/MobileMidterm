package com.example.marscolonymanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkOrderEntity::class,
        WorkOrderEventEntity::class,
        ExpeditionEntity::class,
        MissionReportEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ColonyDatabase : RoomDatabase() {
    internal abstract fun workOrders(): WorkOrderDao
    internal abstract fun expedition(): ExpeditionDao

    companion object {
        @Volatile
        private var instance: ColonyDatabase? = null

        fun getInstance(context: Context): ColonyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ColonyDatabase::class.java,
                    "mars-colony.db"
                ).build().also { instance = it }
            }
    }
}