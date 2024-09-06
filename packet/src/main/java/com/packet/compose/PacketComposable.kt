package com.packet.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.packet.Packet

@Composable
inline fun <T,E> ScreenPacketComposable(
    modifier: Modifier = Modifier,
    screenPacket: Packet<T, E>,
    crossinline onSuccess: @Composable BoxScope.(modifier: Modifier, value: T)-> Unit,
    crossinline onFailure: @Composable (throwable: Throwable,
                                        errorUiModels: E?, showErrorInSnackbar:Boolean) -> Unit,
    crossinline onLoading: @Composable BoxScope.() -> Unit ,
    allowSnackbarErrors: Boolean
) {

    Box(
        modifier = modifier
    ){

        val packetValueIsNotNull = screenPacket.value != null

        if(
            packetValueIsNotNull &&
            (screenPacket !is Packet.Failure || allowSnackbarErrors)
        ){
            onSuccess(Modifier, screenPacket.value!!)
        }

        if(screenPacket is Packet.Loading) {
            onLoading()
        } else if(screenPacket is Packet.Failure){
            val showErrorAsNudge = allowSnackbarErrors && packetValueIsNotNull
            onFailure(screenPacket.throwable, screenPacket.error, showErrorAsNudge)
        }

    }
}

