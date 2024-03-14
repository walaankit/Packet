package com.packet

sealed class Packet<T, E>(val value: T? = null) {
    /** Denotes no op started or all standard op completed and reset to default state **/
    data class Idle<T, E>(private val initialData: T? = null) : Packet<T, E>(initialData)

    /** Denotes that we are loading something **/
    data class Loading<T, E>(private val initialData: T? = null) : Packet<T, E>(initialData)

    /** Denotes that we have data **/
    data class Success<T, E>(private val newData: T) : Packet<T, E>(newData){

        val data: T
            get() = value!!
    }

    /** Denotes a failure to fetch the data **/
    data class Failure<T, E>(
        val throwable: Throwable,
        val error: E? = null,
        private val initialData: T? = null
    ) : Packet<T, E>(initialData)

}

