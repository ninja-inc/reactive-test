package com.example.reactivetest.mono

import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.*

class SimpleMono {
    private val timer = Timer()
    private val log = LoggerFactory.getLogger(this::class.java)

    fun increment(v: Int): Mono<Int> = Mono.create() { e ->
        timer.schedule(object : TimerTask() {
            override fun run() {
                log.debug("increment, $v + 1")
                e.success(v + 1)
            }
        }, 500)
    }
}
