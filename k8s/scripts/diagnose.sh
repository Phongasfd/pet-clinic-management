#!/usr/bin/env bash
# Explains an unhealthy deployment: pod states, recent events, and what the pods say.
#   k8s/scripts/diagnose.sh        only the pods that are not Ready
#   k8s/scripts/diagnose.sh all    every app pod (after a smoke-test failure)
# In GitHub Actions the same text is also written as job annotations: anyone can read
# those through the API, while job logs need a GitHub login.
set -uo pipefail

NS="${NAMESPACE:-petclinic}"
MODE="${1:-unready}"
MAX_POD_NOTES=7   # GitHub keeps at most 10 error annotations per step

note() {  # note <title>  (body on stdin)
  local title="$1" body
  body=$(cat)
  printf '\n===== %s\n%s\n' "$title" "$body"
  if [ -n "${GITHUB_ACTIONS:-}" ]; then
    body=$(printf '%s\n' "$body" | tail -c 3500 | sed -e 's/%/%25/g' -e 's/\r//g' | awk 'BEGIN { ORS = "%0A" } { print }')
    echo "::error title=${title}::${body}"
  fi
}

{
  kubectl -n "$NS" get pods,pvc -o wide
  echo
  echo "secrets outside kube-system:"
  kubectl get secrets -A --no-headers 2>/dev/null | grep -v -e '^kube-' -e 'kube-root-ca' || true
} 2>&1 | note "pods"

kubectl -n "$NS" get events --sort-by=.lastTimestamp 2>&1 | tail -n 30 | note "events"

count=0
for pod in $(kubectl -n "$NS" get pods -o name 2>/dev/null); do
  phase=$(kubectl -n "$NS" get "$pod" -o jsonpath='{.status.phase}')
  [ "$phase" = "Succeeded" ] && continue                      # finished Job pods
  ready=$(kubectl -n "$NS" get "$pod" -o jsonpath='{.status.containerStatuses[*].ready}')
  if [ "$MODE" != "all" ] && [ -n "$ready" ] && ! echo "$ready" | grep -q false; then
    continue                                                    # healthy
  fi
  count=$((count + 1))
  [ "$count" -gt "$MAX_POD_NOTES" ] && break
  {
    kubectl -n "$NS" describe "$pod" \
      | grep -E '^(Init Containers|Containers):|^  [a-z0-9-]+:$|^    (State|Reason|Message|Exit Code|Last State|Ready|Restart Count):' || true
    echo "--- logs"
    kubectl -n "$NS" logs "$pod" --all-containers --tail=30 2>&1 || true
    if kubectl -n "$NS" get "$pod" -o jsonpath='{.status.containerStatuses[*].restartCount}' | grep -q '[1-9]'; then
      echo "--- logs before the last restart"
      kubectl -n "$NS" logs "$pod" --all-containers --previous --tail=30 2>&1 || true
    fi
  } | note "${pod#pod/}"
done
exit 0
