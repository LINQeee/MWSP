package com.stellar.mwsp.configurations

import com.stellar.mwsp.core.MWSPHandler
import com.stellar.mwsp.annotations.MWSP
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.type.AnnotationMetadata
import kotlin.reflect.KClass

class WebSocketConfigRegistrar : ImportBeanDefinitionRegistrar {

    override fun registerBeanDefinitions(metadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
        val attributes = metadata.getAnnotationAttributes(MWSP::class.qualifiedName!!)!!
        val connectionUri = attributes["connectionUri"] as String
        val handlersArray = attributes["handlers"] as Array<*>
        val mwspHandlers = handlersArray.filterIsInstance<KClass<out MWSPHandler>>()
            .map { it.java }
            .toTypedArray()

        val beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(WebSocketConfig::class.java)
            .addConstructorArgValue(connectionUri)
            .addConstructorArgValue(mwspHandlers)
            .beanDefinition

        registry.registerBeanDefinition("webSocketConfig", beanDefinition)
    }
}