package com.example.reactivetest.mono

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.lang.RuntimeException

class MonoOperationTest {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Test
    fun `filter and map on mono`() {
        val result = Result(
                status = Status.SUCCESS,
                detail = Detail(
                        desc = "I am description"
                )
        )

        val mono: Mono<Detail> = Mono.just(result)
                .filter(Result::isSuccess)
                .map { res -> res.detail };

        assertThat(mono.block()).isEqualTo(
                Detail(
                        desc = "I am description"
                )
        );
    }

    @Test
    fun `filter and map on mono2`() {
        val result = Result(
                status = Status.FAILURE,
                detail = Detail(
                        desc = "I am description"
                )
        )

        val mono: Mono<Detail> = Mono.just(result)
                .filter(Result::isSuccess)
                .map { res -> res.detail };

        assertThat(mono.block()).isEqualTo(null);
    }

    @Test
    fun `filter and map on mono3`() {
        val result = Result(
                status = Status.FAILURE,
                detail = Detail(
                        desc = "I am description"
                )
        )

        val mono: Mono<Detail> = Mono.just(result)
                .filter(Result::isSuccess)
                .map { res -> res.detail }
                .switchIfEmpty(Mono.error(RuntimeException("result is not success")))

        assertThatThrownBy { mono.block() }
                .hasMessageContaining("result is not success")
    }

    @Test
    fun `Mono empty`() {
        val mono = Mono.empty<String>()

        assertThat(mono.block()).isNull()
    }

    @Test
    fun `Mono throw exception`() {
        val mono = Mono.just("aaa")
                .doOnNext { throw RuntimeException("fail!!") }
                .map { _ -> "bbb" }

        assertThatThrownBy { mono.block() }
                .hasMessageContaining("fail!!")
    }
}

data class Result(
        val status: Status,
        val detail: Detail
) {
    fun isSuccess() = status == Status.SUCCESS
}

data class Detail(
        val desc: String
)

enum class Status {
    SUCCESS,
    FAILURE
}