package com.olivka.clock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.olivka.clock.data.TimerEntity
import com.olivka.clock.data.TimerRepository
import com.olivka.clock.utils.TimerUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimerViewModel(
    private val repository: TimerRepository
) : ViewModel() {
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    val allTimers: StateFlow<List<TimerEntity>> = repository.allTimers
        .onEach { _loaded.value = true }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /**
     * Inserts a new timer unless a timer with the same duration and label
     * already exists. [onResult] receives the stored timer, or null when the
     * timer is a duplicate.
     */
    fun createTimer(
        durationMs: Long,
        label: String,
        soundName: String,
        soundUri: String,
        vibrationEnabled: Boolean,
        vibrationFollowsGlobal: Boolean,
        onResult: (TimerEntity?) -> Unit
    ) {
        viewModelScope.launch {
            val existing = repository.allTimers.first()
            val candidate = TimerEntity(
                label = label.trim(),
                durationMs = durationMs,
                remainingMs = durationMs,
                createdAt = System.currentTimeMillis(),
                soundName = soundName,
                soundUri = soundUri,
                vibrationEnabled = vibrationEnabled,
                vibrationFollowsGlobal = vibrationFollowsGlobal
            )
            if (existing.any { TimerUtils.isSameTimer(it, candidate) }) {
                onResult(null)
                return@launch
            }
            val id = runCatching { repository.insert(candidate) }
                .getOrElse {
                    onResult(null)
                    return@launch
                }
            onResult(candidate.copy(id = id.toInt()))
        }
    }
}

class TimerViewModelFactory(
    private val repository: TimerRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
