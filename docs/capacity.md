# Capacity model

> **Stub.** This document is a placeholder owned by issue **#141 (OPS3 — Capacity
> model documentation)**, which will expand it into the full sizing model
> (replicas per concurrent session, vCPU/memory budgets, Postgres connection
> arithmetic, cost projections at 100 / 500 / 1000 concurrent sessions, and
> autoscaling thresholds).
>
> Until then, only the single section below is authoritative.

## Single-pod constraint for `SessionRegistry`

The browser-service API is currently constrained to **exactly one Cloud Run
instance** (`min_instances = max_instances = 1`). Both bounds are pinned —
scale-out and scale-to-zero are both unsafe.

### Why

`SessionRegistry`
(`api/src/main/java/io/browserservice/api/session/SessionRegistry.java`) keeps
all live session state on the heap of the API JVM:

- A `ConcurrentHashMap<UUID, SessionHandle>` mapping session id → handle.
- A `Semaphore` enforcing the per-pod concurrent-session cap.

Each `SessionHandle` holds a live Selenium `WebDriver` reference, a
`ReentrantLock`, and an `AtomicBoolean`. None of this is serializable or
shareable between pods.

### Failure mode if relaxed prematurely

Two ways this breaks:

1. **`max_instances > 1`** — the load balancer can route a follow-up request
   (`POST /v1/sessions/{id}/navigate`, screenshot, `DELETE`, …) to a
   different pod from the one that created the session. That pod will not
   find the id in its in-memory map and will return
   `SessionNotFoundException` (HTTP 404). The original pod will keep the
   `WebDriver` open until the reaper TTL fires, leaking a Selenium worker
   in the interim. Customers see intermittent 404s under any kind of
   multi-pod deployment (canary, rolling, autoscaling burst).
2. **`min_instances = 0`** — Cloud Run is allowed to scale the sole API
   instance to zero between requests. The JVM is shut down and its heap
   (including `SessionRegistry`) is discarded. A follow-up
   `navigate` / `screenshot` / `DELETE` cold-starts a fresh instance with
   an empty registry → same 404, plus the WebDriver on the discarded
   instance has been hard-killed rather than gracefully closed.

### Enforcement

The constraint is enforced at the infrastructure layer:

- `terraform/variables.tf` — both `browser_service_min_instances` and
  `browser_service_max_instances` default to `1` and each carries a
  `validation` block that rejects any other value at `terraform plan`
  time with a pointer to this document and to issue #119.
- `terraform/modules/browser_service/main.tf` — inline comment above the
  `autoscaling.knative.dev/minScale` / `maxScale` annotations explaining
  the pin.
- `SessionRegistry.java` — class-level Javadoc explaining the constraint to
  anyone editing the registry.

### Lift path

Tracked by **issue #119 (R10 — Externalize `SessionRegistry` to Redis)**,
Phase 1 (P1, Effort L). Lifting requires either:

1. **Redis-backed registry** plus LB session affinity by `X-Session-Id`, so
   any pod can serve any request for an existing session (WebDriver
   references stay co-located with the owning pod, but the routing table is
   shared); or
2. **Redis-backed registry** plus Selenium Grid broker, so any pod can
   resolve and reattach to the WebDriver via the Grid (true horizontal
   scale, more invasive change).

When Phase 1 lands, the two `== 1` validations in `terraform/variables.tf`
and the inline comments in the module / `SessionRegistry` Javadoc should be
removed or rewritten.
