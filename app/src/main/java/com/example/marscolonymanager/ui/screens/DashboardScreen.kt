package com.example.marscolonymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.marscolonymanager.model.*
import com.example.marscolonymanager.ui.components.*
import com.example.marscolonymanager.ui.theme.*

@Composable
fun DashboardScreen(colony: ColonyState, onMissions: () -> Unit, onReports: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionLabel("ARES-1 / SURVIVAL EXPEDITION", MarsOrange)
        Text("Seven sols.\nOne colony.", style = MaterialTheme.typography.headlineLarge)
        ColonyPanel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { SectionLabel(if (colony.isFinished) "EXPEDITION COMPLETE" else "CURRENT MARTIAN DAY"); Text("Sol ${colony.sol} / 7", style = MaterialTheme.typography.headlineMedium) }
                Surface(color = (if (colony.hasWon) FoodColor else if (!colony.isOperational) CriticalColor else MarsOrange).copy(alpha = .12f), shape = RoundedCornerShape(10.dp)) {
                    Text(if (colony.hasWon) "SURVIVED" else colony.status.name, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall,
                        color = if (colony.hasWon) FoodColor else if (!colony.isOperational) CriticalColor else MarsOrange)
                }
            }
            MarsLandscape(Modifier.fillMaxWidth().height(115.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(ColonyRules.TARGET_SOLS) { index -> Box(Modifier.weight(1f).height(5.dp).background(
                    if (index < colony.completedSols) MarsOrange else SpaceSurfaceVariant, RoundedCornerShape(5.dp))) }
            }
            Text("${colony.completedSols} of 7 sols completed · ${colony.researchPoints} research", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        SectionLabel("COLONY RESERVES")
        BoxWithConstraints {
            if (maxWidth >= 320.dp) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResourceCard("Oxygen", colony.oxygen, Modifier.weight(1f))
                ResourceCard("Energy", colony.energy, Modifier.weight(1f))
                ResourceCard("Food", colony.food, Modifier.weight(1f))
            } else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ResourceCard("Oxygen", colony.oxygen, Modifier.fillMaxWidth())
                ResourceCard("Energy", colony.energy, Modifier.fillMaxWidth())
                ResourceCard("Food", colony.food, Modifier.fillMaxWidth())
            }
        }
        if (colony.missionInProgress) MissionProgressCard(colony)
        ColonyPanel {
            SectionLabel(if (colony.isFinished) "EXPEDITION OUTCOME" else "YOUR OBJECTIVE", MarsOrange)
            Text(when { colony.hasWon -> "Ares-1 survived all seven sols. Your colony has a future."
                !colony.isOperational -> "A vital resource reached zero. Review your choices and try again."
                else -> "Keep oxygen, energy, and food above zero for seven missions." }, style = MaterialTheme.typography.titleMedium)
            if (!colony.isFinished) {
                Text("Every completed mission consumes 14 oxygen, 12 energy, and 13 food. Mission energy costs are additional.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Text("Matching skills earn +8 supplies and +25 research. A leader rests during the following mission.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = if (colony.isFinished) onReports else onMissions,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(if (colony.isFinished) Icons.Rounded.History else Icons.Rounded.RocketLaunch, null)
            Spacer(Modifier.width(8.dp)); Text(if (colony.isFinished) "View expedition results" else if (colony.missionInProgress) "View active mission" else "Plan the next mission")
        }
        colony.reports.lastOrNull()?.let { report ->
            TextButton(onClick = onReports, modifier = Modifier.fillMaxWidth()) { Text("Last mission: ${report.missionName}") }
        }
        Text("Time advances only when a mission completes. You can take your time planning.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}
