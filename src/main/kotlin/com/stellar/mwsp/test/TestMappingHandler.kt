package com.stellar.mwsp.test

import com.stellar.mwsp.core.HandlerOptions
import com.stellar.mwsp.core.SubscriptionStringPayload
import com.stellar.mwsp.core.WebSocketHandler
import com.stellar.mwsp.annotations.MWSPMappingHandler
import com.stellar.mwsp.annotations.MessageBody
import com.stellar.mwsp.annotations.MessageHandler
import com.stellar.mwsp.annotations.PathVariable
import com.stellar.mwsp.annotations.SubscribeHandler

@MWSPMappingHandler
class TestMappingHandler {

    @MessageHandler("/chat/{id}")
    fun handleMessage(@PathVariable id: Long, @MessageBody message: String) {
        println("Received message from user $id: $message")
    }

    @SubscribeHandler("/chat")
    fun handleSubscription(options: HandlerOptions) {
        WebSocketHandler.Companion.sendToSubscribers(SubscriptionStringPayload("/chat", "HI"))
    }
}