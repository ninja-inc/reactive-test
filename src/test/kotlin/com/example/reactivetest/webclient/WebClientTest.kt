package com.example.reactivetest.webclient

import com.fasterxml.jackson.databind.JsonNode
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
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
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

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

        webClient.get()
                .uri("")
                .exchange()
                .flatMap { res -> if (res.statusCode() == HttpStatus.OK) res.bodyToMono(String.javaClass) else res.bodyToMono(String.javaClass) }
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
                .hasMessage("ClientResponse has erroneous status code: 500 Internal Server Error")
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
                .hasCause(TimeoutException("Did not observe any item or terminal signal within 1000ms (and no fallback has been configured)"))
    }

    @Test
    fun `when 500 status code returns with onErrorMap()`() {
        server.enqueue(MockResponse().setResponseCode(500))

        val result: Mono<JsonNode> = webClient.get()
                .uri("/")
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .onErrorMap({e -> e is WebClientResponseException}, {e -> MyCustomException(e)})

        assertThatThrownBy {result.block()}
                .isInstanceOf(MyCustomException::class.java)
    }

    @Test
    fun `when timeout error happens with onErrorMap()`() {
        server.enqueue(MockResponse().setResponseCode(500).setBodyDelay(3, TimeUnit.SECONDS))

        val result: Mono<JsonNode> = webClient.get()
                .uri("/")
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .onErrorMap({e -> e is WebClientResponseException}, {e -> MyCustomException(e)})
                //.onErrorMap {e -> MyCustomException(e)} onError() here will now work as expected in case of timeout. this should be written after .timeout() block.
                .timeout(Duration.ofSeconds(1))
                .onErrorMap {e -> MyCustomException(e)}

        assertThatThrownBy {result.block()}
                .isInstanceOf(MyCustomException::class.java)
                .hasCauseExactlyInstanceOf(TimeoutException::class.java)
    }

    @Test
    fun `block with latency`() {
        val response = MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                        "message": "hello!"
                    }
                """.trimIndent())
                .setBodyDelay(3, TimeUnit.SECONDS)

        server.enqueue(response)

        val result: Mono<JsonNode> = webClient.get()
                .uri("/")
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .cache()
                .cache()

        log.info("start")
        log.info("1st result: ${result.block()}")
        log.info("2nd result: ${result.blockOptional()}")
        log.info("3rd result: ${result.blockOptional()}")
        log.info("4th result: ${result.blockOptional()}")
        log.info("5th result: ${result.blockOptional()}")
    }

    @Test
    fun `block with latency2`() {
        val response1 = MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBodyDelay(3, TimeUnit.SECONDS)

        val response2 = MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBodyDelay(3, TimeUnit.SECONDS)

        server.enqueue(response1)
        server.enqueue(response2)

        val result1: Mono<String> = webClient.get()
                .uri("/")
                .retrieve()
                .bodyToMono(String::class.java)
                .subscribeOn(Schedulers.parallel())

        val result2: Mono<String> = webClient.get()
                .uri("/")
                .retrieve()
                .bodyToMono(String::class.java)
                .subscribeOn(Schedulers.parallel())

        log.info("start test")
        log.info("1st result: ${result1.block()}")
        log.info("2nd result: ${result2.block()}")
    }

    @Test
    fun `(either pattern) when 500 status code returns with onErrorResume()`() {
        server.enqueue(MockResponse().setResponseCode(500))

        val result: Mono<out SubTransactionResult> = webClient.get()
                .uri("/")
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .map { response -> SubTransactionSuccess(response) as SubTransactionResult}
                .onErrorResume( {e -> e is WebClientResponseException}, {e -> Mono.just(SubTransactionFailure(e))} )

        assertThat(result.block()).isInstanceOf(SubTransactionFailure::class.java)
    }

    @Test
    fun `(either pattern) when success`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val result: Mono<out SubTransactionResult> = webClient.get()
                .uri("/")
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .map { response -> SubTransactionSuccess(response) as SubTransactionResult}
                .onErrorResume( {e -> e is WebClientResponseException}, {e -> Mono.just(SubTransactionFailure(e))} )
                .switchIfEmpty ( Mono.just(SubTransactionSuccess()) ) // This is needed when response body is null otherwise `null` will return when we block Mono instance.

        assertThat(result.block()).isInstanceOf(SubTransactionSuccess::class.java)
    }
}

class MyCustomException: RuntimeException {
    val clientResponse: ClientResponse?

    constructor(clientResponse: ClientResponse) {
        this.clientResponse = clientResponse
    }

    constructor(cause: Throwable): super(cause) {
        this.clientResponse = null
    }
}

sealed class SubTransactionResult

data class SubTransactionSuccess(
        val response: JsonNode? = null
) : SubTransactionResult()

data class SubTransactionFailure(
        val exception: Throwable
) : SubTransactionResult()