package com.example.marscolonymanager.model

object WorkOrderLimits {
    const val TITLE = 80
    const val LOCATION = 60
    const val NOTES = 500
}

enum class WorkOrderStatus(val label: String) {
    PLANNED("Planned"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed")
}

enum class WorkOrderCheck(val label: String) {
    PARTS_READY("Parts ready"),
    SAFETY_CHECKED("Safety checked")
}

data class WorkOrder(
    val id: Long,
    val title: String,
    val location: String,
    val notes: String,
    val status: WorkOrderStatus,
    val partsReady: Boolean,
    val safetyChecked: Boolean,
    val history: List<WorkOrderEvent> = emptyList()
)

data class WorkOrderEvent(
    val id: Long,
    val message: String,
    val occurredAt: Long
)