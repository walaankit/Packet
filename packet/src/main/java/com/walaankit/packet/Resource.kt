package com.walaankit.packet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update


sealed class Resource<T, E>( val value: T? = null) {
    /** Denotes no op started or all standard op completed and reset to default state **/
    data class Idle<T, E>(private val initialData: T? = null) : Resource<T, E>(initialData){
        override fun <R> map(preData:R? ,transform: (oldTypeValue: T) -> R): Resource<R, E> {
            return Idle(
                preData?:initialData?.let(transform)
            )
        }

        override suspend fun <R> suspendedMap(preData:R?, transform: suspend (oldTypeValue: T) -> R): Resource<R, E> {
            return Idle(initialData?.let { transform(it) })
        }

    }

    /** Denotes that we are loading something **/
    class Loading<T, E>(private val initialData: T? = null) : Resource<T, E>(initialData){
        override fun <R> map(preData:R?, transform: (oldTypeValue: T) -> R): Resource<R, E> {
            return Loading(
                preData?:initialData?.let(transform)
            )
        }

        override suspend fun <R> suspendedMap(preData:R?, transform: suspend (oldTypeValue: T) -> R): Resource<R, E> {
            return Loading()
        }

    }

    /** Denotes that we have data **/
    data class Success<T, E>(private val newData: T) : Resource<T, E>(newData){
        override fun <R> map(preData:R?, transform: (oldTypeValue: T) -> R): Resource<R, E> {
            return Success(transform(newData)!!)
        }

        override suspend fun <R> suspendedMap(preData:R?, transform: suspend (oldTypeValue: T) -> R): Resource<R, E> {
            return Success(transform(newData)!!)
        }

        val data: T
            get() = value!!
    }

    /** Denotes a failure to fetch the data **/
    data class Failure<T, E>(
        val throwable: Throwable,
        val networkErrors: List<E>? = null,
        private val initialData: T? = null
    ) : Resource<T, E>(initialData){
        override fun <R> map(preData:R?, transform: (oldTypeValue: T) -> R): Resource<R, E> {
            return Failure(
                throwable,
                networkErrors,
                preData?:initialData?.let(transform)
            )
        }

        override suspend fun <R> suspendedMap(preData:R?, transform: suspend (oldTypeValue: T) -> R): Resource<R, E> {
            return Failure(throwable, networkErrors)
        }

        val data: T?
            get() = value
    }

    abstract fun <R> map(preData:R? = null, transform: (oldTypeValue: T) -> R): Resource<R, E>
    abstract suspend fun <R> suspendedMap(preData:R? = null, transform: suspend (oldTypeValue: T) -> R): Resource<R, E>
}

fun <T, E> MutableStateFlow<Resource<T, E>>.markConsumed() {
    update {
        Resource.Idle(it.value)
    }
}