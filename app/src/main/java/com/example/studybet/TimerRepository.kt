package com.example.studybet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SessionPhase {
    IDLE,
    STUDY,
    REST,
}

data class SessionState(
    val phase: SessionPhase = SessionPhase.IDLE,
    val difficulty: Difficulty? = null,
)

object TimerRepository {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    fun update(state: SessionState) {
        _state.value = state
    }
}
