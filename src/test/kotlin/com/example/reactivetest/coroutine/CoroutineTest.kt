package com.example.reactivetest.coroutine

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class CoroutineTest {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Test
    fun test01() {
        val asyncTask1 = GlobalScope.async { doHeavyTask() }
        val asyncTask2 = GlobalScope.async { doHeavyTask() }

        runBlocking {
            log.info("asyncTask1: ${asyncTask1.await()}")
            log.info("asyncTask2: ${asyncTask2.await()}")
        }
    }

    private fun doHeavyTask(): String {
        log.info("start heavy task")
        Thread.sleep(2000)
        log.info("end heavy task")

        return "success"
    }
}