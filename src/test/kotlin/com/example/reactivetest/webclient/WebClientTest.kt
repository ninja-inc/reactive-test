package com.example.reactivetest.webclient

import com.fasterxml.jackson.databind.JsonNode
import io.netty.channel.ChannelOption
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.TimeUnit

class WebClientTest {
    private lateinit var server: MockWebServer
    private lateinit var webClient: WebClient

    private val log = LoggerFactory.getLogger(this::class.java)

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .clientConnector(ReactorClientHttpConnector() {
                    options -> options.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1_000)
                })
                .build()
    }

    @AfterEach
    fun destroy() {
        server.shutdown()
    }

    @Test
    fun `when 500 status code returns`() {
        server.enqueue(MockResponse().setResponseCode(500))

        val result: Mono<JsonNode> = webClient.get()
                .uri("/")
                .retrieve()
                .bodyToMono(JsonNode::class.java)

        assertThatThrownBy {result.block()}
                .isInstanceOf(WebClientResponseException::class.java)
                .hasMessageContaining("500 Internal Server Error")
    }

    @Test
    fun `when 500 status code returns with onStatus() method`() {
        server.enqueue(MockResponse().setResponseCode(500))

        val result: Mono<JsonNode> = webClient.get()
                .uri("/")
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError) { res -> Mono.error(MyCustomException(res)) }
                .bodyToMono(JsonNode::class.java)

        assertThatThrownBy {result.block()}
                .isInstanceOf(MyCustomException::class.java)
    }

    @Test
    fun `when time out error happens`() {
        server.enqueue(
                MockResponse().setResponseCode(500)
                        .setBody("abcdef")
                        .setBodyDelay(3, TimeUnit.SECONDS))

        val result: Mono<JsonNode> = webClient.get()
                .uri("/")
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError) { res -> Mono.error(MyCustomException(res)) }
                .bodyToMono(JsonNode::class.java)
                .timeout(Duration.ofSeconds(1))

        // ReactiveException.class will be thrown but it is default modifier
        assertThatThrownBy {result.block()}
                .isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("1000ms")
    }
}

class MyCustomException(
       val response: ClientResponse
): RuntimeException()
