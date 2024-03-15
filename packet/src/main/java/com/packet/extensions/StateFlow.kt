package com.packet.extensions

import androidx.lifecycle.LifecycleOwner
import com.packet.Packet
import com.packet.ResultHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

fun <T, E> MutableStateFlow<Packet<T, E>>.updateWithFailure(
    throwable: Throwable,
) {
    update { packet ->
        Packet.Failure(
            throwable,
            (throwable as? ResultHandler.ResultHandlerThrowable<E>)?.mapToErrorModel(),
            packet.value
        )
    }
}


/**
 * Extension function to execute a call and update the [MutableStateFlow] with the result.
 * This function prepares for the execution by setting the [MutableStateFlow] into a loading state,
 * then executes the call, and updates the [MutableStateFlow] with either the success or failure result.
 *
 * @param T The type of the data being loaded.
 * @param startWithLoading If true, sets the state to loading before execution.
 * @param call A suspend function representing the call to be executed.
 */
suspend fun <T, E> MutableStateFlow<Packet<T, E>>.executeAndUpdate(
    startWithLoading: Boolean = true,
    call: suspend () -> T,
) {
    val resultHandler = ResultHandler(this)
    resultHandler.executeAndUpdate(
        startWithLoading,
        call,
    )
}

/**
 * Attaches an onSuccess callback to the [MutableStateFlow] which is triggered upon successful completion of the remote call.
 *
 * @param T The type of the data being loaded.
 * @param onSuccess A function to be executed on successful data retrieval.
 * @return A [ResultHandler] instance to manage the call and its results.
 */
fun <T, E> MutableStateFlow<Packet<T, E>>.onSuccess(
    onSuccess: (T) -> Unit
): ResultHandler<T, E> {
    return ResultHandler(this).onSuccess(onSuccess)
}

/**
 * Attaches an onFailure callback to the [MutableStateFlow] which is triggered in case of a failure during the remote call.
 *
 * @param T The type of the data being loaded.
 * @param onFailure A function to be executed in case of an error during data retrieval.
 * @return A [ResultHandler] instance to manage the call and its results.
 */
fun <T, E> MutableStateFlow<Packet<T, E>>.onFailure(
    onFailure: (Throwable) -> Unit
): ResultHandler<T, E> {
    return ResultHandler(this).onFailure(onFailure)
}

/**
 * Prepares for executing a call without immediately requiring a [MutableStateFlow].
 *
 * @param T The type of the data being loaded.
 * @param call A suspend function representing the call to be executed.
 * @return A [ResultHandler] instance to manage the call and its results.
 */
fun <T, E> prepareExecution(
    call: suspend () -> T,
): ResultHandler<T, E> {
    return ResultHandler(call)
}



fun <T, E> MutableStateFlow<Packet<T, E>>.markConsumed() {
    update {
        Packet.Idle(it.value)
    }
}

fun <T, E> Flow<Packet<T, E>>.onEachSuccess(successCallback: suspend (data: T) -> Unit): Flow<Packet<T, E>> {
    return this.onEach {
        (it as? Packet.Success)?.data?.let { data ->
            successCallback(data)
        }
    }
}

inline fun <T, E> StateFlow<Packet<T, E>>.collectOnStarted(
    lifecycleOwner: LifecycleOwner,
    crossinline onSuccess: (value: T) -> Unit,
    crossinline onFailure: (
        error: E?
    ) -> Unit,
    crossinline onLoading: () -> Unit = {},
    crossinline onIdle: (value: T?) -> Unit = {},
    noinline onConsumeFlow: (() -> Unit)? = null
) {
    lifecycleOwner.launchRepeatOnStart(this) {
        it.`when`(
            onSuccess = onSuccess,
            onFailure = onFailure,
            onLoading = onLoading,
            onIdle = onIdle,
            onConsumeFlow = onConsumeFlow
        )
    }
}

inline fun <T, E> StateFlow<Packet<T, E>>.collectOnResumed(
    lifecycleOwner: LifecycleOwner,
    crossinline onSuccess: (value: T) -> Unit,
    crossinline onFailure: (
        error: E?
    ) -> Unit,
    crossinline onLoading: () -> Unit = {},
    crossinline onIdle: (value: T?) -> Unit = {},
    noinline onConsumeFlow: (() -> Unit)? = null
) {
    lifecycleOwner.launchRepeatOnResume(this) {
        it.`when`(
            onSuccess = onSuccess,
            onFailure = onFailure,
            onLoading = onLoading,
            onIdle = onIdle,
            onConsumeFlow = onConsumeFlow
        )
    }
}

inline fun <T, E> Packet<T, E>.`when`(
    crossinline onSuccess: (value: T) -> Unit,
    crossinline onFailure: (
        error: E?
    ) -> Unit,
    crossinline onLoading: () -> Unit = {},
    crossinline onIdle: (value: T?) -> Unit = {},
    noinline onConsumeFlow: (() -> Unit)? = null
) {
    when (this) {
        is Packet.Success -> {
            onSuccess(data)
            onConsumeFlow?.invoke()
        }
        is Packet.Idle -> onIdle(value)
        is Packet.Failure -> {
            onFailure(
                error
            )
            onConsumeFlow?.invoke()
        }
        is Packet.Loading -> onLoading()
    }
}

suspend inline fun <reified T,E> safeApiCall(
    dispatcher: CoroutineDispatcher,
    crossinline apiCall: suspend () -> T,
    crossinline getResultHandlerThrowable: suspend (Throwable) -> ResultHandler.ResultHandlerThrowable<E>
): T {
    return withContext(dispatcher) {
        try {
            apiCall()
        } catch (throwable: Throwable) {
            throw(getResultHandlerThrowable(throwable) as Throwable)
        }
    }
}