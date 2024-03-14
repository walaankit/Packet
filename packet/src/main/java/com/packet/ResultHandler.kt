package com.packet

import com.packet.extensions.updateWithFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A class that encapsulates the logic for making asynchronous calls and updating a [MutableStateFlow] with the result.
 * It supports initializing with either a [MutableStateFlow] for state management or an async call function.
 * Additionally, it allows setting onSuccess and onFailure callbacks to handle the result of the call.
 *
 * @param T The type of the data being requested.
 */

class ResultHandler<T, E> {
    private var stateFlow: MutableStateFlow<Packet<T, E>>? = null
    private var call: (suspend () -> T)? = null
    private var onSuccess: ((T) -> Unit)? = null
    private var onFailure: ((Throwable) -> Unit)? = null

    /**
     * Initializes the handler with a state flow.
     *
     * @param stateFlow A [MutableStateFlow] instance to update with loading, success, and failure states.
     */
    constructor(stateFlow: MutableStateFlow<Packet<T, E>>) {
        this.stateFlow = stateFlow
    }

    /**
     * Initializes the handler with a call function.
     *
     * @param call A suspend function representing the call to be made.
     */
    constructor(call: suspend () -> T) {
        this.call = call
    }

    /**
     * Sets a callback to be invoked on successful completion of the call.
     *
     * @param callback A function to be called with the result of the call.
     * @return The [ResultHandler] instance for chaining.
     */
    fun onSuccess(callback: (T) -> Unit): ResultHandler<T, E> {
        onSuccess = callback
        return this
    }

    /**
     * Sets a callback to be invoked in case of failure of the call.
     *
     * @param callback A function to be called with the error occurred during the call.
     * @return The [ResultHandler] instance for chaining.
     */
    fun onFailure(callback: (Throwable) -> Unit): ResultHandler<T, E> {
        onFailure = callback
        return this
    }

    /**
     * Executes the call and updates the state flow with the result.
     *
     * @param startWithLoading If true, the state flow is set to a loading state before making the call.
     * @param call The suspend function representing the call to be made.
     */
    suspend fun executeAndUpdate(
        startWithLoading: Boolean = true,
        call: suspend () -> T,
    ) {
        this.call = call
        execute(
            startWithLoading,
        )
    }

    /**
     * Launches the execution of the call within a given CoroutineScope.
     *
     * @param scope The [CoroutineScope] within which to launch the call.
     * @return A [Job] representing the coroutine that is executing the call.
     */
    fun executeIn(
        scope: CoroutineScope,
    ): Job {
        return scope.launch {
            execute(
                startWithLoading = false,
            )
        }
    }

    /**
     * Executes the call and updates the state flow with the result.
     *
     * @param startWithLoading If true, the state flow is set to a loading state before making the call.
     */
    private suspend fun execute(
        startWithLoading: Boolean,
    ) {
        call?.let { call ->
            if (startWithLoading) {
                stateFlow?.update { packet ->
                    Packet.Loading(packet.value)
                }
            }
            try {
                val data = call()
                stateFlow?.value = Packet.Success(data)
                onSuccess?.invoke(data)
            } catch (throwable: Throwable) {


                stateFlow?.updateWithFailure(throwable)
                onFailure?.invoke(throwable)
            }
        }
    }

    interface ResultHandlerThrowable<E> {
        fun mapThrowableToErrorData(): E
    }
}

