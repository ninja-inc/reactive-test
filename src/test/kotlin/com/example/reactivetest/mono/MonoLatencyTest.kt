package com.example.reactivetest.mono

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

class MonoLatencyTest {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Test
    fun test01() {
        val task1: Mono<String> = makeHeavyTask().subscribeOn(Schedulers.parallel()).cache()
        val task2: Mono<String> = makeHeavyTask().subscribeOn(Schedulers.parallel()).cache()
        val task3: Mono<String> = makeHeavyTask().subscribeOn(Schedulers.parallel()).cache()
        val task4: Mono<String> = makeHeavyTask().subscribeOn(Schedulers.parallel()).cache()

        task1.subscribe()
        task2.subscribe()
        task3.subscribe()
        task4.subscribe()

        log.info("test start")

        log.info("test result1: ${task1.block()}")
        log.info("test result2: ${task2.block()}")
        log.info("test result3: ${task3.block()}")
        log.info("test result4: ${task4.block()}")


        makeHeavyTask().map { }
    }

    private fun makeHeavyTask(): Mono<String> = Mono.create { sink ->
        log.info("start heavy task!")
        Thread.sleep(2000)
        sink.success("success")
    }


    fun test2(){
        val m1 = Mono.just("A")
        val m2 = Mono.just("B")
        val m3 = Mono.just("C")

        val combined = Mono.zip(m1, m2, m3).map { t ->
            t.t1 + t.t2 + t.t3
        }

        combined.block()
    }
}