# k8s/

Kubernetes manifests for Pet Clinic, written with Kustomize.

| Path | What |
| --- | --- |
| `base/` | The app: 5 Deployments + Services, PostgreSQL and Kafka StatefulSets, ConfigMap, HPAs, NetworkPolicies, the `outbox-maintenance` CronJob |
| `overlays/local/` | kind / minikube: locally built `:dev` images, Secret generated from `secrets.env` (git-ignored), gateway on NodePort 30080 |
| `overlays/prod/` | Registry images, 2 replicas minimum on the request path, PodDisruptionBudgets, Ingress, topology spread; the Secret is created out of band |
| `kind/cluster.yaml` | One-node kind cluster mapping `localhost:8080` to the gateway |
| `scripts/` | `kind-up.sh` (everything), `build-images.sh`, `smoke-test.sh` (end to end), `kind-down.sh` |

```bash
k8s/scripts/kind-up.sh && k8s/scripts/smoke-test.sh
```

Full guide, in Vietnamese, with the reasoning behind each feature and hands-on labs: [../docs/kubernetes.md](../docs/kubernetes.md).
