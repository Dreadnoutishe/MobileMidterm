package com.example.marscolonymanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
internal interface ExpeditionDao {
    @Transaction
    @Query("SELECT * FROM expedition WHERE id = 1")
    suspend fun loadExpedition(): ExpeditionWithReports?

    @Upsert
    suspend fun saveExpedition(expedition: ExpeditionEntity)

    @Query("SELECT * FROM mission_reports WHERE expeditionId = :id ORDER BY sol")
    suspend fun loadReports(id: Int): List<MissionReportEntity>

    @Query("DELETE FROM mission_reports WHERE expeditionId = :id")
    suspend fun deleteReports(id: Int)

    @Insert
    suspend fun insertReports(reports: List<MissionReportEntity>)
}