# E-Auction — Development Phases

## Phase 0 — Planning and project setup

- Create the NetBeans web project.
- Write the project requirements, architecture, rules, phases, and design documents.
- Confirm Tomcat can run `index.html`.

**Status:** in progress.

## Phase 1 — Basic teacher-style web flow

- Home page.
- Registration page and `RegisterServlet`.
- Oracle `EAUCTION_USERS` table and JDBC insertion.
- Login page and login validation.

## Phase 2 — Product and admin flow

- Seller product-submission form.
- Product table and product insertion.
- Admin login/dashboard.
- Approve/reject product flow.

## Phase 3 — Auction and scheduling flow

- Auction table and schedule form.
- Upcoming/live/ended auction pages.
- Queue/parallel scheduling rules.
- Background status checks for start and end times.

## Phase 4 — Bidding engine

- Bid table and place-bid Servlet.
- Current price update.
- Oracle transaction and row locking.
- Quick-bid percentages.
- Settlement and certificate records.

## Phase 5 — Real-time and presentation features

- WebSocket live updates.
- Server-authoritative timer and anti-sniping extension.
- Audit log.
- Multi-browser/LAN test and demonstration script.

## Phase 6 — After assessment

- Hash passwords and externalise configuration.
- Upgrade legacy database/driver choices.
- Add Redis, RabbitMQ, Docker, cloud deployment, tests, monitoring, and optional AI assistance.
