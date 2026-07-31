package com.wealthos

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WealthOsApplication

fun main(args: Array<String>) {
    runApplication<WealthOsApplication>(*args)
}
