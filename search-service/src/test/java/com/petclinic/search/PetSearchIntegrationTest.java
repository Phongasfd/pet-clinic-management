package com.petclinic.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.petclinic.events.PetEvent;
import com.petclinic.search.index.PetDocument;
import com.petclinic.search.index.PetIndexManager;
import com.petclinic.search.index.PetIndexer;
import com.petclinic.search.search.PetSearchResponse;
import com.petclinic.search.search.PetSearchService;
import com.petclinic.search.web.Caller;
import com.petclinic.search.web.Role;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Against a real Elasticsearch, with the real mapping and analyzers: proves what unit tests
 * can't (asciifolding, fuzziness, external versioning).
 *
 * <p>Starts Elasticsearch 8.13 with Testcontainers (CI). Without Docker, set
 * {@code ELASTICSEARCH_TEST_URL=http://localhost:9200} to use a running node instead;
 * with neither, the test is skipped.
 */
class PetSearchIntegrationTest {

    private static final String EXTERNAL_URL = System.getenv("ELASTICSEARCH_TEST_URL");

    private static ElasticsearchContainer container;

    private static final Caller VET = new Caller(1, Role.VET);

    private static RestClient restClient;
    private static ElasticsearchClient client;
    private static PetIndexer indexer;
    private static PetSearchService search;

    @BeforeAll
    static void setUp() throws Exception {
        String url = EXTERNAL_URL;
        if (url == null || url.isBlank()) {
            assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                    "Neither Docker nor ELASTICSEARCH_TEST_URL available");
            container = new ElasticsearchContainer(
                    DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.13.4"))
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
            container.start();
            url = "http://" + container.getHttpHostAddress();
        }
        restClient = RestClient.builder(HttpHost.create(url)).build();
        client = new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
        // a reused external node may still have the index from the previous run
        client.indices().delete(d -> d.index("pets-v1").ignoreUnavailable(true));

        PetIndexManager manager = new PetIndexManager(client, "pets", "pets-v1", 1, Duration.ZERO);
        assertTrue(manager.ensureIndex(), "first start creates the index");
        assertFalse(manager.ensureIndex(), "second start (or second replica) reuses it");
        indexer = new PetIndexer(client, "pets");
        search = new PetSearchService(client, "pets");
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (restClient != null) {
            restClient.close();
        }
        if (container != null) {
            container.stop();
        }
    }

    @BeforeEach
    void emptyIndex() throws Exception {
        client.deleteByQuery(d -> d.index("pets").query(q -> q.matchAll(m -> m)).refresh(true));
    }

    private static PetEvent pet(long petId, String name, String species, long ownerUserId, String ownerName,
                                String phone, String allergies, long version) {
        return PetEvent.newBuilder()
                .setEventId("e-" + petId + "-" + version)
                .setEventType(version == 0 ? "PET_CREATED" : "PET_UPDATED")
                .setPetId(petId).setPetName(name).setSpecies(species)
                .setOwnerId(petId * 10).setOwnerUserId(ownerUserId).setOwnerName(ownerName)
                .setOwnerPhone(phone).setAllergies(allergies)
                .setTimestamp(1_700_000_000_000L + version)
                .setVersion(version)
                .build();
    }

    private static void index(PetEvent... events) throws Exception {
        for (PetEvent event : events) {
            indexer.index(event);
        }
        client.indices().refresh(r -> r.index("pets"));
    }

    private static List<String> names(PetSearchResponse response) {
        return response.items().stream().map(PetSearchResponse.PetHit::petName).toList();
    }

    @Test
    void findsVietnameseNamesWithoutDiacritics() throws Exception {
        index(pet(1, "Mướp", "CAT", 0, "Nguyễn Văn An", "0911111111", "", 0));

        assertEquals(List.of("Mướp"), names(search.search(VET, "nguyen van an", null, 0, 20)));
        assertEquals(List.of("Mướp"), names(search.search(VET, "muop", null, 0, 20)));
    }

    @Test
    void toleratesTyposAndSearchesAsYouType() throws Exception {
        index(pet(2, "Milu", "DOG", 0, "Tran Thi B", "0922222222", "", 0),
              pet(3, "Bông", "CAT", 0, "Le Van C", "0933333333", "", 0));

        assertEquals(List.of("Milu"), names(search.search(VET, "mlu", null, 0, 20)), "one typo");
        assertEquals(List.of("Milu"), names(search.search(VET, "mi", null, 0, 20)), "prefix while typing");
    }

    @Test
    void findsByOwnerPhonePrefixAndAllergy() throws Exception {
        index(pet(4, "Lucky", "DOG", 0, "Pham D", "0912345678", "penicillin", 0),
              pet(5, "Kitty", "CAT", 0, "Pham E", "0987654321", "", 0));

        assertEquals(List.of("Lucky"), names(search.search(VET, "0912 345", null, 0, 20)));
        assertEquals(List.of("Lucky"), names(search.search(VET, "penicilin", null, 0, 20)), "vets look up allergies");
    }

    @Test
    void customerFindsOnlyTheirOwnPetsEvenInFacets() throws Exception {
        index(pet(6, "Lu", "DOG", 1001, "Owner A", "0900000001", "", 0),
              pet(7, "Lu", "CAT", 1002, "Owner B", "0900000002", "", 0),
              pet(8, "Lu", "CAT", 0, "Walk-in", "0900000003", "", 0));

        PetSearchResponse mine = search.search(new Caller(1001, Role.CUSTOMER), "lu", null, 0, 20);
        assertEquals(1, mine.total());
        assertEquals(6L, mine.items().get(0).petId());
        assertEquals(Map.of("DOG", 1L), mine.speciesCounts(), "no leak through aggregations");

        PetSearchResponse staff = search.search(VET, "lu", null, 0, 20);
        assertEquals(3, staff.total());
        assertEquals(2L, staff.speciesCounts().get("CAT"));
    }

    @Test
    void replicaLosingTheCreateRaceStartsNormally() throws Exception {
        // its alias check said "missing", but another instance created pets-v1 meanwhile
        PetIndexManager racer = new PetIndexManager(client, "pets-race", "pets-v1", 1, Duration.ZERO);

        assertFalse(racer.ensureIndex(), "resource_already_exists_exception is not a failure");
    }

    @Test
    void olderOrDuplicateEventNeverOverwritesNewerData() throws Exception {
        PetEvent created = pet(9, "Rex", "DOG", 0, "Owner", "0900000009", "", 0);
        PetEvent updated = pet(9, "Rex", "DOG", 0, "Owner", "0900000009", "penicillin", 1);

        // the update overtakes the create (outbox retry with backoff)
        assertEquals(PetIndexer.Outcome.INDEXED, indexer.index(updated));
        assertEquals(PetIndexer.Outcome.STALE, indexer.index(created), "late, older event is dropped");
        assertEquals(PetIndexer.Outcome.STALE, indexer.index(updated), "redelivery is a no-op");

        PetDocument stored = client.get(g -> g.index("pets").id("9"), PetDocument.class).source();
        assertNotNull(stored);
        assertEquals("penicillin", stored.allergies());
    }
}
