package com.stellar.mwsp.annotations

import com.stellar.mwsp.core.MWSPHandler
import com.stellar.mwsp.configurations.WebSocketConfigRegistrar
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(WebSocketConfigRegistrar::class)
@Configuration
annotation class MWSP(val connectionUri: String, val handlers: Array<KClass<out MWSPHandler>> = [])