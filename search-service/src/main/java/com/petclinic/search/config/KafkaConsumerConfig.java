package com.petclinic.search.config;

import com.petclinic.search.kafka.InvalidPetEventException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Retry and dead-letter policy of the indexer.
 *
 * <ul>
 *   <li><b>Elasticsearch down</b> (IOException, 5xx): retried with exponential backoff
 *       (1s, 2s, 4s ... capped at 30s, 6 retries), since it usually comes back.</li>
 *   <li><b>Corrupt payload</b> ({@link InvalidPetEventException}): retrying can't fix it, so it goes
 *       to the dead-letter topic at once instead of blocking the partition for a minute.</li>
 * </ul>
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    /** Same as pet-service and analytics-service: the topic and its DLT share one partition count. */
    static final int PET_EVENTS_PARTITIONS = 3;

    @Value("${app.kafka.topics.pet-events}")
    private String petEventsTopic;

    @Value("${app.kafka.topics.dead-letter}")
    private String deadLetterTopic;

    /** Declared here too, so whichever service boots first, the topic has 3 partitions. */
    @Bean
    public NewTopic petEventsTopic() {
        return TopicBuilder.name(petEventsTopic).partitions(PET_EVENTS_PARTITIONS).replicas(1).build();
    }

    /** The recoverer keeps the partition number, so the DLT needs as many partitions as the topic. */
    @Bean
    public NewTopic searchDeadLetterTopic() {
        return TopicBuilder.name(deadLetterTopic).partitions(PET_EVENTS_PARTITIONS).replicas(1).build();
    }

    @Bean
    public CommonErrorHandler errorHandler(KafkaOperations<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, e) -> new TopicPartition(deadLetterTopic, record.partition()));

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(6);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(30_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(InvalidPetEventException.class);
        return handler;
    }
}
