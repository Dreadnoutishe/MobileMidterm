package com.example.marscolonymanager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.marscolonymanager.ui.theme.*

@Composable
fun ResourceCard(name: String, amount: Float, modifier: Modifier = Modifier) {
    val resourceColor = when (name) { "Oxygen" -> OxygenColor; "Energy" -> EnergyColor; else -> FoodColor }
    val displayColor = if (amount < 20f) CriticalColor else resourceColor
    val progress by animateFloatAsState((amount / 100f).coerceIn(0f, 1f), label = name)
    ColonyPanel(modifier) {
        Text(name, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Text("${amount.toInt()}%", style = MaterialTheme.typography.headlineMedium, color = displayColor)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
            color = displayColor, trackColor = SpaceSurfaceVariant, drawStopIndicator = {})
    }
}
