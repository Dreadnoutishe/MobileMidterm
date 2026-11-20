package com.example.marscolonymanager.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marscolonymanager.data.WorkOrderException
import com.example.marscolonymanager.data.WorkOrderRepository
import com.example.marscolonymanager.model.WorkOrder
import com.example.marscolonymanager.model.WorkOrderCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class WorkOrderUiState<T>(
    val data: T? = null,
    val isLoading: Boolean = true,
    val loadError: String? = null
)

data class WorkOrderFeedback(val message: String, val deleted: Boolean = false)

class WorkOrderViewModel(
    private val repository: WorkOrderRepository,
    private val orderId: Long? = null
) : ViewModel() {
    private val _orders = MutableStateFlow(WorkOrderUiState<List<WorkOrder>>())
    val orders = _orders.asStateFlow()
    private val _detail = MutableStateFlow(WorkOrderUiState<WorkOrder>())
    val detail = _detail.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()
    private val _feedback = MutableStateFlow<WorkOrderFeedback?>(null)
    val feedback = _feedback.asStateFlow()
    private var observation: Job? = null

    init {
        retry()
    }

    fun retry() {
        observation?.cancel()
        observation = if (orderId == null) {
            observe(repository.observeOrders(), _orders)
        } else {
            observe(repository.observeOrder(orderId), _detail)
        }
    }

    fun start() = changeOrder(success = "Work started.", action = repository::startOrder)

    fun setCheck(check: WorkOrderCheck, checked: Boolean) = changeOrder { id ->
        repository.setCheck(id, check, checked)
    }

    fun complete() = changeOrder(success = "Work order completed.", action = repository::completeOrder)

    fun delete() = changeOrder(
        success = "Work order deleted.",
        deleted = true,
        action = repository::deleteOrder
    )

    fun dismissFeedback() {
        _feedback.value = null
    }

    private fun <T> observe(
        source: Flow<T?>,
        state: MutableStateFlow<WorkOrderUiState<T>>
    ): Job {
        state.value = WorkOrderUiState()
        return viewModelScope.launch {
            source.catch { error ->
                Log.e("Maintenance", "Unable to read work orders", error)
                state.value = WorkOrderUiState(
                    isLoading = false,
                    loadError = "Could not load work orders. Try again."
                )
            }.collect { data -> state.value = WorkOrderUiState(data = data, isLoading = false) }
        }
    }

    private fun changeOrder(
        success: String? = null,
        deleted: Boolean = false,
        action: suspend (Long) -> Unit
    ) {
        val id = orderId ?: return
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                action(id)
                if (success != null) _feedback.value = WorkOrderFeedback(success, deleted)
            } catch (error: CancellationException) {
                throw error
            } catch (error: WorkOrderException) {
                _feedback.value = WorkOrderFeedback(error.message.orEmpty())
            } catch (error: Exception) {
                Log.e("Maintenance", "Unable to update work order", error)
                _feedback.value = WorkOrderFeedback("Could not update the work order. Try again.")
            } finally {
                _isSaving.value = false
            }
        }
    }
}
