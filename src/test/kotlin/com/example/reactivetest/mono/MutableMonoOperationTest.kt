package com.example.reactivetest.mono

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

class MutableMonoOperationTest {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Test
    fun test() {
        val mutableList1: MutableList<String> = mutableListOf()

        val mono: Mono<String> = Mono.create { sink -> sink.success("success") }

        val cachedMono1 = mono.subscribeOn(Schedulers.parallel())
                .doOnNext {result -> mutableList1.add(result)}
                .cache()

        cachedMono1.block()
        cachedMono1.block()

        assertThat(mutableList1).hasSize(1)
    }

    @Test
    fun test2() {
        val mutableList1: MutableList<String> = mutableListOf()

        val mono: Mono<String> = Mono.create { sink -> sink.success("success") }

        val cachedMono1 = mono.subscribeOn(Schedulers.parallel())
                .cache()
                .doOnNext {result -> mutableList1.add(result)}

        cachedMono1.block()
        cachedMono1.block()

        assertThat(mutableList1).hasSize(2)
    }
}