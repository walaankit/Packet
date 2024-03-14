package com.packet.extensions

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


inline fun <T> LifecycleOwner.launchRepeatOnResume(
    flow: StateFlow<T>,
    crossinline collector: (T) -> Unit
) {
    launchRepeatWithLifecycleState(flow, Lifecycle.State.RESUMED, collector)
}

inline fun <T> LifecycleOwner.launchRepeatOnStart(
    flow: StateFlow<T>,
    crossinline collector: (T) -> Unit
) {
    launchRepeatWithLifecycleState(flow, Lifecycle.State.STARTED, collector)
}

inline fun <T> LifecycleOwner.launchRepeatWithLifecycleState(
    flow: StateFlow<T>,
    lifecycleState: Lifecycle.State,
    crossinline collector: (T) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(lifecycleState) {
            flow.collect {
                collector(it)
            }
        }
    }
}