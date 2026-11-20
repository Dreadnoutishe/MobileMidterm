package com.example.marscolonymanager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marscolonymanager.model.WorkOrder
import com.example.marscolonymanager.model.WorkOrderCheck
import com.example.marscolonymanager.model.WorkOrderEvent
import com.example.marscolonymanager.model.WorkOrderStatus
import com.example.marscolonymanager.ui.components.ColonyPanel
import com.example.marscolonymanager.ui.theme.EnergyColor
import com.example.marscolonymanager.ui.theme.FoodColor
import com.example.marscolonymanager.ui.theme.MarsOrange
import com.example.marscolonymanager.ui.theme.OxygenColor
import com.example.marscolonymanager.ui.theme.SpaceOutline
import com.example.marscolonymanager.ui.theme.SpaceSurface
import com.example.marscolonymanager.ui.theme.TextSecondary
import com.example.marscolonymanager.viewmodel.WorkOrderFormState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun MaintenanceScreen(
    orders: List<WorkOrder>?,
    loadError: String?,
    onRetry: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "heading") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WorkOrderHeading("Work orders")
                Button(onClick = onCreate, enabled = orders != null && loadError == null) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("New work order", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        when {
            loadError != null -> item(key = "error") {
                WorkOrderMessage("Couldn't load work orders", loadError, "Retry", onRetry)
            }
            orders == null -> item(key = "loading") { WorkOrderLoading("Loading work orders…") }
            orders.isEmpty() -> item(key = "empty") {
                ColonyPanel(Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Build, contentDescription = null, tint = MarsOrange)
                    Text("No work orders yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Create a work order to track a repair from planning through completion.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> {
                item(key = "summary") { WorkOrderSummary(orders) }
                items(orders, key = { it.id }) { order ->
                    WorkOrderCard(order, onOpen = { onOpen(order.id) })
                }
            }
        }
    }
}

@Composable
private fun WorkOrderSummary(orders: List<WorkOrder>) {
    val counts = remember(orders) { orders.groupingBy { it.status }.eachCount() }
    ColonyPanel(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WorkOrderStatus.entries.forEach { status ->
                Column(Modifier.weight(1f)) {
                    Text(
                        (counts[status] ?: 0).toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = status.color
                    )
                    Text(status.label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun WorkOrderCard(order: WorkOrder, onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = SpaceSurface,
        border = BorderStroke(1.dp, SpaceOutline.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    order.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    order.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                WorkOrderStatusLabel(order.status)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
fun WorkOrderDetailScreen(
    order: WorkOrder?,
    isLoading: Boolean,
    loadError: String?,
    isSaving: Boolean,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit,
    onCheck: (WorkOrderCheck, Boolean) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    var showDelete by rememberSaveable(order?.id) { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when {
            loadError != null -> item(key = "error") {
                WorkOrderMessage("Couldn't load this work order", loadError, "Retry", onRetry)
            }
            isLoading -> item(key = "loading") { WorkOrderLoading("Loading work order…") }
            order == null -> item(key = "missing") {
                WorkOrderMessage(
                    "Work order unavailable",
                    "This work order may have been deleted.",
                    "Back to work orders",
                    onBack
                )
            }
            else -> {
                item(key = "work_order") {
                    ColonyPanel(Modifier.fillMaxWidth()) {
                        WorkOrderStatusLabel(order.status)
                        WorkOrderHeading(order.title)
                        Text(order.location, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        if (order.notes.isNotBlank()) {
                            Text(order.notes, style = MaterialTheme.typography.bodyLarge)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onEdit, enabled = !isSaving) { Text("Edit") }
                            TextButton(onClick = { showDelete = true }, enabled = !isSaving) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                item(key = "checklist") {
                    WorkOrderChecklist(order, isSaving, onStart, onCheck, onComplete)
                }
                item(key = "history_heading") {
                    Text(
                        "Activity history",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                if (order.history.isEmpty()) {
                    item(key = "history_empty") { Text("No activity yet.", color = TextSecondary) }
                }
                items(order.history, key = { it.id }) { event -> WorkOrderHistoryEntry(event) }
            }
        }
    }
    if (showDelete && order != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete work order?") },
            text = { Text("This will permanently delete the work order and its activity history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        onDelete()
                    },
                    enabled = !isSaving
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun WorkOrderChecklist(
    order: WorkOrder,
    isSaving: Boolean,
    onStart: () -> Unit,
    onCheck: (WorkOrderCheck, Boolean) -> Unit,
    onComplete: () -> Unit
) {
    val canCheck = order.status == WorkOrderStatus.IN_PROGRESS && !isSaving
    ColonyPanel(Modifier.fillMaxWidth()) {
        Text(
            "Completion checklist",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
        ChecklistRow("Parts ready", order.partsReady, canCheck) {
            onCheck(WorkOrderCheck.PARTS_READY, it)
        }
        HorizontalDivider(color = SpaceOutline)
        ChecklistRow("Safety checked", order.safetyChecked, canCheck) {
            onCheck(WorkOrderCheck.SAFETY_CHECKED, it)
        }
        when (order.status) {
            WorkOrderStatus.PLANNED -> {
                Text("Start work to update the checklist.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onStart, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text("Start work")
                }
            }
            WorkOrderStatus.IN_PROGRESS -> {
                if (!order.partsReady || !order.safetyChecked) {
                    Text(
                        "Check both items to complete this work order.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(
                    onClick = onComplete,
                    enabled = order.partsReady && order.safetyChecked && !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Complete work order") }
            }
            WorkOrderStatus.COMPLETED -> Unit
        }
    }
}

@Composable
private fun ChecklistRow(label: String, checked: Boolean, enabled: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChecked),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun WorkOrderHistoryEntry(event: WorkOrderEvent) {
    val timestamp = remember(event.occurredAt) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(event.occurredAt))
    }
    ColonyPanel(Modifier.fillMaxWidth()) {
        Text(event.message, style = MaterialTheme.typography.bodyLarge)
        Text(timestamp, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun WorkOrderFormScreen(
    state: WorkOrderFormState,
    isEditing: Boolean,
    onTitleChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onRetry: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            state.isLoading -> WorkOrderLoading("Loading work order…")
            state.loadError != null -> WorkOrderMessage("Couldn't load work order", state.loadError, "Retry", onRetry)
            state.isMissing -> WorkOrderMessage(
                "Work order unavailable",
                "This work order may have been deleted.",
                "Go back",
                onCancel
            )
            else -> {
                WorkOrderField("Title", state.title, state.titleError, !state.isSaving, onTitleChange)
                WorkOrderField("Location", state.location, state.locationError, !state.isSaving, onLocationChange)
                WorkOrderField(
                    "Notes (optional)", state.notes, state.notesError, !state.isSaving,
                    onNotesChange, multiline = true
                )
                state.saveError?.let { message ->
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving
                ) {
                    Text(
                        when {
                            state.isSaving -> "Saving…"
                            isEditing -> "Save changes"
                            else -> "Create work order"
                        }
                    )
                }
                TextButton(onClick = onCancel, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun WorkOrderField(
    label: String,
    value: String,
    error: String?,
    enabled: Boolean,
    onChange: (String) -> Unit,
    multiline: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        singleLine = !multiline,
        minLines = if (multiline) 3 else 1,
        maxLines = if (multiline) 6 else 1,
        isError = error != null,
        supportingText = if (error == null) null else ({ Text(error) }),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (multiline) ImeAction.Default else ImeAction.Next
        )
    )
}

@Composable
private fun WorkOrderHeading(title: String) {
    Text(
        title,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineMedium
    )
}

@Composable
private fun WorkOrderLoading(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator()
        Text(label, color = TextSecondary)
    }
}

@Composable
private fun WorkOrderMessage(title: String, detail: String, actionLabel: String, onAction: () -> Unit) {
    ColonyPanel(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(detail, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun WorkOrderStatusLabel(status: WorkOrderStatus) {
    Surface(shape = RoundedCornerShape(8.dp), color = status.color.copy(alpha = 0.12f)) {
        Text(
            status.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelLarge,
            color = status.color
        )
    }
}

private val WorkOrderStatus.color: Color get() = when (this) {
    WorkOrderStatus.PLANNED -> OxygenColor
    WorkOrderStatus.IN_PROGRESS -> EnergyColor
    WorkOrderStatus.COMPLETED -> FoodColor
}
