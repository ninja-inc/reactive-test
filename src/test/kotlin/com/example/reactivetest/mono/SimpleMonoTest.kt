package com.example.reactivetest.mono

import org.assertj.core.api.Assertions.assertThat

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

class SimpleMonoTest {
    private val monoTest = SimpleMono()
    private val log = LoggerFactory.getLogger(this::class.java)

    @Test
    fun `subscribe mono`() {
        log.info("subscribe starts")
        monoTest.increment(0).subscribe() {v -> log.info("value: $v")}
        log.info("subscribe ends")

        Thread.sleep(1_000)
    }

    @Test
    fun `block mono`() {
        val mono: Mono<Int> = monoTest.increment(0)

        assertThat(mono.block()).isEqualTo(1)

        // same result
        assertThat(mono.block()).isEqualTo(1)
    }

    @Test
    fun `flat map mono`() {
        val mono: Mono<Int> = monoTest.increment(0)
                .flatMap(monoTest::increment)
                .flatMap(monoTest::increment)
                .flatMap(monoTest::increment)

        assertThat(mono.block()).isEqualTo(4)
    }

    /**
     * some article says when() will wait for completion of all Mono's result but subscriber will not be invoked..
     */
    @Test
    fun `when mono`() {
        Mono.`when`(
                monoTest.increment(0),
                monoTest.increment(0).flatMap(monoTest::increment),
                monoTest.increment(0).flatMap(monoTest::increment).flatMap(monoTest::increment)
        ).subscribe() {v -> log.info("result: $v")}

        Thread.sleep(3_000)
    }
}
