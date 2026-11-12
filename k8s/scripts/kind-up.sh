#!/usr/bin/env bash
# One command from nothing to a running app on a local kind cluster:
#   k8s/scripts/kind-up.sh            build images, create cluster, deploy, wait
#   SKIP_BUILD=1 k8s/scripts/kind-up.sh   reuse the images already built
# Needs: docker, kind (0.24+), kubectl, openssl. Then: k8s/scripts/smoke-test.sh
set -euo pipefail
cd "$(dirname "$0")/../.."

CLUSTER="${CLUSTER:-petclinic}"
NS=petclinic
SERVICES="api-gateway auth-service pet-service billing-service analytics-service"
SECRETS=k8s/overlays/local/secrets.env

# A rollout that times out only says "timed out waiting for the condition". Print why,
# right here in the same output: pod states, recent events, and the pods' own words.
on_error() {
  echo "::group::kind-up failed: cluster state"
  kubectl get secrets,configmaps -A --field-selector metadata.namespace!=kube-system 2>/dev/null | grep -v kube-root-ca || true
  kubectl -n "$NS" get pods,pvc -o wide || true
  kubectl -n "$NS" get events --sort-by=.lastTimestamp | tail -n 40 || true
  for pod in $(kubectl -n "$NS" get pods -o name 2>/dev/null); do
    if ! kubectl -n "$NS" get "$pod" -o jsonpath='{.status.containerStatuses[*].ready}' | grep -q true; then
      echo "----- $pod"
      kubectl -n "$NS" describe "$pod" | sed -n '/^Containers:/,$p' | tail -n 40 || true
      kubectl -n "$NS" logs "$pod" --all-containers --tail=60 || true
    fi
  done
  echo "::endgroup::"
}
trap on_error ERR

if ! kind get clusters | grep -qx "$CLUSTER"; then
  kind create cluster --name "$CLUSTER" --config k8s/kind/cluster.yaml
fi
kubectl config use-context "kind-${CLUSTER}" >/dev/null

if [ "${SKIP_BUILD:-0}" != "1" ]; then
  k8s/scripts/build-images.sh dev
fi
# kind nodes cannot see the host's Docker images: copy them into the node
for svc in $SERVICES; do
  kind load docker-image "petclinic/${svc}:dev" --name "$CLUSTER"
done

if [ ! -f "$SECRETS" ]; then
  echo "creating $SECRETS with random values"
  cat > "$SECRETS" <<EOF
DB_USERNAME=petclinic_user
DB_PASSWORD=$(openssl rand -hex 16)
JWT_SECRET=$(openssl rand -base64 32)
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=$(openssl rand -hex 12)
EOF
fi

kubectl apply -k k8s/overlays/local

echo "waiting for the database and the broker..."
kubectl -n "$NS" rollout status statefulset/postgres --timeout=300s
kubectl -n "$NS" rollout status statefulset/kafka --timeout=300s
echo "waiting for the services (first start of five JVMs takes a few minutes)..."
for svc in $SERVICES; do
  kubectl -n "$NS" rollout status "deployment/${svc}" --timeout=600s
done

kubectl -n "$NS" get pods -o wide
echo
echo "Gateway: http://localhost:8080   (admin password: grep BOOTSTRAP_ADMIN_PASSWORD $SECRETS)"
echo "Try:     k8s/scripts/smoke-test.sh"
