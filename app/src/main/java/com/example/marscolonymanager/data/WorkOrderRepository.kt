package com.example.marscolonymanager.data

import androidx.room.withTransaction
import com.example.marscolonymanager.model.WorkOrder
import com.example.marscolonymanager.model.WorkOrderCheck
import com.example.marscolonymanager.model.WorkOrderLimits
import com.example.marscolonymanager.model.WorkOrderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkOrderException(message: String) : IllegalStateException(message)

class WorkOrderRepository(private val database: ColonyDatabase) {
    private val dao = database.workOrders()

    fun observeOrders(): Flow<List<WorkOrder>> =
        dao.observeOrders().map { orders -> orders.map { it.toWorkOrder() } }

    fun observeOrder(id: Long): Flow<WorkOrder?> =
        dao.observeOrder(id).map { it?.toWorkOrder() }

    suspend fun createOrder(title: String, location: String, notes: String): Long {
        validateDetails(title, location, notes)
        return database.withTransaction {
            val id = dao.insertOrder(
                WorkOrderEntity(title = title.trim(), location = location.trim(), notes = notes.trim())
            )
            recordEvent(id, "Work order created")
            id
        }
    }

    suspend fun editOrder(id: Long, title: String, location: String, notes: String) {
        validateDetails(title, location, notes)
        database.withTransaction {
            val order = requireOrder(id)
            val edited = order.copy(title = title.trim(), location = location.trim(), notes = notes.trim())
            if (edited != order) {
                dao.updateOrder(edited)
                recordEvent(id, "Work order details edited")
            }
        }
    }

    suspend fun deleteOrder(id: Long) {
        if (dao.deleteOrder(id) == 0) throw WorkOrderException("This work order was removed.")
    }

    suspend fun startOrder(id: Long) {
        database.withTransaction {
            val order = requireOrder(id)
            if (order.status != WorkOrderStatus.PLANNED.name) {
                throw WorkOrderException("Only planned work orders can be started.")
            }
            dao.updateOrder(order.copy(status = WorkOrderStatus.IN_PROGRESS.name))
            recordEvent(id, "Work started")
        }
    }

    suspend fun setCheck(id: Long, check: WorkOrderCheck, checked: Boolean) {
        database.withTransaction {
            val order = requireOrder(id)
            if (order.status != WorkOrderStatus.IN_PROGRESS.name) {
                throw WorkOrderException("Start the work order before changing its checklist.")
            }
            val updated = when (check) {
                WorkOrderCheck.PARTS_READY -> order.copy(partsReady = checked)
                WorkOrderCheck.SAFETY_CHECKED -> order.copy(safetyChecked = checked)
            }
            if (updated != order) {
                dao.updateOrder(updated)
                val action = if (checked) "Checked" else "Cleared"
                recordEvent(id, "$action checklist item \"${check.label}\"")
            }
        }
    }

    suspend fun completeOrder(id: Long) {
        database.withTransaction {
            val order = requireOrder(id)
            if (order.status != WorkOrderStatus.IN_PROGRESS.name) {
                throw WorkOrderException("Only work orders in progress can be completed.")
            }
            if (!order.partsReady || !order.safetyChecked) {
                throw WorkOrderException("Finish both checklist items before completing the work order.")
            }
            dao.updateOrder(order.copy(status = WorkOrderStatus.COMPLETED.name))
            recordEvent(id, "Work completed")
        }
    }

    private suspend fun requireOrder(id: Long): WorkOrderEntity =
        dao.findOrder(id) ?: throw WorkOrderException("This work order was removed.")

    private suspend fun recordEvent(id: Long, message: String) {
        dao.insertEvent(
            WorkOrderEventEntity(orderId = id, message = message, occurredAt = System.currentTimeMillis())
        )
    }

    private fun validateDetails(title: String, location: String, notes: String) {
        if (title.isBlank()) throw WorkOrderException("Enter a work order title.")
        if (location.isBlank()) throw WorkOrderException("Enter a location.")
        if (title.trim().length > WorkOrderLimits.TITLE) {
            throw WorkOrderException("Keep the title to ${WorkOrderLimits.TITLE} characters or fewer.")
        }
        if (location.trim().length > WorkOrderLimits.LOCATION) {
            throw WorkOrderException("Keep the location to ${WorkOrderLimits.LOCATION} characters or fewer.")
        }
        if (notes.trim().length > WorkOrderLimits.NOTES) {
            throw WorkOrderException("Keep notes to ${WorkOrderLimits.NOTES} characters or fewer.")
        }
    }
}