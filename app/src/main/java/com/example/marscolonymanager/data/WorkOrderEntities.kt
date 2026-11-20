package com.example.marscolonymanager.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.marscolonymanager.model.WorkOrder
import com.example.marscolonymanager.model.WorkOrderEvent
import com.example.marscolonymanager.model.WorkOrderStatus

@Entity(tableName = "work_orders")
internal data class WorkOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val location: String,
    val notes: String,
    val status: String = WorkOrderStatus.PLANNED.name,
    val partsReady: Boolean = false,
    val safetyChecked: Boolean = false
) {
    fun toWorkOrder(history: List<WorkOrderEvent> = emptyList()) = WorkOrder(
        id = id,
        title = title,
        location = location,
        notes = notes,
        status = WorkOrderStatus.valueOf(status),
        partsReady = partsReady,
        safetyChecked = safetyChecked,
        history = history
    )
}

@Entity(
    tableName = "work_order_events",
    foreignKeys = [
        ForeignKey(
            entity = WorkOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
internal data class WorkOrderEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val message: String,
    val occurredAt: Long
)

internal data class WorkOrderWithHistory(
    @Embedded val order: WorkOrderEntity,
    @Relation(parentColumn = "id", entityColumn = "orderId")
    val events: List<WorkOrderEventEntity>
) {
    fun toWorkOrder() = order.toWorkOrder(
        history = events.sortedByDescending { it.id }.map {
            WorkOrderEvent(id = it.id, message = it.message, occurredAt = it.occurredAt)
        }
    )
}