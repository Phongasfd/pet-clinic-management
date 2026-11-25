package com.petclinic.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Read side of the pet data (CQRS): consumes pet-events from Kafka into an Elasticsearch index
 * and serves full-text search. pet-service stays the source of truth; this index can be dropped
 * and rebuilt from the events at any time.
 */
@SpringBootApplication
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
