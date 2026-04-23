# Browser Service — Design Draft

> **Status:** design draft on Look-see branch `claude/extract-browsing-service-MgOw3`. Nothing in this directory is shipped yet. The contract below is the first thing up for review; once accepted, these artifacts move to the new repo `brandonkindred/browser-service` and implementation begins.

## What this is

A standalone HTTP service that exposes Selenium/Appium browser sessions to remote callers. Agents, audit services, and anything else that needs a browser can open a session, drive it, and close it — without embedding a WebDriver library.

The engine is being lifted almost verbatim from `LookseeCore/looksee-browser/` (already a self-contained Java module with zero intra-repo coupling) and wrapped in a Spring Boot REST layer. Selenium 3 / Appium 7 get upgraded to Selenium 4 / Appium 8 during the move.

## Who this is for

- **LookseeCore** — today's consumer. Existing services (PageBuilder, element-enrichment, journeyExecutor, audits, etc.) migrate behind a compatibility shim: `BrowserService` keeps its public signatures but delegates to an HTTP client instead of an in-process `Browser`.
- **Khala** ([brandonkindred/Khala-Agentic-AI-Teams](https://github.com/brandonkindred/Khala-Agentic-AI-Teams)) — Python agentic-teams project that needs a browsing capability. Talks to this service over plain HTTP; no Java dependency.
- **Anyone else** — the service is open source (MIT) and intentionally generic. No Look-see domain concepts leak into the public API.

## Session model

Browser interactions are stateful. The shape is: **create → interact → close**.

```bash
# 1. Open a session.
curl -X POST http://browser-service/v1/sessions \
  -H 'Content-Type: application/json' \
  -d '{"browser": "chrome", "environment": "discovery"}'
# -> {"session_id": "abc123", "expires_at": "2026-04-22T18:35:00Z", ...}

# 2. Navigate.
curl -X POST http://browser-service/v1/sessions/abc123/navigate \
  -H 'Content-Type: application/json' \
  -d '{"url": "https://example.com"}'

# 3. Find an element.
curl -X POST http://browser-service/v1/sessions/abc123/element/find \
  -H 'Content-Type: application/json' \
  -d '{"xpath": "//button[@id=\"submit\"]"}'
# -> {"element_handle": "el_42", "found": true, "displayed": true, ...}

# 4. Click it.
curl -X POST http://browser-service/v1/sessions/abc123/element/action \
  -H 'Content-Type: application/json' \
  -d '{"element_handle": "el_42", "action": "click"}'

# 5. Screenshot (bytes).
curl -X POST http://browser-service/v1/sessions/abc123/screenshot \
  -H 'Content-Type: application/json' \
  -d '{"strategy": "full_page_shutterbug"}' \
  --output page.png

# 6. Close.
curl -X DELETE http://browser-service/v1/sessions/abc123
```

Idle sessions expire after 5 minutes; all sessions expire after 30 minutes, no matter what. The registry reaps them automatically.

For the trivial "open → navigate → screenshot → close" path, `POST /v1/capture` collapses all of the above into one request.

## MVP scope

| Area | Decision |
|---|---|
| Transport | HTTP / JSON (OpenAPI 3.1 in `openapi.yaml`) |
| Screenshots | Default returns `image/png` bytes. `?encoding=base64` returns JSON for MCP / non-binary callers |
| Screenshot storage | **Caller uploads.** Service holds bytes only long enough to return them |
| Session model | Stateful sessions, opaque `session_id`, idle + absolute TTL |
| Mobile | Appium (Android / iOS) included. Same session API; mobile gestures use `/element/touch` |
| Engine | Selenium 4 + Appium Java Client 8 (upgraded from today's Selenium 3 / Appium 7) |
| Upstream | Existing Selenium Grid (in `LookseeIaC`) + optional BrowserStack — unchanged |
| Clients | Java client (for LookseeCore's shim). Khala uses the HTTP API directly |
| Concurrency | 20 concurrent sessions per instance; 429 once capped. Horizontal scale via replicas |
| Licence | MIT (inherited from Look-see) |

## Explicitly out of MVP

Listed here so nothing gets forgotten:

- **Authentication.** Service runs on a private network (VPC-only ingress). API keys / OAuth / per-tenant isolation deferred.
- **Rate limits / quotas.** Need auth first to key off.
- **Network egress policy.** SSRF guard against `localhost` / `169.254.169.254` / private CIDRs — defer to post-auth.
- **Streaming events.** No WebSocket or SSE at MVP. Page-load progress, console logs, network events, DOM mutations all come later if a caller asks.
- **MCP server.** The REST endpoints are named so each maps 1:1 to a future MCP tool (`browser.navigate`, `browser.screenshot`, etc.) but no MCP wrapper ships in MVP.
- **Live-view UI.** Vendors like Browserbase ship a debug panel (VNC/CDP stream). Possible later.
- **Cloud-storage backends.** Service never uploads to GCS/S3; callers handle storage.
- **Chrome extension loading.** `LookseeChromeExtension` is a user-facing product unrelated to this service and is not bundled.

## Files in this directory

- `openapi.yaml` — the full API contract. **Source of truth.** Each endpoint includes a pointer back to the engine method it wraps.
- `README.md` — this file.

## How to review

1. **Render the spec.** Paste `openapi.yaml` into <https://editor.swagger.io> or run `npx @redocly/cli preview-docs openapi.yaml` for a browsable view.
2. **Lint.** `npx @redocly/cli lint openapi.yaml` — must be clean.
3. **Coverage walk.** Open `LookseeCore/looksee-browser/src/main/java/com/looksee/browser/Browser.java` and `MobileDevice.java`. Every `public` method should map to an endpoint — or be intentionally collapsed (e.g., the four screenshot variants live under one endpoint with a `strategy` field).

## Engine → endpoint map (for the coverage walk)

| Engine method (`Browser.java` / `MobileDevice.java`) | Endpoint |
|---|---|
| `navigateTo` + `waitForPageToLoad` | `POST /sessions/{id}/navigate` |
| `close` | `DELETE /sessions/{id}` |
| `getViewportScreenshot` | `POST /sessions/{id}/screenshot` with `strategy=viewport` |
| `getFullPageScreenshot` | `POST /sessions/{id}/screenshot` with `strategy=full_page_shutterbug` |
| `getFullPageScreenshotAshot` | `POST /sessions/{id}/screenshot` with `strategy=full_page_ashot` |
| `getFullPageScreenshotShutterbug` | `POST /sessions/{id}/screenshot` with `strategy=full_page_shutterbug_paused` |
| `getElementScreenshot` | `POST /sessions/{id}/element/screenshot` |
| `findElement` / `findWebElementByXpath` / `isDisplayed` / `extractAttributes` | `POST /sessions/{id}/element/find` (combined) |
| `ActionFactory.execAction` | `POST /sessions/{id}/element/action` |
| `MobileActionFactory` (touch) | `POST /sessions/{id}/element/touch` |
| `removeElement` / `removeDriftChat` / `removeGDPRmodals` / `removeGDPR` | `POST /sessions/{id}/dom/remove` (preset + optional value) |
| `scrollToTopOfPage` / `scrollToBottomOfPage` / `scrollToElement` / `scrollToElementCentered` / `scrollDownPercent` / `scrollDownFull` | `POST /sessions/{id}/scroll` (mode) |
| `getViewportScrollOffset` + `getViewportSize` | `GET /sessions/{id}/viewport` |
| `moveMouseOutOfFrame` / `moveMouseToNonInteractive` | `POST /sessions/{id}/mouse/move` |
| `isAlertPresent` | `GET /sessions/{id}/alert` |
| `AlertChoice.ACCEPT` / `DISMISS` | `POST /sessions/{id}/alert/respond` |
| `getSource` | `GET /sessions/{id}/source` |
| `is503Error` + `driver.getCurrentUrl()` | `GET /sessions/{id}/status` |
| getters (`browserName`, `viewportSize`, scroll offsets) | `GET /sessions/{id}` and `GET /sessions/{id}/viewport` |
| — (escape hatch, no engine method) | `POST /sessions/{id}/execute` |

Engine methods that are intentionally **not** exposed:
- `getDriver()` — returning a live WebDriver over HTTP doesn't make sense.
- `waitForPageToLoad()` standalone — always called after `navigate`, so merged.
- Private helpers (`extractYOffset`, `extractViewportWidth`, etc.) — internal.

## Extraction program (full context)

This spec is **phase 0**. The whole program, for reviewers who want it:

| Phase | Work | Status |
|---|---|---|
| 0 | Lock the API contract (this directory) | in progress |
| 1 | Stand up `brandonkindred/browser-service` repo; subtree-move `looksee-browser`; upgrade Selenium 3→4 / Appium 7→8; build Spring Boot REST layer | blocked on phase 0 |
| 2 | Deploy to staging (Cloud Run via LookseeIaC pattern); load test | |
| 3 | LookseeCore shim: `BrowsingClient` HTTP client + `mode=local\|remote` flag on `BrowserService`. One `looksee-core` minor-version bump | |
| 4 | Per-consumer cutover (qa-testbed → element-enrichment → PageBuilder → audits → journeyExecutor/Expander). Each bumps `LOOKSEE_CORE_VERSION`, sets `mode=remote`, canaries | |
| 5 | Remove `mode=local` path; delete `looksee-browser` module from LookseeCore; major-version bump | |

Each phase ends in a shippable, revertible state.
