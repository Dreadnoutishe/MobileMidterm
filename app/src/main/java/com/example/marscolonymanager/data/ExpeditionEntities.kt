package com.example.marscolonymanager.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.marscolonymanager.model.ColonyState
import com.example.marscolonymanager.model.MissionReport

@Entity(tableName = "expedition")
internal data class ExpeditionEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val sol: Int,
    val oxygen: Float,
    val energy: Float,
    val food: Float,
    val researchPoints: Int,
    val completedSols: Int,
    val restingCrewId: Int?,
    val activeMissionId: Int?,
    val activeCrewId: Int?,
    val progressSeconds: Int
) {
    @Ignore
    constructor(state: ColonyState) : this(
        id = 1,
        name = state.name,
        sol = state.sol,
        oxygen = state.oxygen,
        energy = state.energy,
        food = state.food,
        researchPoints = state.researchPoints,
        completedSols = state.completedSols,
        restingCrewId = state.restingCrewId,
        activeMissionId = state.activeMissionId,
        activeCrewId = state.activeCrewId,
        progressSeconds = state.progressSeconds
    )
}

@Entity(
    tableName = "mission_reports",
    foreignKeys = [
        ForeignKey(
            entity = ExpeditionEntity::class,
            parentColumns = ["id"],
            childColumns = ["expeditionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("expeditionId")]
)
internal data class MissionReportEntity(
    @PrimaryKey val sol: Int,
    val expeditionId: Int,
    val missionName: String,
    val crewName: String,
    val oxygenBefore: Float,
    val energyBefore: Float,
    val foodBefore: Float,
    val oxygenAfter: Float,
    val energyAfter: Float,
    val foodAfter: Float,
    val researchGain: Int,
    val specialistBonus: Boolean
) {
    @Ignore
    constructor(report: MissionReport, expeditionId: Int) : this(
        sol = report.sol,
        expeditionId = expeditionId,
        missionName = report.missionName,
        crewName = report.crewName,
        oxygenBefore = report.oxygenBefore,
        energyBefore = report.energyBefore,
        foodBefore = report.foodBefore,
        oxygenAfter = report.oxygenAfter,
        energyAfter = report.energyAfter,
        foodAfter = report.foodAfter,
        researchGain = report.researchGain,
        specialistBonus = report.specialistBonus
    )

    fun toMissionReport() = MissionReport(
        sol = sol,
        missionName = missionName,
        crewName = crewName,
        oxygenBefore = oxygenBefore,
        energyBefore = energyBefore,
        foodBefore = foodBefore,
        oxygenAfter = oxygenAfter,
        energyAfter = energyAfter,
        foodAfter = foodAfter,
        researchGain = researchGain,
        specialistBonus = specialistBonus
    )
}

internal data class ExpeditionWithReports(
    @Embedded val expedition: ExpeditionEntity,
    @Relation(parentColumn = "id", entityColumn = "expeditionId")
    val reports: List<MissionReportEntity>
) {
    fun toColonyState() = ColonyState(
        name = expedition.name,
        sol = expedition.sol,
        oxygen = expedition.oxygen,
        energy = expedition.energy,
        food = expedition.food,
        researchPoints = expedition.researchPoints,
        completedSols = expedition.completedSols,
        restingCrewId = expedition.restingCrewId,
        activeMissionId = expedition.activeMissionId,
        activeCrewId = expedition.activeCrewId,
        progressSeconds = expedition.progressSeconds,
        reports = reports.sortedBy { it.sol }.map { it.toMissionReport() }
    )
}