package com.stellar.mwsp.core

import com.stellar.mwsp.system.MWSPSystemHandler
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KFunction

@Component
class WebSocketHandler(
    var mwspHandlers: MutableSet<MWSPHandler> = mutableSetOf()
) :
    WebSocketHandler {

    init {
        mwspHandlers = mutableSetOf(MWSPSystemHandler(), *mwspHandlers.toTypedArray())
    }

    override fun handle(session: WebSocketSession): Mono<Void> {
        val attributes = mutableMapOf<String, String>()
        var authed = false

        println("✅ User connected ${session.id} [${session.handshakeInfo.remoteAddress?.address?.hostAddress ?: "unknown"}]")

        return session.receive()
            .doOnNext { message ->
                val payload = parseMessage(message.payloadAsText)
                if (payload == null) return@doOnNext

                if (payload.type == MessageType.CONNECT) if (mwspHandlers.map {
                        it.handleConnect(
                            payload.payload,
                            HandlerOptions(session, attributes)
                        )
                    }.all { it } == true) authed = true

                if (!authed) {
                    println("❌ Unauthorized message from ${session.id}")
                    return@doOnNext
                }

                when (payload.type) {
                    MessageType.SUBSCRIBE -> {
                        subscriptions.computeIfAbsent(payload.endpoint) { ConcurrentHashMap() }.put(session.id, session)
                        mwspHandlers.forEach {
                            it.handleSubscription(
                                payload.endpoint,
                                payload.payload,
                                HandlerOptions(session, attributes)
                            )
                        }
                    }

                    MessageType.UNSUBSCRIBE -> {
                        subscriptions[payload.endpoint]?.remove(session.id)
                        mwspHandlers.forEach {
                            it.handleUnsubscription(
                                payload.endpoint,
                                payload.payload,
                                HandlerOptions(session, attributes)
                            )
                        }
                    }

                    MessageType.MESSAGE ->
                        mwspHandlers.forEach {
                            it.handleMessage(
                                payload.endpoint,
                                payload.payload,
                                HandlerOptions(session, attributes)
                            )
                        }

                    MessageType.CONNECT -> {}
                }
                println("📩 Received message ${payload.endpoint}[${payload.type}]: ${message.payloadAsText}")
            }
            .doFinally {
                subscriptions.forEach { it.value.remove(session.id) }
                mwspHandlers.forEach { it.handleDisconnect(HandlerOptions(session, attributes)) }
            }
            .doOnError { e -> println("❌ WebSocket error: ${e.message}") }
            .then()
    }

    companion object {

        val subscriptionHandlers = mutableMapOf<String, Pair<KFunction<*>, Any>>()
        val messageHandlers = mutableMapOf<String, Pair<KFunction<*>, Any>>()

        private val subscriptions = ConcurrentHashMap<String, ConcurrentHashMap<String, WebSocketSession>>()

        fun sendToSubscribers(subscriptionPayload: Any) {
            val topic = when (subscriptionPayload) {
                is SubscriptionPayload -> subscriptionPayload.topic
                is SubscriptionStringPayload -> subscriptionPayload.topic
                else -> throw IllegalArgumentException("Unsupported payload type")
            }

            val payloadJson =
                if (subscriptionPayload is SubscriptionPayload) Json.encodeToJsonElement<SubscriptionPayload>(
                    subscriptionPayload
                ).toString()
                else Json.encodeToJsonElement<SubscriptionStringPayload>(subscriptionPayload as SubscriptionStringPayload)
                    .toString()
            val payloadBytes = payloadJson.toByteArray()

            subscriptions[topic]?.forEach { (_, session) ->
                val bufferFactory = session.bufferFactory()
                val pooledBuffer = bufferFactory.allocateBuffer(payloadBytes.size)
                pooledBuffer.write(payloadBytes)

                session.send(Flux.just(session.binaryMessage { pooledBuffer })).subscribe()
            }
        }
    }

    private fun parseMessage(textPayload: String): Message? =
        try {
            Json.decodeFromString<Message>(textPayload)
        } catch (_: Exception) {
            println("Couldn't parse message $textPayload")
            null
        }
}

@Serializable
data class SubscriptionPayload(val topic: String, val payload: java.io.Serializable)

@Serializable
data class SubscriptionStringPayload(val topic: String, val payload: String)

@Serializable
data class Message(val endpoint: String, val payload: String, val type: MessageType)

enum class MessageType {
    CONNECT,
    SUBSCRIBE,
    UNSUBSCRIBE,
    MESSAGE
}