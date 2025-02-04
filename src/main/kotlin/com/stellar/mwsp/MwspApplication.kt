package com.stellar.mwsp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MwspApplication

fun main(args: Array<String>) {
    runApplication<MwspApplication>(*args)
}
