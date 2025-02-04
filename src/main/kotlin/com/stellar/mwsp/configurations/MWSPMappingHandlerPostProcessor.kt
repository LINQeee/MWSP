package com.stellar.mwsp.configurations

import com.stellar.mwsp.core.WebSocketHandler
import com.stellar.mwsp.annotations.MWSPMappingHandler
import com.stellar.mwsp.annotations.MessageHandler
import com.stellar.mwsp.annotations.SubscribeHandler
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.stereotype.Component
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation

@Component
class MWSPMappingHandlerPostProcessor : BeanPostProcessor {

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean::class.annotations.any { it is MWSPMappingHandler }) {
            bean::class.declaredMemberFunctions.forEach { method ->
                method.findAnnotation<MessageHandler>()?.let { messageAnnotation ->
                    WebSocketHandler.Companion.messageHandlers[messageAnnotation.endpoint] = method to bean
                    println("Registered MessageHandler for endpoint: ${messageAnnotation.endpoint}")
                }
                method.findAnnotation<SubscribeHandler>()?.let { subscribeAnnotation ->
                    WebSocketHandler.Companion.subscriptionHandlers[subscribeAnnotation.topic] = method to bean
                    println("Registered SubscribeHandler for endpoint: ${subscribeAnnotation.topic}")
                }
            }
        }
        return bean
    }
}