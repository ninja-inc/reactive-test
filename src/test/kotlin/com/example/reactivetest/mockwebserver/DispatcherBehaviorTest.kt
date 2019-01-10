package com.example.reactivetest.mockwebserver

import com.fasterxml.jackson.databind.JsonNode
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.atomic.AtomicInteger

class DispatcherBehaviorTest {
    lateinit var server: MockWebServer
    lateinit var webClient: WebClient

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.setDispatcher(object : Dispatcher() {
            var cnt: AtomicInteger = AtomicInteger()

            override fun dispatch(request: RecordedRequest): MockResponse = MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                        {
                            "cnt": ${cnt.getAndIncrement()}
                        }
                    """.trimIndent())
        })

        webClient = WebClient.create(server.url("/").toString())
    }

    @AfterEach
    fun destroy() {
        server.shutdown()
    }

    @Test
    fun test() {
        val res1: JsonNode? = webClient.get().uri("").retrieve().bodyToMono(JsonNode::class.java).block()
        val res2: JsonNode? = webClient.get().uri("").retrieve().bodyToMono(JsonNode::class.java).block()

        assertAll(
                { assertThat(res1!!["cnt"].intValue()).isEqualTo(0) },
                { assertThat(res2!!["cnt"].intValue()).isEqualTo(1) }
        )
    }
}
