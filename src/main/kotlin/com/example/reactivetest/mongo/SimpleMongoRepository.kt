package com.example.reactivetest.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository

interface SimpleMongoRepository : MongoRepository<SimpleDocument, String>

@Document
data class SimpleDocument(
        @Id
        var id: String? = null,
        var message: String,
        @Version
        var version: Long? = null
)
