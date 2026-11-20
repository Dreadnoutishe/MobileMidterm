package com.example.marscolonymanager.data

import androidx.room.withTransaction
import com.example.marscolonymanager.model.ColonyState

class ColonyRepository(private val database: ColonyDatabase) {
    private val dao = database.expedition()

    suspend fun loadColony(): ColonyState? = dao.loadExpedition()?.toColonyState()

    suspend fun saveColony(state: ColonyState) {
        database.withTransaction {
            val expedition = ExpeditionEntity(state)
            val reports = state.reports.map { MissionReportEntity(it, expedition.id) }
            dao.saveExpedition(expedition)
            if (dao.loadReports(expedition.id) != reports) {
                dao.deleteReports(expedition.id)
                if (reports.isNotEmpty()) dao.insertReports(reports)
            }
        }
    }
}