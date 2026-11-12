#!/bin/sh
# Run by the outbox-maintenance CronJob (outbox-maintenance.yaml) against pet_clinic_pets.
#
# 1. Deletes delivered outbox rows older than RETENTION_DAYS, so the table does not grow
#    forever (the relay only reads undelivered rows, but the table and its index still grow).
# 2. Looks for "dead" rows: undelivered and already tried MAX_ATTEMPTS times. OutboxRelay
#    never retries those, so a person has to look at last_error and replay them. If there
#    are any, the script prints them and exits 1: the Job is marked Failed, which is what
#    `kubectl get jobs` and alerting on kube_job_status_failed pick up.
set -eu

: "${DB_HOST:?}" "${DB_PORT:?}" "${DB_NAME:?}" "${PGUSER:?}" "${PGPASSWORD:?}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-10}"

psql_q() {
  psql -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" -v ON_ERROR_STOP=1 -X -q -At "$@"
}

deleted=$(psql_q -c "
  WITH gone AS (
    DELETE FROM outbox_events
    WHERE processed_at IS NOT NULL
      AND processed_at < NOW() - make_interval(days => ${RETENTION_DAYS})
    RETURNING 1)
  SELECT count(*) FROM gone")
echo "deleted ${deleted} delivered outbox rows older than ${RETENTION_DAYS} days"

pending=$(psql_q -c "SELECT count(*) FROM outbox_events WHERE processed_at IS NULL")
echo "undelivered rows (relay still retrying or dead): ${pending}"

dead=$(psql_q -c "
  SELECT count(*) FROM outbox_events
  WHERE processed_at IS NULL AND attempts >= ${MAX_ATTEMPTS}")
if [ "$dead" -gt 0 ]; then
  echo "ALERT: ${dead} outbox rows reached ${MAX_ATTEMPTS} attempts and will not be retried:"
  psql_q -F ' | ' -c "
    SELECT id, event_type, aggregate_id, attempts, left(coalesce(last_error, ''), 200)
    FROM outbox_events
    WHERE processed_at IS NULL AND attempts >= ${MAX_ATTEMPTS}
    ORDER BY id LIMIT 50"
  echo "Fix the cause, then replay with: UPDATE outbox_events SET attempts = 0, next_attempt_at = NOW() WHERE id IN (...);"
  exit 1
fi
echo "no dead outbox rows"
