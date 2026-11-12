#!/usr/bin/env bash
# Builds the five service images as petclinic/<service>:<tag> (default tag: dev).
# Same build contexts as docker-compose.yml: services that need proto-common build from the
# repository root.
set -euo pipefail
cd "$(dirname "$0")/../.."

TAG="${1:-dev}"

docker build -t "petclinic/api-gateway:${TAG}" api-gateway
docker build -t "petclinic/auth-service:${TAG}" auth-service
for svc in pet-service billing-service analytics-service; do
  docker build -t "petclinic/${svc}:${TAG}" -f "${svc}/Dockerfile" .
done
