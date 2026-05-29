package com.nova.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class NovaLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovaLedgerApplication.class, args);
    }
}
