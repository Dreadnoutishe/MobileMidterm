package com.example.marscolonymanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.marscolonymanager.model.*
import com.example.marscolonymanager.ui.theme.*

@Composable
fun MissionCard(mission: Mission, colony: ColonyState, member: CrewMember, onLaunch: () -> Unit) {
    val result = remember(colony, mission, member) { ColonyRules.forecast(colony, mission, member) }
    val problem = ColonyRules.launchProblem(colony, mission, member)
    val accent = missionColor(mission.id)
    var confirmRisk by remember { mutableStateOf(false) }
    ColonyPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(missionIcon(mission.id), null, tint = accent, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(mission.name, style = MaterialTheme.typography.titleLarge)
                Text(mission.location, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
        Text(mission.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        val reward = when (mission.id) { 1 -> "${mission.oxygenReward.toInt()} oxygen"; 2 -> "${mission.energyReward.toInt()} energy"; else -> "${mission.foodReward.toInt()} food" }
        Text("+$reward${if (result.specialistBonus) " + 8 specialist bonus" else ""}", color = accent, style = MaterialTheme.typography.titleMedium)
        Text("Launch cost: ${mission.energyCost.toInt()} energy · ${ColonyRules.durationSeconds(member)} seconds", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(if (result.specialistBonus) "${member.name} matches ${mission.preferredSkill}: +8 supplies, +25 research."
            else if (member.isPilot) "Pilot advantage: 3-second missions and +25 research."
            else "A ${mission.preferredSkill} specialist would earn +8 supplies and +25 research.", color = if (result.specialistBonus || member.isPilot) accent else TextSecondary, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(color = SpaceOutline)
        ResourceForecast(colony, result)
        Text("+${result.researchGain} research", color = MarsOrange, style = MaterialTheme.typography.labelLarge)
        if (problem != null) Text(problem, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        else if (result.causesFailure) Text("This mission would deplete a vital resource.", color = CriticalColor, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { if (result.causesFailure) confirmRisk = true else onLaunch() }, enabled = problem == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(14.dp)) {
            Text(if (colony.missionInProgress) "Mission running" else "Launch with ${member.name}")
        }
    }
    if (confirmRisk) AlertDialog(onDismissRequest = { confirmRisk = false }, title = { Text("This mission will end the colony") },
        text = { Text("The forecast shows at least one resource reaching zero after daily consumption. Choose another mission or leader to protect the colony.") },
        confirmButton = { TextButton(onClick = { confirmRisk = false; onLaunch() }) { Text("Launch anyway") } },
        dismissButton = { TextButton(onClick = { confirmRisk = false }) { Text("Review choice") } })
}
