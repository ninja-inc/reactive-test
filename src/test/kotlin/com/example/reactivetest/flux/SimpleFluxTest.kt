package com.example.reactivetest.flux

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger
import javax.print.attribute.IntegerSyntax

class SimpleFluxTest {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Test
    fun test() {
        val monoList: List<Mono<String>> = listOf(
                Mono.just("A"),
                Mono.just("B"),
                Mono.just("C")
        )

        val flux: Flux<String> = Flux.mergeSequential(monoList)

        assertAll(
                { assertThat(flux.blockFirst()).isEqualTo("A") },
                { assertThat(flux.blockFirst()).isEqualTo("A") },
                { assertThat(flux.blockFirst()).isEqualTo("A") }
        )
    }

    @Test
    fun test1() {
        val monoList: List<Mono<String>> = listOf(
                Mono.just("A"),
                Mono.error(RuntimeException("Fail!!")),
                Mono.just("C")
        )

        val flux: Flux<String> = Flux.mergeSequential(monoList)

        assertThatThrownBy { flux.collectList().block() }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Fail!!")
    }

    @Test
    fun test2() {
        val monoList: List<Mono<String>> = listOf(
                Mono.just("A"),
                Mono.error(RuntimeException("Fail!!")),
                Mono.just("C")
        )

        val flux: Flux<Result> = Flux.mergeSequential(monoList)
                .map { str -> Success(str) as Result }
                .onErrorResume { t -> Mono.just(Failure(t) as Result) }

        val resultList: List<Result>? = flux.collectList().block()

        assertAll(
                { assertThat(resultList).isNotNull },
                { assertThat(resultList).hasSize(2) },
                { assertThat(resultList!![0]).isEqualTo(Success("A")) },
                { assertThat((resultList!![1] as Failure).t.message).isEqualTo("Fail!!") }

                // when error happens, stream will be terminated
                //{ assertThat(resultList!![2]).isEqualTo(Success("C")) }
        )
    }

    @Test
    fun test3() {
        val atomicInteger = AtomicInteger()

        val mono: Mono<Int> = Mono.create {
            e -> e.success(atomicInteger.incrementAndGet())
        }

        assertAll(
                { assertThat(mono.block()).isEqualTo(1) },
                { assertThat(mono.block()).isEqualTo(2) },
                { assertThat(mono.block()).isEqualTo(3) }
        )

        val cachedMono: Mono<Int> = mono.cache()

        assertAll(
                { assertThat(cachedMono.block()).isEqualTo(4) },
                { assertThat(cachedMono.block()).isEqualTo(4) },
                { assertThat(cachedMono.block()).isEqualTo(4) },

                { assertThat(atomicInteger.get()).isEqualTo(4) }
        )
    }
}

sealed class Result

data class Success(
        val str: String
): Result()

data class Failure(
        val t: Throwable
): Result()