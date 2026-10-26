package com.example.marscolonymanager.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.marscolonymanager.model.*
import com.example.marscolonymanager.ui.components.*
import com.example.marscolonymanager.ui.theme.*

@Composable
fun MissionsScreen(colony: ColonyState, missions: List<Mission>, crew: List<CrewMember>, selectedCrewId: Int,
    onSelectCrew: (Int) -> Unit, onLaunchMission: (Int, Int) -> Unit, onReports: () -> Unit) {
    val member = crew.find { it.id == selectedCrewId } ?: crew.first()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SectionLabel("SOL ${colony.sol} / PLAN YOUR NEXT MOVE", MarsOrange)
            Spacer(Modifier.height(8.dp))
            Text("Choose your mission.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Oxygen ${colony.oxygen.toInt()} · Energy ${colony.energy.toInt()} · Food ${colony.food.toInt()}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        if (colony.isFinished) item {
            ColonyPanel {
                Text(if (colony.hasWon) "The colony survived!" else "The expedition has ended.", style = MaterialTheme.typography.titleLarge)
                Text("Review the reports or use the restart button above to begin a new expedition.", color = TextSecondary)
                Button(onClick = onReports) { Text("View results") }
            }
        }
        if (colony.missionInProgress) item { MissionProgressCard(colony) }
        if (!colony.missionInProgress && !colony.isFinished && colony.reports.isNotEmpty()) item {
            Text("Sol ${colony.reports.last().sol} complete. Select a rested leader for your next mission.", color = FoodColor, style = MaterialTheme.typography.bodyMedium)
        }
        item {
            SectionLabel("MISSION LEADER")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                crew.forEach { person ->
                    FilterChip(selected = person.id == member.id, onClick = { onSelectCrew(person.id) },
                        enabled = !colony.missionInProgress && !colony.isFinished && person.id != colony.restingCrewId,
                        label = { Text(person.name + if (person.id == colony.restingCrewId) " · resting" else "") })
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("${member.name} · ${member.role} · ${member.skills.joinToString(", ")}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            if (member.id == colony.restingCrewId) Text("${member.name} is resting. Select another leader above.", color = EnergyColor, style = MaterialTheme.typography.bodyMedium)
        }
        items(missions, key = { it.id }) { mission ->
            MissionCard(mission, colony, member, onLaunch = { onLaunchMission(mission.id, member.id) })
        }
    }
}
