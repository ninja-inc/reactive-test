package com.example.reactivetest.mono

import org.assertj.core.api.Assertions.assertThat

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

class SimpleMono2Test {
    private val monoTest = SimpleMono()
    private val log = LoggerFactory.getLogger(this::class.java)

    @Test
    fun `subscribe mono`() {
        log.info("start")
        val mono = monoTest.increment2(0)

        log.info("1st block ${mono.block()}")
        log.info("2nd block ${mono.block()}")
        log.info("3rd block ${mono.block()}")
    }
}
