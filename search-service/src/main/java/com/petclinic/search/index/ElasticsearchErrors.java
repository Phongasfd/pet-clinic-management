package com.petclinic.search.index;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import org.elasticsearch.client.ResponseException;

/**
 * Recognizes an Elasticsearch error by HTTP status and error type, whichever way the client
 * reports it: the typed {@link ElasticsearchException}, or, as the 8.13 RestClient transport does
 * for e.g. a 409 version conflict, the low-level {@link ResponseException} (an IOException) whose
 * message holds the raw error body. Treating the latter as an outage would retry a harmless
 * duplicate six times and then dead-letter it.
 */
final class ElasticsearchErrors {

    private ElasticsearchErrors() {
    }

    static boolean is(Exception e, int status, String type) {
        if (e instanceof ElasticsearchException es) {
            return es.status() == status && es.error() != null && type.equals(es.error().type());
        }
        if (e instanceof ResponseException re) {
            return re.getResponse().getStatusLine().getStatusCode() == status
                    && String.valueOf(re.getMessage()).contains("\"type\":\"" + type + "\"");
        }
        return false;
    }
}
