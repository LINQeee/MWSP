package com.stellar.mwsp.core

import org.springframework.web.reactive.socket.WebSocketSession

interface MWSPHandler {

    fun handleSubscription(topic: String, message: String, options: HandlerOptions)

    fun handleMessage(destination: String, message: String, options: HandlerOptions)

    fun handleConnect(message: String, options: HandlerOptions): Boolean

    fun handleDisconnect(options: HandlerOptions)

    fun handleUnsubscription(topic: String, message: String, options: HandlerOptions)
}

data class HandlerOptions(
    val session: WebSocketSession,
    val attributes: Map<String, String>
)