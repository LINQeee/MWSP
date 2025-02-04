package com.stellar.mwsp.system

import com.stellar.mwsp.core.WebSocketHandler
import com.stellar.mwsp.core.HandlerOptions
import com.stellar.mwsp.core.MWSPHandler
import com.stellar.mwsp.annotations.MessageBody
import com.stellar.mwsp.annotations.PathVariable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.collections.get
import kotlin.collections.iterator
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

class MWSPSystemHandler : MWSPHandler {
    override fun handleSubscription(
        topic: String,
        message: String,
        options: HandlerOptions
    ) {
        callMessageHandler(WebSocketHandler.Companion.subscriptionHandlers, topic, message, options)
    }

    override fun handleMessage(
        destination: String,
        message: String,
        options: HandlerOptions
    ) {
        callMessageHandler(WebSocketHandler.Companion.messageHandlers, destination, message, options)
    }

    override fun handleConnect(
        message: String,
        options: HandlerOptions
    ): Boolean {
        return true
    }

    override fun handleDisconnect(options: HandlerOptions) {
    }

    override fun handleUnsubscription(
        topic: String,
        message: String,
        options: HandlerOptions
    ) {
    }

    private fun callMessageHandler(
        handlers: MutableMap<String, Pair<KFunction<*>, Any>>,
        endpoint: String,
        message: String,
        options: HandlerOptions
    ): Any? {
        val pair = findHandlerAndParsePathVariables(handlers, endpoint)
        if (pair == null) {
            println("Not found $endpoint")
            return null
        }
        val (handler, pathVariables) = pair
        val (method) = handler
        return method.callBy(parseArgs(handler, message, pathVariables, options))
    }

    private fun parseArgs(
        handler: Pair<KFunction<*>, Any>,
        message: String,
        pathVariables: Map<String, String>,
        options: HandlerOptions
    ): MutableMap<KParameter, Any?> {
        val (method, instance) = handler
        val args: MutableMap<KParameter, Any?> = mutableMapOf()

        method.instanceParameter?.let { args[it] = instance }

        for (param in method.valueParameters) {
            if (param.findAnnotation<MessageBody>() != null)
                args[param] = deserialize(message, param.type)
            else if (param.findAnnotation<PathVariable>() != null) {
                val varValue = pathVariables[param.name]
                if (varValue == null) {
                    println("Not found path variable with name ${param.name}")
                    continue
                }
                args[param] = deserialize(varValue, param.type)
            } else if (param.type.jvmErasure == HandlerOptions::class)
                args[param] = options
            else println("NOT ANNOTATED PARAM")
        }
        return args
    }

    private fun deserialize(message: String, paramType: KType): Any {

        return when (paramType.jvmErasure) {
            String::class -> message
            Int::class -> message.toInt()
            Double::class -> message.toDouble()
            Boolean::class -> message.toBoolean()
            Long::class -> message.toLong()
            else -> {
                if (message.isEmpty()) {
                    throw IllegalArgumentException("Empty JSON cannot be deserialized to $paramType")
                }
                Json.Default.decodeFromJsonElement(serializer(paramType), Json.Default.parseToJsonElement(message))!!
            }
        }
    }

    private fun findHandlerAndParsePathVariables(
        handlers: MutableMap<String, Pair<KFunction<*>, Any>>,
        endpoint: String
    ): Pair<Pair<KFunction<*>, Any>, Map<String, String>>? {
        for ((uriPattern, handlerPair) in handlers) {
            val regexPattern = uriPattern.replace(Regex("\\{[^/]+}"), "([^/]+)").toRegex()

            val matchResult = regexPattern.matchEntire(endpoint)

            if (matchResult != null) {
                val pathVariables = uriPattern.split("/")
                    .filter { it.startsWith("{") && it.endsWith("}") }
                    .map { it.substring(1, it.length - 1) }

                val variableValues = matchResult.groupValues.drop(1)

                val pathVariablesMap = pathVariables.zip(variableValues).toMap()

                return handlerPair to pathVariablesMap
            }
        }

        return null
    }
}