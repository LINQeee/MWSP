package com.stellar.mwsp.configurations

import com.stellar.mwsp.core.WebSocketHandler
import com.stellar.mwsp.core.MWSPHandler
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

class WebSocketConfig(
    private val connectionUri: String,
    private val mwspHandlers: Array<Class<out MWSPHandler>>
) {
    @Bean
    fun webSocketMapping() =
        SimpleUrlHandlerMapping(
            mapOf(
                connectionUri to WebSocketHandler(mwspHandlers.map { it.getDeclaredConstructor().newInstance() }
                    .toMutableSet())
            ), 1
        )

    @Bean
    fun handlerAdapter() = WebSocketHandlerAdapter()
}

//setOf(
//"/new-message/{userId}",
//"/message-status/{userId}",
//"/new-chat/{userId}",
//"/new-friend-request/{userId}",
//"/friend-request-accept/{userId}",
//"/friend-request-decline/{userId}",
//"/chat"
//),