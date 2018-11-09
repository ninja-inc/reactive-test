package com.example.reactivetest.mongo

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.test.context.junit.jupiter.SpringExtension

@DataMongoTest
@ExtendWith(SpringExtension::class)
class OptimisticLockingTest(
        @Autowired
        val simpleMongoRepository: SimpleMongoRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Test
    fun test() {
        val document = SimpleDocument(message = "hello!")
        val savedDocument = simpleMongoRepository.save(document)

        val document1 = simpleMongoRepository.findById(savedDocument.id!!).get()
        val document2 = simpleMongoRepository.findById(savedDocument.id!!).get()

        document1.message = "updated"
        document2.message = "updated"

        val storedDocument1 = simpleMongoRepository.save(document1)
        assertThatThrownBy() { simpleMongoRepository.save(document2) }
                .isInstanceOf(OptimisticLockingFailureException::class.java)
                .hasMessageContaining("Has it been modified meanwhile?")

        log.info("document1: $document1, document2: $document2")
        log.info("storedDocument1: $storedDocument1")
    }
}
