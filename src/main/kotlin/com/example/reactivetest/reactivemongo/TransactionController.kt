package com.example.reactivetest.reactivemongo

import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@RestController
class TransactionController(
        val transactionRepository: TransactionRepository
) {

    @GetMapping("/transactions/{id}")
    fun get(@PathVariable id: String): Mono<Transaction> = transactionRepository.findById(id)

    @GetMapping("/transactions")
    fun getAll(): Flux<Transaction> = transactionRepository.findAll();

    // curl -X GET http://localhost:8080/dummy -H 'Accept: text/event-stream'
    @GetMapping("/dummy")
    fun getDummy(): Flux<Transaction> = Flux.interval(Duration.ofSeconds(1))
            .map { l -> Transaction(l.toString(), Status.PROCESSING) }
            .take(5)

    @PutMapping("/transactions")
    fun put(@RequestBody transaction: Transaction): Mono<Transaction> = transactionRepository.save(transaction)
}
