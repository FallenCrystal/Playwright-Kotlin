# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Playwright-Kotlin is a Kotlin/JVM client for Playwright browser automation. It communicates with a TypeScript server over TCP using NDJSON (newline-delimited JSON). The Kotlin API exposes suspend functions that mirror Playwright's native API.

## Build & Test Commands

### Prerequisites
- JVM 21+, Node.js 18+, npm
- Playwright browsers: `cd server && npx playwright install chromium`

### Server (TypeScript)
```bash
cd server && npm install        # install dependencies
cd server && npm run build      # compile TS → dist/ (required before Kotlin tests)
```

### Kotlin Client
```bash
./gradlew :playwright-kotlin:build    # compile
./gradlew :playwright-kotlin:test     # run all tests (requires server built first)
```

### Full build from scratch
```bash
cd server && npm install && npm run build && cd .. && ./gradlew :playwright-kotlin:test
```

## Architecture

**Two-process design:** A Kotlin client (Netty TCP) talks to a Node.js server that wraps real Playwright.

### Communication flow
```
Kotlin suspend fn → Connection.sendMessage(id, guid, method, params)
    → Netty TCP → NDJSON line →
        TS Server: ProtocolHandler → handler by type → real Playwright call
    ← JSON response (id, result/error) or event (guid, __event__) ←
← CompletableDeferred resolved → suspend fn returns
```

### Wire protocol
- **Request:** `{"id":1, "guid":"page-abc", "method":"goto", "params":{"url":"..."}}`
- **Response:** `{"id":1, "result":{...}}` or `{"id":1, "error":{"name":"...","message":"..."}}`
- **Event:** `{"guid":"page-abc", "method":"__event__", "params":{"type":"close"}}`

### Kotlin client (`playwright-kotlin/src/main/kotlin/io/playwright/kotlin/`)
- `Playwright.kt` — entry point; starts server subprocess, connects, initializes browser types
- `connection/Transport.kt` — Netty bootstrap with LineBasedFrameDecoder + JSON codecs
- `connection/Connection.kt` — request/response correlation via `ConcurrentHashMap<Long, CompletableDeferred>`, object registry, event dispatch
- `core/ChannelOwner.kt` — base class for all remote objects (Browser, Page, Locator, etc.); holds guid, sends messages via Connection
- `api/` — domain objects (BrowserType, Browser, BrowserContext, Page, Frame, Locator, ElementHandle, Response) that extend ChannelOwner
- `util/ServerProcess.kt` — launches `node server/dist/index.js`, reads `LISTENING:<port>` from stdout

### TypeScript server (`server/src/`)
- `server.ts` — TCP server, NDJSON framing, per-connection ObjectRegistry
- `protocolHandler.ts` — routes requests by object type to handler functions
- `objectRegistry.ts` — bidirectional GUID ↔ Playwright object mapping
- `handlers/` — one file per type (pageHandler.ts, locatorHandler.ts, etc.), translates protocol methods into real Playwright calls

### Key pattern: Object Registry + GUID
Both sides maintain a GUID → object mapping. When the server creates a new Playwright object (e.g., launching a browser returns a Browser), it registers it with a GUID and returns `{guid, type}`. The Kotlin client creates a corresponding ChannelOwner subclass and registers it locally. All subsequent calls reference objects by GUID.

## Dependencies (from gradle.properties)
- Kotlin 2.1.0, kotlinx-coroutines 1.9.0, kotlinx-serialization 1.7.3, Netty 4.1.115.Final
- Server: playwright 1.49.x, TypeScript 5.x
