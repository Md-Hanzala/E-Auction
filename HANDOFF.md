# E-AUCTION MASTER HANDOFF

> Durable project record for the E-Auction web application.
> This file is the **source of truth** for future AI sessions. Read it first.
> Last verified during the FINAL VALIDATION / PRE-OPTIMIZATION phase.

---

## 1. Project identity

| Item | Value |
|------|-------|
| Project name | E-Auction |
| Project root | `D:\E-Auction\EAuction` |
| Language / stack | Java (Jakarta EE 6.0 servlets), HTML/JS frontend, JDBC |
| Build tool | NetBeans (Ant via `nbproject`) — Clean & Build produces `dist\EAuction.war` |
| Servlet container | Apache Tomcat 10.1.57 |
| Web framework | Plain servlets + JSP/HTML + WebSocket (`AuctionWebSocket`) |
| Database | Oracle XE, local, JDBC |
| Package | `eauction` (all 19 servlets in `src\java\eauction`) |
| Runtime context | `/EAuction` |
| Server port | `8081` |

`README.md` exists but contains only the title line `# E-Auction`. It is
NOT a handoff document. This `HANDOFF.md` is the dedicated handoff file.

---

## 2. Current state

**Phase: FINAL VALIDATION / PRE-OPTIMIZATION.**

The major multi-day repair phase is substantially complete. The project currently
compiles cleanly (19 Java files) and produces `dist\EAuction.war`. The previously
documented repairs (authentication guards, idempotency, Oracle time consistency,
settlement/payment/transaction/certificate orchestration, UNSOLD fix, JSON/WebSocket
lifecycle, image/runtime mapping) are present in source and verified by code markers
(see §4). Build is green. Full end-to-end runtime proof is still desirable but is
not yet a pristine demonstration of the newest flow (see §7).

The DBUtil "Ponytail" refactor was attempted and **fully rolled back** (see §10).
It is NOT an active change.

---

## 3. Architecture

- **Servlets (`src\java\eauction`)** — one servlet per operation, each performs its
  own `DriverManager.getConnection(...)` with hardcoded Oracle credentials.
  - `RegisterServlet` (`/register`)
  - `LoginServlet` (`/login`), `LogoutServlet` (`/logout`)
  - `ProductSubmitServlet` (`/product`)
  - `AdminProductServlet` (`/admin-products`), `ApproveProductServlet` (`/approve-product`), `RejectProductServlet` (`/reject-product`)
  - `ScheduleAuctionServlet` (`/schedule-auction`)
  - `PlaceBidServlet` (`/placeBid`)
  - `SettlementServlet` (`/settlement`)
  - `PaymentServlet` (`/payment`)
  - `TransactionServlet` (`/transaction`)
  - `CertificateServlet` (`/certificate`)
  - `AuctionDetailsServlet` (`/auction-details`)
  - `AuctionListServlet` (`/auction-list`)
  - `UserSessionServlet` (`/user-session`)
  - `AuctionScheduler` + `AuctionSchedulerListener` (timer-driven lifecycle job)
  - `AuctionWebSocket` (lifecycle + countdown broadcast)
- **Web root (`web`)** — `index.html`, `auction.html`, `login.html`, `register.html`,
  `product.html`, `schedule-auction.html`, `admin-products.html`, `logout.html`.
  Declarative config: `web\WEB-INF\web.xml` (servlet mappings) and
  `web\META-INF\context.xml` (external uploads resource).
- **Data flow** — browser → servlet (JDBC to Oracle XE) → JSON/HTML response;
  auctions broadcast via `AuctionWebSocket`.
- **Uploads** — external directory `D:\E-Auction\uploads` mounted at
  `webAppMount="/uploads"` (see §6). Served at `/EAuction/uploads/<filename>`.
- **Database** — Oracle XE on the same server PC, `localhost:1521:xe`, accessed
  server-side only. No `.sql` schema files are present in the project tree.

No central data-access layer exists (the abandoned DBUtil refactor intended one).
Connection logic is duplicated per servlet — this is a known code smell, not a bug
to fix without approval (see §8, §12).

---

## 4. Completed repairs

All items below were verified by direct inspection of source markers on the current
working tree. Markers found: `FOR UPDATE` (Settlement/Payment/Transaction/Certificate/
PlaceBid/AuctionScheduler), `SYSTIMESTAMP` (PlaceBid/AuctionScheduler),
`idempotency` (PlaceBid), `UNSOLD` (AuctionScheduler), `toInstant` (AuctionDetails/
AuctionList), `SETTLED` + `rollback` (CertificateServlet), `session==null` /
`getAttribute("user")` auth guards (Settlement/Payment/Transaction/Certificate/PlaceBid).

1. **SettlementServlet** — auth/session guard added; anonymous settlement creation
   blocked; duplicate settlement check hardened with `FOR UPDATE`.
2. **PaymentServlet** — auth/session guard added; existing PENDING → PAID behavior
   preserved.
3. **TransactionServlet** — auth/session guard added; requires a PAID settlement
   before transaction creation; duplicate transaction check hardened with
   `FOR UPDATE`; existing fee calculation preserved (`Math.floor`).
4. **CertificateServlet** — auth/session guard added; duplicate certificate check
   hardened with `FOR UPDATE`; certificate INSERT + auction `SETTLED` update occur in
   the same transaction; a zero-row auction SETTLED update triggers rollback
   (`getUpdateCount` check → `con.rollback()`).
5. **PlaceBidServlet** — client/server idempotency support added; `idempotency_key`
   required; duplicate bid detection added; `idempotency_key` stored in BIDS;
   Oracle `SYSTIMESTAMP` added as `db_now` for bid timing (JVM-vs-Oracle mismatch
   removed); WebSocket `end_time` timestamp formatting normalized.
6. **AuctionScheduler** — no-winner expired auctions become `UNSOLD` (not `SOLD`);
   winner-bearing expired auctions become `SOLD`; lifecycle WebSocket events added;
   lifecycle events broadcast only after successful DB commit.
7. **AuctionDetailsServlet / AuctionListServlet** — timestamp output normalized to
   ISO-8601 via `Timestamp.toInstant().toString()`.
8. **auction.html** — client idempotency state added; lifecycle WebSocket handling
   added; countdown interval handling corrected; image URL made context-aware;
   legacy `image/` references supported.
9. **index.html** — image URL made context-aware; legacy `image/` references
   supported.
10. **context.xml** — external upload directory mapped:
    `D:\E-Auction\uploads` → `/EAuction/uploads` (via `DirResourceSet` PreResources).
11. **Runtime Tomcat discovery** — Tomcat 10.1.57, port 8081, runtime context
    `/EAuction`, NetBeans directory deployment; authoritative runtime descriptor at
    `C:\Program Files\Apache Software Foundation\Tomcat 10.1\conf\Catalina\localhost\EAuction.xml`;
    runtime resource mapping verified through the deployed app.
12. **Image serving** — direct URL `/EAuction/uploads/<filename>` verified working;
    older DB references use `image/<filename>`; newer records use `/uploads/<filename>`;
    frontend helpers support both. Traced DB → filesystem → Tomcat → frontend.
13. **LAN readiness** — frontend does not hardcode localhost; WebSocket host derived
    dynamically; Oracle localhost references are server-side only; another laptop
    connects via the server PC's LAN IP + port 8081; Windows firewall / network
    isolation may still need configuration for LAN testing.
14. **Build verification** — multiple NetBeans Clean & Build operations succeeded;
    latest successful build compiled 19 Java source files and produced
    `dist\EAuction.war` (verified present, ~1.7 MB).
15. **Ponytail/refactoring incident** — see §10. All DBUtil changes rolled back;
    `DBUtil.java` no longer exists; project rebuilt successfully. NOT an active change.

---

## 5. Verification / build history

- Build system: NetBeans Ant (`Clean & Build`). Output: `dist\EAuction.war`.
- Latest verified artifact: `D:\E-Auction\EAuction\dist\EAuction.war` (present).
- Source count: 19 `.java` files under `src\java\eauction` (verified).
- No automated test suite is present (`test/` exists but contains no tests).
- Runtime verification was done via the deployed app on Tomcat 10.1.57, context
  `/EAuction`, port 8081 (see §6).

---

## 6. Deployment / runtime facts

| Fact | Value |
|------|-------|
| Project root | `D:\E-Auction\EAuction` |
| Uploads directory | `D:\E-Auction\uploads` (exists, mounted at `/EAuction/uploads`) |
| Tomcat home | `C:\Program Files\Apache Software Foundation\Tomcat 10.1` |
| Tomcat version | 10.1.57 |
| Server port | `8081` |
| Runtime context | `/EAuction` |
| Runtime descriptor | `C:\Program Files\Apache Software Foundation\Tomcat 10.1\conf\Catalina\localhost\EAuction.xml` |
| Deployment mode | NetBeans directory deployment (not exploded-WAR copy) |
| Oracle | Oracle XE on same server PC; JDBC `localhost:1521:xe`, server-side only |
| Image URL (direct) | `http://<host>:8081/EAuction/uploads/<filename>` |
| App base URL | `http://<host>:8081/EAuction/` |

`web\META-INF\context.xml` content (verified):
```xml
<Context path="/EAuction">
    <Resources>
        <PreResources
            className="org.apache.catalina.webresources.DirResourceSet"
            base="D:\E-Auction\uploads"
            webAppMount="/uploads" />
    </Resources>
</Context>
```

---

## 7. Database state (last verified snapshot)

Oracle XE, same server PC. Last known/recorded snapshot:

| Table | ID | Linked | Status |
|-------|----|--------|--------|
| AUCTIONS | 1 | — | SOLD |
| AUCTIONS | 21 | — | SOLD |
| AUCTIONS | 41 | — | SCHEDULED |
| AUCTIONS | 42 | — | LIVE |
| SETTLEMENTS | 1 | auction 1 | PAID |
| TRANSACTIONS | 1 | auction 1 | COMPLETED |
| CERTIFICATES | 1 | transaction 1 | ISSUED |

Note: auction 1 is an existing completed post-auction example (settlement →
transaction → certificate), but it **predates some of the newer fixes** and is
therefore NOT a pristine end-to-end proof of the newly repaired flow. A fresh
LIVE→SOLD→settlement→payment→transaction→certificate run should be performed to
validate the current code end-to-end (see §11).

No `.sql` schema files are committed in the project; schema must be verified
directly against the running Oracle XE if needed.

---

## 8. Known remaining issues (lower priority — do NOT auto-fix)

These are documented code smells / optional cleanups. This is a college project;
goal is stability and correctness, not wholesale modernization. Do not treat these
as required fixes unless explicitly approved as a small, isolated task.

- **Repeated `DriverManager` / hardcoded Oracle credentials** across all 19 servlets
  (each servlet contains its own JDBC connect block). Verified by marker search.
- **Duplicate server-side `showMessage` helper** — copied into multiple servlets
  (`SettlementServlet`, `PaymentServlet`, `TransactionServlet`, `CertificateServlet`,
  `ScheduleAuctionServlet`, `ApproveProductServlet`, `RejectProductServlet`,
  `PlaceBidServlet`).
- **Duplicate frontend helper logic** — repeated JS helpers across `index.html`,
  `auction.html`, etc.
- **Platform fee uses `Math.floor()`** — in `TransactionServlet` (existing behavior,
  preserved).
- **`admin-products.html` relative image path** — line 34 uses `uploads/...` (a
  relative path). Verified present. May need context-aware prefix like the other
  pages, but only if it misbehaves.
- **More comprehensive end-to-end testing** remains desirable.

No new bugs were invented here. Do not assume every code smell is a defect.

---

## 9. Intentionally deferred items

- **WebSocket auction filtering** — intentionally deferred (not scoped for this phase).
- **Wholesale modernization** (Spring/Spring Boot migration, ORM, schema changes,
  central DB layer) — explicitly out of scope (see §12).
- **LAN firewall / network-isolation configuration** — may be needed for LAN testing;
  environment-dependent, not a code change.

---

## 10. Ponytail incident

A broad DBUtil refactor was attempted across approximately 15 servlets. The AI
provider **terminated during the operation**. The Review panel showed **17 recorded
changes**. ALL changes from that Ponytail task were **manually rolled back**.
`DBUtil.java` **no longer exists** (verified: file absent). The project was then
rebuilt successfully.

**IMPORTANT:** The DBUtil refactor MUST NOT be treated as an active project change.
If any future agent sees references to `DBUtil`, those are stale/abandoned and must
not be reintroduced without explicit approval. Each servlet uses its own
`DriverManager` connection (verified).

---

## 11. Current next step

**Preferred next phase (in order):**
1. Final regression pass against the documented repairs (§4).
2. Limited, safe cleanup only where justified (§8) — never broad.
3. Final NetBeans Clean & Build.
4. Final end-to-end test: create LIVE auction → bid → expire → SOLD → settlement →
   payment (PENDING→PAID) → transaction → certificate, validating the current code
   path (not relying solely on legacy auction 1).
5. Then, only if explicitly approved, optional optimization in small checkpoints.

Any future optimization must be MUCH smaller and safer than the Ponytail attempt.
Do not refactor many critical files in one operation.

---

## 12. PROJECT SAFETY RULE

- Do **NOT** automatically restructure the project.
- Do **NOT** rewrite working servlets.
- Do **NOT** migrate to Spring / Spring Boot.
- Do **NOT** replace JDBC with an ORM.
- Do **NOT** change the database schema without explicit approval.
- Do **NOT** refactor many critical files in one operation.
- Every change must be a **small checkpoint**.
- **Inspect first.**
- **Propose change.**
- **Approve change.**
- **Edit only the approved scope.**
- **Build.**
- **Test.**
- **Checkpoint.**
- **Preserve all previously working behavior.**

> If an AI agent begins broad autonomous refactoring, **STOP it.**

---

## 13. HOW FUTURE AI AGENTS MUST WORK

1. Start from `HANDOFF.md`.
2. Do not repeat broad discovery unless necessary.
3. Do not assume old findings are still true; verify only the relevant scope.
4. Never modify code before a design checkpoint.
5. Prefer a fresh chat when context becomes large.
6. Keep each task small.
7. Stop after each edit.
8. Build after controlled code changes.
9. Never perform global project reverts.
10. Never assume a terminated agent completed its last operation.
11. Check the Review panel after interrupted mutations.
12. Preserve previous repairs.

---

## 14. Checkpoint history

| # | Checkpoint | Status | What changed | Verification |
|---|-----------|--------|--------------|--------------|
| 2 | Bid idempotency | Done | `PlaceBidServlet` client/server idempotency, `idempotency_key` required + stored, duplicate detection | Source markers present (`idempotency`, `FOR UPDATE`) |
| 3 | UNSOLD auction fix | Done | `AuctionScheduler` no-winner → `UNSOLD`; winner → `SOLD` | Source markers present (`UNSOLD`, `SOLD`) |
| 4 | Oracle time consistency | Done | `SYSTIMESTAMP` as `db_now` in `PlaceBidServlet`; removed JVM-Oracle mismatch; normalized WebSocket `end_time` | Source markers present (`SYSTIMESTAMP`) |
| 5 | Settlement/Payment/Transaction/Certificate orchestration | Done | Auth guards; FOR UPDATE dup checks; PENDING→PAID preserved; PAID-before-transaction; cert+SETTLED same txn + rollback | Source markers present (`FOR UPDATE`, `SETTLED`+`rollback`, auth guards) |
| 6 | JSON/WebSocket lifecycle | Done | Lifecycle events in `AuctionScheduler`/`AuctionWebSocket`; broadcast only after commit; normalized JSON | Source markers present (`LIFECYCLE`, `broadcast`) |
| 7 | Image/upload/runtime deployment | Done | `context.xml` upload mapping; `/EAuction/uploads` verified; frontend context-aware image URLs | `context.xml` verified; markers in `index.html`/`auction.html` |
| 8 | Duplicate post-auction protection | Done | `FOR UPDATE` on settlement/transaction/certificate dup checks | Source markers present |
| 9 | Regression audit | Done | Auth guards + behavior preservation audited across servlets | Markers confirmed across all critical servlets |
| 9B | Ponytail attempt and rollback | Rolled back | DBUtil refactor across ~15 servlets; terminated; 17 changes; ALL reverted; `DBUtil.java` removed; rebuilt | `DBUtil.java` absent; 19 servlets build clean |
| current | Final validation | In progress | Verify all repairs, build green, prepare end-to-end test | `dist\EAuction.war` present; 19 `.java` compile; source markers confirmed |

---

*End of handoff. This document is the authoritative state record. Do not modify any
source, config, or database files when updating it.*
