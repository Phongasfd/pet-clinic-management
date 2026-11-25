package com.petclinic.search.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.petclinic.events.PetEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PetIndexerTest {

    private ElasticsearchClient client;
    private PetIndexer indexer;

    @BeforeEach
    void setUp() {
        client = mock(ElasticsearchClient.class);
        indexer = new PetIndexer(client, "pets");
    }

    private static PetEvent event(long version) {
        return PetEvent.newBuilder()
                .setEventId("e-" + version)
                .setEventType("PET_UPDATED")
                .setPetId(42)
                .setPetName("Milu")
                .setSpecies("DOG")
                .setOwnerId(7)
                .setOwnerUserId(1007)
                .setOwnerName("Nguyễn Văn An")
                .setAllergies("penicillin")
                .setTimestamp(1_700_000_000_000L)
                .setVersion(version)
                .build();
    }

    private static ElasticsearchException esError(int status, String type) {
        return new ElasticsearchException("index", ErrorResponse.of(r -> r
                .status(status)
                .error(e -> e.type(type).reason("test"))));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void writesThroughTheAliasWithExternalVersion() throws Exception {
        assertEquals(PetIndexer.Outcome.INDEXED, indexer.index(event(2)));

        ArgumentCaptor<IndexRequest> captor = ArgumentCaptor.forClass(IndexRequest.class);
        verify(client).index(captor.capture());
        IndexRequest<PetDocument> request = captor.getValue();
        assertEquals("pets", request.index());
        assertEquals("42", request.id(), "one document per pet: a newer event replaces it");
        assertEquals(3L, request.version(), "pets.version + 1");
        assertEquals(VersionType.External, request.versionType());
        assertEquals(Boolean.TRUE, request.requireAlias());
        assertEquals(1007L, request.document().ownerUserId());
        assertEquals("penicillin", request.document().allergies());
    }

    @Test
    @SuppressWarnings("unchecked")
    void staleOrDuplicateEventIsSkippedNotRetried() throws Exception {
        when(client.index(any(IndexRequest.class))).thenThrow(esError(409, PetIndexer.VERSION_CONFLICT));

        assertEquals(PetIndexer.Outcome.STALE, indexer.index(event(1)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void otherElasticsearchErrorsPropagateForRetry() throws Exception {
        when(client.index(any(IndexRequest.class))).thenThrow(esError(404, "index_not_found_exception"));

        assertThrows(ElasticsearchException.class, () -> indexer.index(event(1)));
    }

    @Test
    void walkInOwnerAndEmptyStringsBecomeNull() {
        PetDocument doc = PetDocument.from(PetEvent.newBuilder()
                .setPetId(1).setPetName("Lu").setSpecies("CAT").setOwnerId(3).build());

        assertNull(doc.ownerUserId(), "0 = walk-in owner: no customer can match it");
        assertNull(doc.breed());
        assertNull(doc.allergies());
    }
}
