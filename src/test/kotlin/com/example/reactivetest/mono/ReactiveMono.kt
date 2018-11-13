package com.example.reactivetest.mono

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

class ReactiveMono {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Test
    fun test1() {
        val a = Mono.just(1)
        val b = Mono.just(2)

        val c = Mono.zip(a, b) {x, y -> x + y}

        log.info("${c.block()}")

    }
}