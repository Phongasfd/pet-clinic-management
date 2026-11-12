#!/usr/bin/env bash
# Deletes the kind cluster and everything in it, volumes included.
set -euo pipefail
kind delete cluster --name "${CLUSTER:-petclinic}"
