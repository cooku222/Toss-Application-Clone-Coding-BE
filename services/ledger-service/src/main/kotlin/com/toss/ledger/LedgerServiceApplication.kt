package com.toss.ledger

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication(scanBasePackages = ["com.toss.ledger", "com.toss.shared"])
@EnableFeignClients
@EnableJpaAuditing
class LedgerServiceApplication

fun main(args: Array<String>) {
    runApplication<LedgerServiceApplication>(*args)
}
