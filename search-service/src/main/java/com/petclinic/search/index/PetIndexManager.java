package com.petclinic.search.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.ResponseException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

/**
 * Creates the {@code pets-v1} index (analyzers + strict mapping from {@code elasticsearch/pets-v1.json})
 * behind the {@code pets} alias, if the alias does not exist yet.
 *
 * <p>Runs in {@link #afterPropertiesSet()}, i.e. while the context is being built and before the
 * Kafka listener containers start, so no event is ever indexed before the mapping exists.
 *
 * <p>Index and alias are created in one request, so a second replica starting at the same time
 * either sees the alias or gets {@code resource_already_exists_exception}, which is fine.
 */
@Slf4j
@Component
public class PetIndexManager implements InitializingBean {

    static final String ALREADY_EXISTS = "resource_already_exists_exception";

    private final ElasticsearchClient client;
    private final String alias;
    private final String indexName;
    private final int startupAttempts;
    private final Duration retryDelay;

    public PetIndexManager(ElasticsearchClient client,
                           @Value("${app.search.index-alias}") String alias,
                           @Value("${app.search.index-name}") String indexName,
                           @Value("${app.search.startup-attempts:30}") int startupAttempts,
                           @Value("${app.search.startup-retry-delay:2s}") Duration retryDelay) {
        this.client = client;
        this.alias = alias;
        this.indexName = indexName;
        this.startupAttempts = startupAttempts;
        this.retryDelay = retryDelay;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (int attempt = 1; ; attempt++) {
            try {
                ensureIndex();
                return;
            } catch (IOException e) { // Elasticsearch not reachable yet
                if (attempt >= startupAttempts) {
                    throw new IllegalStateException("Elasticsearch unreachable after " + attempt + " attempts", e);
                }
                log.warn("Elasticsearch not ready (attempt {}/{}): {}", attempt, startupAttempts, e.getMessage());
                Thread.sleep(retryDelay.toMillis());
            }
        }
    }

    /** @return true if this call created the index, false if the alias was already there */
    public boolean ensureIndex() throws IOException {
        if (client.indices().existsAlias(a -> a.name(alias)).value()) {
            log.info("Search alias '{}' exists", alias);
            return false;
        }
        try (InputStream definition = new ClassPathResource("elasticsearch/" + indexName + ".json").getInputStream()) {
            client.indices().create(c -> c
                    .withJson(definition)
                    .index(indexName)
                    .aliases(alias, a -> a.isWriteIndex(true)));
            log.info("Created index '{}' behind alias '{}'", indexName, alias);
            return true;
        } catch (ElasticsearchException | ResponseException e) {
            if (ElasticsearchErrors.is(e, 400, ALREADY_EXISTS)) {
                log.info("Index '{}' was created concurrently by another instance", indexName);
                return false;
            }
            throw e;
        }
    }
}
