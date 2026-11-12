#!/usr/bin/env bash
# End-to-end check of a deployed cluster, through the real gateway, gRPC, Kafka and databases:
#   1. register + login, create an owner and a pet through the gateway
#   2. another customer gets 403 on them (ownership rules)
#   3. analytics counts the pet (pet-service -> outbox -> Kafka -> analytics-service)
#   4. the outbox drains and billing has the owner's account (outbox -> gRPC -> billing)
#   5. NetworkPolicy: a random pod cannot reach pet-service, a gateway-labelled pod can
#   6. the outbox-maintenance CronJob runs successfully
# Usage: k8s/scripts/smoke-test.sh   (BASE_URL, NAMESPACE, SKIP_NETPOL=1 to skip step 5)
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
NS="${NAMESPACE:-petclinic}"
RUN="$(date +%s)$RANDOM"

step() { printf '\n== %s\n' "$*"; }
fail() {
  echo "FAIL: $*" >&2
  # an annotation can be read without a GitHub login, unlike the job log
  [ -n "${GITHUB_ACTIONS:-}" ] && echo "::error title=smoke-test::$(printf '%s' "$*" | tr '\n' ' ' | head -c 1500)"
  exit 1
}
# poll "description" timeout_seconds command...
poll() {
  local what="$1" timeout="$2"; shift 2
  local start=$SECONDS
  until "$@"; do
    (( SECONDS - start >= timeout )) && fail "timed out after ${timeout}s waiting for: $what"
    sleep 3
  done
}
psql_in() {  # psql_in <database> <sql>
  kubectl -n "$NS" exec postgres-0 -- sh -c "psql -U \"\$POSTGRES_USER\" -d $1 -At -c \"$2\""
}
json_str() { sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p"; }
json_num() { sed -n "s/.*\"$1\":\([0-9]*\).*/\1/p"; }

post() {  # post <path> <json> [token] -> prints body, then the HTTP code on the last line
  local auth=()
  [ -n "${3:-}" ] && auth=(-H "Authorization: Bearer $3")
  curl -s -w '\n%{http_code}' -X POST "$BASE$1" -H 'Content-Type: application/json' "${auth[@]}" -d "$2"
}
body() { sed '$d'; }
code() { tail -n1; }

login() {
  post /api/v1/auth/login "{\"username\":\"$1\",\"password\":\"$2\"}" | body | json_str token
}

step "gateway is up"
gateway_answers() {
  local c
  c=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/auth/login" \
        -H 'Content-Type: application/json' -d '{"username":"nobody","password":"x"}' || true)
  [ "$c" = "401" ]
}
poll "gateway -> auth-service answering 401 for bad credentials" 180 gateway_answers

step "customer signs up, logs in, creates an owner profile and a pet"
ALICE="alice_$RUN"
res=$(post /api/v1/auth/register "{\"username\":\"$ALICE\",\"password\":\"secret123\",\"fullName\":\"Alice\"}")
[ "$(echo "$res" | code)" = "200" ] || [ "$(echo "$res" | code)" = "201" ] || fail "register: $res"
TOKEN=$(login "$ALICE" secret123)
[ -n "$TOKEN" ] || fail "login returned no token"

BEFORE=$(curl -s "$BASE/api/v1/analytics/summary" -H "Authorization: Bearer $TOKEN" | json_num totalPets)
BEFORE=${BEFORE:-0}

PHONE="09$(printf '%08d' $(( (RANDOM * 32768 + RANDOM) % 100000000 )))"
res=$(post /api/v1/owners "{\"fullName\":\"Alice $RUN\",\"phoneNumber\":\"$PHONE\",\"email\":\"a$RUN@example.com\"}" "$TOKEN")
[ "$(echo "$res" | code)" = "201" ] || fail "create owner: $res"
OWNER_ID=$(echo "$res" | body | json_num id)
echo "owner id $OWNER_ID"

res=$(post /api/v1/pets "{\"name\":\"Milu\",\"species\":\"DOG\",\"ownerId\":$OWNER_ID}" "$TOKEN")
[ "$(echo "$res" | code)" = "201" ] || fail "create pet: $res"

step "another customer gets 403 on Alice's pets"
BOB="bob_$RUN"
post /api/v1/auth/register "{\"username\":\"$BOB\",\"password\":\"secret123\",\"fullName\":\"Bob\"}" >/dev/null
BOB_TOKEN=$(login "$BOB" secret123)
c=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/pets/owner/$OWNER_ID" -H "Authorization: Bearer $BOB_TOKEN")
[ "$c" = "403" ] || fail "expected 403 for Bob, got $c"

step "analytics counted the pet (outbox -> Kafka -> analytics-service), total was $BEFORE"
analytics_counted() {
  local now
  now=$(curl -s "$BASE/api/v1/analytics/summary" -H "Authorization: Bearer $TOKEN" | json_num totalPets)
  [ "${now:-0}" -gt "$BEFORE" ]
}
poll "totalPets > $BEFORE" 90 analytics_counted

step "outbox drained and billing account created (outbox -> gRPC -> billing-service)"
outbox_empty() { [ "$(psql_in pet_clinic_pets 'SELECT count(*) FROM outbox_events WHERE processed_at IS NULL')" = "0" ]; }
poll "no undelivered outbox rows" 90 outbox_empty
[ "$(psql_in pet_clinic_billing "SELECT count(*) FROM billing_accounts WHERE customer_id = $OWNER_ID")" = "1" ] \
  || fail "no billing account for owner $OWNER_ID"

if [ "${SKIP_NETPOL:-0}" != "1" ]; then
  step "NetworkPolicy: only the gateway may call pet-service"
  probe() {  # probe <pod-name> [labels] : exit 0 if pet-service answered
    kubectl -n "$NS" run "$1" --rm -i --quiet --restart=Never --image=busybox:1.36 ${2:+--labels="$2"} \
      -- wget -q -T 5 -O /dev/null http://pet-service:8082/actuator/health/liveness >/dev/null 2>&1
  }
  if probe "np-intruder-$RUN"; then
    fail "an unlabelled pod reached pet-service: NetworkPolicy not enforced (CNI without policy support?)"
  fi
  echo "unlabelled pod: blocked"
  probe "np-gateway-$RUN" "app.kubernetes.io/name=api-gateway" \
    || fail "a pod labelled as the gateway could not reach pet-service (policy too strict, or DNS down)"
  echo "gateway-labelled pod: allowed"
fi

step "outbox-maintenance CronJob runs"
JOB="outbox-smoke-$RUN"
kubectl -n "$NS" create job --from=cronjob/outbox-maintenance "$JOB" >/dev/null
if ! kubectl -n "$NS" wait --for=condition=complete "job/$JOB" --timeout=120s; then
  kubectl -n "$NS" logs "job/$JOB" || true
  fail "outbox-maintenance job did not complete"
fi
kubectl -n "$NS" logs "job/$JOB"

printf '\nALL CHECKS PASSED\n'
