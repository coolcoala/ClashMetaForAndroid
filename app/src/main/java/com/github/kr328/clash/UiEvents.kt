package com.github.kr328.clash

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object UiEvents {
    sealed class Event {
        data class ProxySelected(val group: String, val name: String) : Event()
        data class GroupSelected(val group: String) : Event()
        data class ProfileSelected(val profileName: String?) : Event()
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<Event> = _events

    fun emit(event: Event) {
        _events.tryEmit(event)
    }
}


