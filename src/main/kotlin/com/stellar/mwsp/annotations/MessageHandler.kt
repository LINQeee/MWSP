package com.stellar.mwsp.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MessageHandler(val endpoint: String)