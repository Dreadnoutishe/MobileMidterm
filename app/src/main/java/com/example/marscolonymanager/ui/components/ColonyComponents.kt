package com.example.marscolonymanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.marscolonymanager.model.*
import com.example.marscolonymanager.ui.theme.*

@Composable fun SectionLabel(text: String, color: Color = TextSecondary) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
}
@Composable fun ColonyPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(22.dp), color = SpaceSurface, border = BorderStroke(1.dp, SpaceOutline.copy(alpha = .65f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
fun signed(value: Float): String = if (value >= 0) "+${value.toInt()}" else "${value.toInt()}"
fun missionIcon(id: Int): ImageVector = when (id) { 1 -> Icons.Rounded.WaterDrop; 2 -> Icons.Rounded.Bolt; else -> Icons.Rounded.Eco }
fun missionColor(id: Int): Color = when (id) { 1 -> OxygenColor; 2 -> EnergyColor; else -> FoodColor }

@Composable fun MissionProgressCard(colony: ColonyState) {
    val mission = ColonyRules.missions.find { it.id == colony.activeMissionId } ?: return
    val member = ColonyRules.crew.find { it.id == colony.activeCrewId } ?: return
    val duration = ColonyRules.durationSeconds(member)
    ColonyPanel {
        SectionLabel("MISSION IN PROGRESS", MarsOrange)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.RocketLaunch, null, tint = MarsOrange)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(mission.name, style = MaterialTheme.typography.titleMedium)
                Text("${member.name} · ${duration - colony.progressSeconds} seconds remaining", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        LinearProgressIndicator(progress = { (colony.progressSeconds.toFloat() / duration).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(6.dp)), color = MarsOrange,
            trackColor = SpaceSurfaceVariant, drawStopIndicator = {})
        Text("Explore other screens while the mission runs. One mission advances one sol.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable fun ResourceForecast(colony: ColonyState, result: MissionForecast) {
    SectionLabel("AFTER MISSION + DAILY CONSUMPTION")
    listOf(Triple("Oxygen", colony.oxygen, result.oxygen), Triple("Energy", colony.energy, result.energy), Triple("Food", colony.food, result.food)).forEach { (name, before, after) ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text("${before.toInt()} → ${after.toInt()} (${signed(after - before)})", style = MaterialTheme.typography.bodyMedium,
                color = if (after <= 0) CriticalColor else if (after < before) EnergyColor else FoodColor)
        }
    }
}

/** Native vector artwork, so there are no remote images or extra asset dependencies. */
@Composable fun MarsLandscape(modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "Ares-1 habitat under the Martian sky" }) {
        val w = size.width; val h = size.height
        repeat(22) { i ->
            drawCircle(TextSecondary.copy(alpha = .45f), if (i % 4 == 0) 2f else 1f,
                Offset(w * ((i * 37 % 101) / 101f), h * ((i * 13 % 59) / 100f)))
        }
        drawCircle(MarsOrange.copy(alpha = .08f), h * .5f, Offset(w * .79f, h * .28f))
        drawCircle(MarsOrange.copy(alpha = .5f), h * .15f, Offset(w * .79f, h * .28f), style = Stroke(1.5f))
        val ridge = Path().apply {
            moveTo(0f, h * .72f); lineTo(w * .17f, h * .56f); lineTo(w * .35f, h * .76f)
            lineTo(w * .66f, h * .48f); lineTo(w, h * .72f); lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(ridge, MarsDarkOrange.copy(alpha = .18f))
        val ground = Path().apply { moveTo(0f, h * .9f); quadraticTo(w * .5f, h * .72f, w, h * .89f); lineTo(w, h); lineTo(0f, h); close() }
        drawPath(ground, MarsDarkOrange.copy(alpha = .22f))
        drawArc(OxygenColor.copy(alpha = .28f), 180f, 180f, true, Offset(w * .15f, h * .36f), Size(w * .42f, h * .9f))
        drawArc(OxygenColor.copy(alpha = .7f), 180f, 180f, false, Offset(w * .15f, h * .36f), Size(w * .42f, h * .9f), style = Stroke(2f))
        drawRoundRect(SpaceSurfaceVariant, Offset(w * .24f, h * .66f), Size(w * .25f, h * .18f), androidx.compose.ui.geometry.CornerRadius(4f))
        repeat(4) { i -> drawRect(EnergyColor.copy(alpha = .9f), Offset(w * (.27f + i * .05f), h * .7f), Size(w * .025f, h * .06f)) }
        drawLine(MarsOrange, Offset(w * .63f, h * .82f), Offset(w * .63f, h * .49f), 2f)
        drawLine(MarsOrange, Offset(w * .63f, h * .49f), Offset(w * .71f, h * .44f), 2f)
        drawCircle(OxygenColor, 4f, Offset(w * .71f, h * .44f))
    }
}
