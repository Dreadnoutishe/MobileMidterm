package com.example.marscolonymanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.marscolonymanager.model.*
import com.example.marscolonymanager.ui.components.*
import com.example.marscolonymanager.ui.theme.*

@Composable
fun ReportsScreen(colony: ColonyState, onMissions: () -> Unit, onRestart: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SectionLabel(if (colony.isFinished) "EXPEDITION DEBRIEF" else "EXPEDITION JOURNAL", MarsOrange)
            Spacer(Modifier.height(8.dp))
            Text(when { colony.hasWon -> "A future on Mars."
                !colony.isOperational -> "The colony went quiet."
                else -> "Every mission matters." }, style = MaterialTheme.typography.headlineLarge)
        }
        item {
            ColonyPanel {
                Icon(if (colony.hasWon) Icons.Rounded.EmojiEvents else if (!colony.isOperational) Icons.Rounded.Warning else Icons.Rounded.History,
                    null, tint = if (colony.hasWon) FoodColor else MarsOrange, modifier = Modifier.size(32.dp))
                Text(when { colony.hasWon -> "All seven sols survived. Ares-1 is established."
                    !colony.isOperational -> "${listOfNotNull(if (colony.oxygen <= 0) "Oxygen" else null, if (colony.energy <= 0) "Energy" else null, if (colony.food <= 0) "Food" else null).joinToString(" and ")} depleted. The expedition ended after ${colony.completedSols} sols."
                    else -> "${colony.completedSols} of 7 sols complete. Keep every vital resource above zero." }, style = MaterialTheme.typography.titleMedium)
                SectionLabel(if (colony.isFinished) "FINAL SCORE" else "CURRENT SCORE")
                Text("${colony.score}", style = MaterialTheme.typography.displayMedium, color = MarsOrange)
                Text("${colony.researchPoints} research · ${colony.reports.map { it.crewName }.distinct().size} crew leaders used", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Text("Oxygen ${colony.oxygen.toInt()} · Energy ${colony.energy.toInt()} · Food ${colony.food.toInt()}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                if (colony.isFinished) Button(onClick = onRestart, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Start a new colony") }
            }
        }
        if (colony.missionInProgress) item { MissionProgressCard(colony) }
        if (colony.reports.isEmpty()) item {
            ColonyPanel {
                Text("Your story starts with a mission.", style = MaterialTheme.typography.titleMedium)
                Text("Completed missions will appear here with their leader, resource changes, and research rewards.", color = TextSecondary)
                OutlinedButton(onClick = onMissions) { Text("Choose a mission") }
            }
        }
        if (colony.reports.isNotEmpty()) item { SectionLabel("MISSION HISTORY / NEWEST FIRST") }
        items(colony.reports.asReversed(), key = { it.sol }) { report ->
            ColonyPanel {
                SectionLabel("SOL ${report.sol} / ${report.crewName}", MarsOrange)
                Text(report.missionName, style = MaterialTheme.typography.titleLarge)
                if (report.specialistBonus) Text("Specialist supply bonus applied", color = FoodColor, style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(color = SpaceOutline)
                listOf(Triple("Oxygen", report.oxygenBefore, report.oxygenAfter), Triple("Energy", report.energyBefore, report.energyAfter), Triple("Food", report.foodBefore, report.foodAfter)).forEach { (name, before, after) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text("${before.toInt()} → ${after.toInt()} (${signed(after - before)})", color = if (after <= 0) CriticalColor else if (after < before) EnergyColor else FoodColor,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text("+${report.researchGain} research · Includes daily consumption", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}
