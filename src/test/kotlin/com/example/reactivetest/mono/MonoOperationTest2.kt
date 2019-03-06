package com.example.reactivetest.mono

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MonoOperationTest2 {
    @Test
    fun `subscribe test`() {
        var result = ""

        Mono.just("abcd")
                .subscribe {
                    TimeUnit.SECONDS.sleep(1)
                    result = it
                }

        val a = Mono.just(result)

        assertThat(a.block()).isEqualTo("abcd")
    }

    @Test
    fun `subscribe test2`() {
        val atom = AtomicInteger()

        val mono = Mono.create<String> { e ->
            atom.getAndIncrement()
            e.success("abcd")
        }

        mono.subscribe()
        mono.subscribe()

        assertThat(atom.get()).isEqualTo(2)
    }
}
