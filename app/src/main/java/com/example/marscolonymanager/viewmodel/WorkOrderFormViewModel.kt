package com.example.marscolonymanager.viewmodel

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marscolonymanager.data.WorkOrderException
import com.example.marscolonymanager.data.WorkOrderRepository
import com.example.marscolonymanager.model.WorkOrderLimits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkOrderFormState(
    val title: String = "",
    val location: String = "",
    val notes: String = "",
    val titleError: String? = null,
    val locationError: String? = null,
    val notesError: String? = null,
    val isLoading: Boolean = false,
    val isMissing: Boolean = false,
    val isSaving: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null,
    val savedOrderId: Long? = null
)

class WorkOrderFormViewModel(
    private val repository: WorkOrderRepository,
    private val savedState: SavedStateHandle,
    private val orderId: Long?
) : ViewModel() {
    private val restoredDraft = savedState.get<Bundle>("workOrderDraft")
    private val _state = MutableStateFlow(
        WorkOrderFormState(
            title = restoredDraft?.getString("title").orEmpty(),
            location = restoredDraft?.getString("location").orEmpty(),
            notes = restoredDraft?.getString("notes").orEmpty(),
            isLoading = orderId != null
        )
    )
    val state = _state.asStateFlow()
    private var loading: Job? = null

    init {
        retry()
    }

    fun retry() {
        val id = orderId ?: return
        loading?.cancel()
        _state.update { it.copy(isLoading = true, isMissing = false, loadError = null) }
        loading = viewModelScope.launch {
            try {
                val order = repository.observeOrder(id).first()
                _state.update { current ->
                    when {
                        order == null -> current.copy(isLoading = false, isMissing = true)
                        savedState.contains("workOrderDraft") -> current.copy(isLoading = false)
                        else -> current.copy(
                            title = order.title,
                            location = order.location,
                            notes = order.notes,
                            isLoading = false
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("Maintenance", "Unable to load work order form", error)
                _state.update { it.copy(isLoading = false, loadError = "Could not load this work order. Try again.") }
            }
        }
    }

    fun changeTitle(title: String) = changeDraft { it.copy(title = keepDraft(title, WorkOrderLimits.TITLE), titleError = null) }

    fun changeLocation(location: String) = changeDraft { it.copy(location = keepDraft(location, WorkOrderLimits.LOCATION), locationError = null) }

    fun changeNotes(notes: String) = changeDraft { it.copy(notes = keepDraft(notes, WorkOrderLimits.NOTES), notesError = null) }

    fun save() {
        val current = _state.value
        if (current.isLoading || current.isMissing || current.loadError != null ||
            current.isSaving || current.savedOrderId != null
        ) return

        val title = current.title.trim()
        val location = current.location.trim()
        val notes = current.notes.trim()
        val validated = current.copy(
            titleError = requiredError(title, WorkOrderLimits.TITLE, "a work order title"),
            locationError = requiredError(location, WorkOrderLimits.LOCATION, "a location"),
            notesError = lengthError(notes, WorkOrderLimits.NOTES),
            saveError = null
        )
        if (validated.titleError != null || validated.locationError != null || validated.notesError != null) {
            _state.value = validated
            return
        }
        _state.value = validated.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val savedId = if (orderId == null) {
                    repository.createOrder(title, location, notes)
                } else {
                    repository.editOrder(orderId, title, location, notes)
                    orderId
                }
                _state.update { it.copy(isSaving = false, savedOrderId = savedId) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: WorkOrderException) {
                _state.update { it.copy(isSaving = false, saveError = error.message) }
            } catch (error: Exception) {
                Log.e("Maintenance", "Unable to save work order form", error)
                _state.update { it.copy(isSaving = false, saveError = "Could not save the work order. Try again.") }
            }
        }
    }

    private fun changeDraft(update: (WorkOrderFormState) -> WorkOrderFormState) {
        val current = _state.value
        if (current.isSaving) return
        val next = update(current).copy(saveError = null)
        _state.value = next
        savedState["workOrderDraft"] = Bundle().apply {
            putString("title", next.title)
            putString("location", next.location)
            putString("notes", next.notes)
        }
    }

    private fun keepDraft(value: String, limit: Int): String =
        if (value.length <= limit + 1) value else value.trim().take(limit + 1)

    private fun requiredError(value: String, maxLength: Int, label: String): String? =
        if (value.isBlank()) "Enter $label." else lengthError(value, maxLength)

    private fun lengthError(value: String, maxLength: Int): String? =
        if (value.length > maxLength) "Use $maxLength characters or fewer." else null
}
