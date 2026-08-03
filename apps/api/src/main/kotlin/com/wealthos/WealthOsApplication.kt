package com.wealthos

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class WealthOsApplication

fun main(args: Array<String>) {
    runApplication<WealthOsApplication>(*args)
}
