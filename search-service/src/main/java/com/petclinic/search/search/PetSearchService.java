package com.petclinic.search.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.petclinic.search.index.PetDocument;
import com.petclinic.search.web.Caller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Full-text search over the pets index.
 *
 * <ul>
 *   <li><b>Typo tolerant:</b> {@code fuzziness: AUTO} ("mlu" finds "Milu").</li>
 *   <li><b>Search as you type:</b> {@code bool_prefix} over the {@code search_as_you_type} name field
 *       ("mi" finds "Milu").</li>
 *   <li><b>Vietnamese without diacritics:</b> the {@code vi_folded} analyzer (asciifolding) indexes
 *       "Nguyễn" as "nguyen", so both spellings match.</li>
 *   <li><b>Phone lookup</b> for the reception desk: a digits-only query also matches the owner's phone by prefix.</li>
 *   <li><b>Authorization in the query:</b> a customer's search always carries a {@code ownerUserId}
 *       filter, so another customer's pets can't appear in hits, totals or facets (no IDOR through search).</li>
 * </ul>
 */
@Service
public class PetSearchService {

    static final int MAX_PAGE_SIZE = 50;
    /** Elasticsearch's default index.max_result_window: from + size beyond it is rejected. */
    static final int MAX_RESULT_WINDOW = 10_000;
    static final int MAX_QUERY_LENGTH = 100;
    static final String SPECIES_AGG = "species";

    private static final Pattern SPECIES = Pattern.compile("[A-Za-z_]{1,32}");
    private static final Pattern PHONE_LIKE = Pattern.compile("[0-9 .+()-]+");

    private final ElasticsearchClient client;
    private final String alias;

    public PetSearchService(ElasticsearchClient client, @Value("${app.search.index-alias}") String alias) {
        this.client = client;
        this.alias = alias;
    }

    public PetSearchResponse search(Caller caller, String query, String species, int page, int size) throws IOException {
        SearchResponse<PetDocument> response = client.search(
                buildRequest(alias, caller, query, species, page, size), PetDocument.class);

        List<PetSearchResponse.PetHit> items = response.hits().hits().stream()
                .filter(hit -> hit.source() != null)
                .map(PetSearchService::toHit)
                .toList();
        long total = response.hits().total() != null ? response.hits().total().value() : items.size();
        return new PetSearchResponse(total, page, size, items, speciesCounts(response.aggregations().get(SPECIES_AGG)));
    }

    static SearchRequest buildRequest(String index, Caller caller, String query, String species, int page, int size) {
        validate(query, species, page, size);
        BoolQuery.Builder bool = new BoolQuery.Builder();

        String text = query == null ? "" : query.trim();
        if (!text.isEmpty()) {
            bool.must(must -> must.bool(any -> {
                // search as you type: the last word is a prefix ("mi" -> "Milu"). bool_prefix does
                // not apply fuzziness to that last word, hence the separate fuzzy clause below.
                any.should(s -> s.multiMatch(m -> m
                        .query(text)
                        .type(TextQueryType.BoolPrefix)
                        .fields("petName", "petName._2gram", "petName._3gram")
                        .boost(2.0f)));
                // typo tolerant full words over every text field ("mlu" -> "Milu")
                any.should(s -> s.multiMatch(m -> m
                        .query(text)
                        .type(TextQueryType.BestFields)
                        .fields("petName^3", "ownerName^2", "breed", "allergies")
                        .fuzziness("AUTO")
                        .prefixLength(1))); // first letter must match: fewer, cheaper expansions
                String digits = text.replaceAll("\\D", "");
                if (digits.length() >= 3 && PHONE_LIKE.matcher(text).matches()) {
                    any.should(s -> s.prefix(p -> p.field("ownerPhone").value(digits).boost(5.0f)));
                }
                return any.minimumShouldMatch("1");
            }));
        }
        if (species != null && !species.isBlank()) {
            String normalized = species.trim().toUpperCase(Locale.ROOT);
            bool.filter(f -> f.term(t -> t.field("species").value(normalized)));
        }
        if (!caller.isStaff()) {
            // filter context: not scored, cached, and applied to hits AND aggregations
            bool.filter(f -> f.term(t -> t.field("ownerUserId").value(caller.userId())));
        }

        BoolQuery boolQuery = bool.build();
        return SearchRequest.of(s -> s
                .index(index)
                .query(q -> q.bool(boolQuery))
                .from(page * size)
                .size(size)
                .trackTotalHits(t -> t.enabled(true))
                .sort(o -> o.score(sc -> sc.order(SortOrder.Desc)))
                .sort(o -> o.field(f -> f.field("petId").order(SortOrder.Desc))) // stable paging on ties
                .aggregations(SPECIES_AGG, a -> a.terms(t -> t.field("species").size(10))));
    }

    private static void validate(String query, String species, int page, int size) {
        if (page < 0) {
            throw new InvalidSearchRequestException("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidSearchRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if ((long) (page + 1) * size > MAX_RESULT_WINDOW) {
            throw new InvalidSearchRequestException("Too deep: only the first " + MAX_RESULT_WINDOW + " results can be paged");
        }
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new InvalidSearchRequestException("q must be at most " + MAX_QUERY_LENGTH + " characters");
        }
        if (species != null && !species.isBlank() && !SPECIES.matcher(species.trim()).matches()) {
            throw new InvalidSearchRequestException("Invalid species");
        }
    }

    private static PetSearchResponse.PetHit toHit(Hit<PetDocument> hit) {
        PetDocument d = hit.source();
        return new PetSearchResponse.PetHit(d.petId(), d.petName(), d.species(), d.breed(), d.allergies(),
                d.ownerId(), d.ownerName(), d.ownerPhone(), hit.score());
    }

    private static Map<String, Long> speciesCounts(Aggregate aggregate) {
        Map<String, Long> counts = new LinkedHashMap<>();
        if (aggregate != null && aggregate.isSterms()) {
            for (StringTermsBucket bucket : aggregate.sterms().buckets().array()) {
                counts.put(bucket.key().stringValue(), bucket.docCount());
            }
        }
        return counts;
    }
}
