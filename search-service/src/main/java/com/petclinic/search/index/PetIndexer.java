package com.petclinic.search.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.petclinic.events.PetEvent;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.ResponseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes pet snapshots to Elasticsearch with <b>external versioning</b>.
 *
 * <p>Delivery is at-least-once and not always in order: the outbox relay retries a failed row
 * with backoff, so a PET_UPDATED can reach Kafka before an older event of the same pet that was
 * waiting for its retry. Each event carries {@code pets.version}; Elasticsearch accepts a write
 * only if its version is <i>higher</i> than the stored one. So:
 * <ul>
 *   <li>a redelivered event (same version) is rejected: idempotent without a processed-events table;</li>
 *   <li>an old event arriving late (lower version) can't overwrite newer data.</li>
 * </ul>
 * Both come back as a version conflict, which is the expected outcome here, not an error.
 */
@Slf4j
@Component
public class PetIndexer {

    static final String VERSION_CONFLICT = "version_conflict_engine_exception";

    public enum Outcome { INDEXED, STALE }

    private final ElasticsearchClient client;
    private final String alias;

    public PetIndexer(ElasticsearchClient client, @Value("${app.search.index-alias}") String alias) {
        this.client = client;
        this.alias = alias;
    }

    public Outcome index(PetEvent event) throws IOException {
        IndexRequest<PetDocument> request = IndexRequest.of(b -> b
                .index(alias)
                .id(String.valueOf(event.getPetId()))
                .document(PetDocument.from(event))
                .version(externalVersion(event))
                .versionType(VersionType.External)
                // fail instead of letting Elasticsearch auto-create a concrete index named "pets"
                // (dynamic mapping, no analyzer) if the alias is missing
                .requireAlias(true));
        try {
            client.index(request);
            log.info("Indexed pet {} at version {}", event.getPetId(), externalVersion(event));
            return Outcome.INDEXED;
        } catch (ElasticsearchException | ResponseException e) {
            if (ElasticsearchErrors.is(e, 409, VERSION_CONFLICT)) {
                log.info("Skipped stale or duplicate event {} for pet {} (version {})",
                        event.getEventId(), event.getPetId(), externalVersion(event));
                return Outcome.STALE;
            }
            throw e;
        }
    }

    /**
     * pets.version starts at 0; shifted by one so the very first event is a normal version
     * (events written before V5 have no version field and read as 0, i.e. version 1).
     */
    static long externalVersion(PetEvent event) {
        return event.getVersion() + 1;
    }
}
