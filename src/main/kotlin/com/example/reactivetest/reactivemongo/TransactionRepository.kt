package com.example.reactivetest.reactivemongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface TransactionRepository : ReactiveMongoRepository<Transaction, String>

@Document(collection = "transaction")
data class Transaction(
        @Id
        var id: String?,
        var status: Status
)

enum class Status {
    PROCESSING,
    COMPLETED
}
