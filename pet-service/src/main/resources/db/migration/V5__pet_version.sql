-- Optimistic locking on pets (JPA @Version). Two concurrent health-info updates no longer
-- silently overwrite each other: the second one fails with 409 instead of a lost update.
-- The same number is sent in PET_CREATED / PET_UPDATED events, and search-service uses it as the
-- Elasticsearch external version, so an older event can never overwrite a newer document.
ALTER TABLE pets ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
