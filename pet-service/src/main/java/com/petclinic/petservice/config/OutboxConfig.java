package com.petclinic.petservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Enables the scheduled {@code OutboxRelay} and provides the clock used for outbox timestamps
 * (injectable so tests can pin "now").
 */
@Configuration
@EnableScheduling
public class OutboxConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
