# E-Auction — Complete AI Handoff

> **Purpose:** Give this file to a new AI assistant or teammate so they can understand the E-Auction project, what has been decided, what has already been built, and what should happen next.
>
> **Security note:** This file intentionally does **not** contain database passwords, API keys, tokens, or other secrets. Keep credentials only in the developer's local configuration. Do not add them to this file, Git, screenshots, reports, or messages.
>
> **Current project root:** `D:\E-Auction\EAuction`
>
> **Last updated:** 2026-08-26

---

## 1. What the project is

**E-Auction** is a college web application that simulates an online auction platform.

It must first be built in a simple, teacher-friendly style so the student can explain every layer and receive marks. After the assessment, it can be modernised and scaled without changing the core business rules.

The project is not merely a product-listing website. The application itself is the **auctioneer**:

- It controls the auction lifecycle and authoritative timer.
- It validates bids.
- It handles simultaneous bids safely.
- It announces the winner.
- It calculates simulated settlement and a 5% platform fee.
- It records certificates and audit data.

The Admin is the platform owner/governance role; the Admin does **not** manually select winners or operate every bid.

---

## 2. Teaching style and communication preferences

The student is learning Java web development through NetBeans practicals. Follow this style during the first version:

1. Work **one small step at a time**. Do not flood the student with an entire application at once.
2. Explain in the **Richard Feynman method**: what happens, why it matters, simple analogy if useful, then exact practical action.
3. Prefer full-file replacements when editing a file, rather than tiny fragments.
4. Follow the teacher's JSP/Servlet/JDBC/Oracle approach before introducing newer frameworks.
5. Use `web.xml` mappings when the teacher expects that style.
6. Stop after a meaningful step and ask for a screenshot, output, or `done` before proceeding.
7. Keep the final presentation in mind. Every advanced feature should solve a real problem and be easy to demonstrate.

### First-submission stack

```text
NetBeans 21
Java 21
Apache Tomcat 10.1 / Jakarta EE 10
HTML + CSS + JavaScript
JSP + Servlets
Oracle XE
JDBC
web.xml servlet mappings
```

### Later, not first

```text
Modern JDBC driver / database upgrade
Password hashing and externalised credentials
REST API and a modern frontend
Redis
RabbitMQ
Docker
Cloud deployment
Automated tests, monitoring, load testing
Optional AI recommendations
```

The first version should not be delayed by cloud, Docker, Redis, RabbitMQ, or AI.

---

## 3. Project context gathered from earlier work

Before E-Auction, the student completed/attempted these classroom exercises:

### Oracle and JDBC practicals

- Oracle XE and SQL*Plus are installed and were used successfully.
- A simple Java project (`JdbcDemo`) inserted records into table `t75` and fetched records by ID.
- An old Oracle JDBC JAR, `ojdbc14.jar`, was added manually to NetBeans projects.
- The old classroom connection pattern used the Oracle Thin JDBC URL on local XE.

### Servlet and JSP practicals

- NetBeans web applications and Tomcat were used successfully.
- The student learned that Tomcat 10 uses `jakarta.servlet.*`, not `javax.servlet.*`.
- The old `LoginProject` used HTML forms, Servlet classes, and `web.xml` mappings.
- A JSP exercise inserted employee/user records into Oracle.
- A JavaBean exercise used the pattern:

```text
HTML form
    -> Servlet
    -> JavaBean (getters/setters + InsertMethod)
    -> Oracle
```

### Existing learning projects visible in NetBeans

```text
Cookie
JdbcDemo
LoginProject
ServletExample1
signup_jsp
ValidationForm
EAuction               <- current project
```

These are practice projects. Do **not** extend them for E-Auction; E-Auction is a clean new project.

---

## 4. Core product vision and roles

### Participants

```text
ADMIN                         USER
Platform owner                Customer account
Approves / rejects            Can buy and can sell
Schedules auctions            Bids or submits a product
Sees private settlement       Does not manually control the auction

APPLICATION / AUCTION ENGINE
Automatic digital auctioneer
Timer, validation, bids, winner, settlement, certificate
```

### Important role decision

A normal person must not need separate buyer and seller accounts. One user account may:

```text
buy an item today
sell an item tomorrow
```

Admin is the distinct privileged role. In a future robust data model, roles should support a user having both buyer and seller capabilities.

### Public versus private information

Public auction pages may show:

- Product details
- Starting price
- Current/highest bid
- Floor/reserve price if the design chooses to expose it
- Remaining time
- Auction status
- Final price and a masked/display winner identity after closure

They must not show:

- Seller's net amount
- Platform fee
- Private contact information
- Database credentials
- Full private payment data

---

## 5. Required business flow

```text
User registers / logs in
        |
        +--> Buyer: browses live/upcoming auctions and bids
        |
        +--> Seller: submits a product
                      |
                      v
                 Product = PENDING
                      |
                      v
              Admin approves or rejects
                 |              |
                 |              +--> REJECTED: never public
                 v
          Product = APPROVED
                 |
                 v
       Admin schedules an auction
                 |
                 v
        Auction = SCHEDULED
                 |
                 v
        Server starts it automatically
                 |
                 v
           Auction = LIVE
                 |
                 v
      Buyers submit valid bids
                 |
                 v
        Server validates + records bids
                 |
                 v
       Timer expires without extension
                 |
                 v
      SOLD or UNSOLD, then settlement
                 |
                 v
  Transaction, certificate, and audit records
```

### Lifecycle names

Use a clear state machine, not unrelated boolean flags:

```text
Product: PENDING -> APPROVED or REJECTED
Auction: SCHEDULED -> LIVE -> ENDED -> SOLD or UNSOLD -> SETTLED
Bid: submitted -> ACCEPTED or REJECTED
```

The exact final enum/table names may differ, but every state transition must be visible and testable.

---

## 6. Auction rules that are already decided

### Bidding

1. Only a live auction can accept bids.
2. The bid amount must be strictly greater than the current valid bid.
3. Quick-bid buttons and manually typed bids use exactly the same server validation path.
4. The browser is never allowed to decide a bid's validity.
5. The server/database is the source of truth.

### Simultaneous bids / concurrency

This is one of the major presentation features.

```text
Buyer 1 and Buyer 2 submit a bid at almost the same time
                    |
                    v
           One Oracle transaction locks the auction row
                    |
                    v
    Read the newest current bid, validate, update auction, insert bid
                    |
                    v
                 Commit or reject
```

The intended Oracle pattern is:

```sql
SELECT current_price, end_time
FROM auctions
WHERE auction_id = ?
FOR UPDATE;
```

Then, inside the same JDBC transaction:

```text
validate auction is LIVE and time has not ended
validate new bid > current price
update auction current price / provisional winner / end time
insert bid history row
commit
```

If two equal bids arrive together, the first committed bid is accepted; the second becomes invalid because it is no longer greater than the current price.

### WebSocket distinction

WebSocket does **not** solve concurrent bid consistency.

```text
Oracle transaction + row lock = decides valid authoritative state
WebSocket                      = tells every viewer about that state immediately
```

The correct future live-bid route is:

```text
Browser -> HTTP POST /placeBid -> Bid Servlet -> Oracle transaction
                                            -> accepted update
                                            -> WebSocket broadcast
```

Do not make WebSocket the authority for accepting bids.

### Timer and anti-sniping

- The backend stores `start_time` and `end_time`.
- Browsers display a countdown derived from server state.
- When a valid bid arrives in a final configurable window, the backend extends `end_time`.
- The extension happens in the same bid transaction.
- WebSocket later informs every client of the new end time.
- A maximum total extension should prevent an auction from continuing indefinitely.

Examples mentioned in planning documents differ (3 seconds, 10 seconds, 3 minutes). The implementation must make these settings configurable, not hard-code an example.

### Quick bids

The page will offer percentage-based quick-bid buttons calculated from the latest price. Examples mentioned in planning include 2/4/10/15% and 2/5/10/20%. The definitive percentages will be configuration values.

### Scheduling

The Admin sees existing auctions and free time windows.

```text
Quick scheduling: choose an available predefined slot
Custom scheduling: choose exact start time and duration
```

Modes:

```text
QUEUE    -> place into the next appropriate free gap
PARALLEL -> allow overlap only within configured capacity
```

There is a conflicting sentence in one version of the Executive Summary about long durations. The final project decision is:

```text
duration <= 2 hours: Admin may choose Queue or Parallel
duration > 2 hours:  Parallel only, if capacity allows
```

Keep `MAX_QUEUED_DURATION = 2 hours` configurable rather than scattering the value through code.

### Settlement

After a sold auction:

```text
platform_fee  = floor(final_price * 0.05)
seller_amount = final_price - platform_fee
```

This is a simulation/internal accounting record for the college project. It is not a real payment gateway or bank transfer.

---

## 7. Security requirements and teacher-version boundary

### Required direction

- Validate data on the server.
- Use `PreparedStatement` parameter binding for application SQL.
- Use database foreign keys, constraints, and indexes.
- Do not trust role information supplied by the browser.
- Do not expose the seller amount or platform fee publicly.
- Record audit events for login, product submission, approval/rejection, bids, auction closure, and settlement.

### Classroom implementation boundary

The student needs to follow the teacher's simple JSP/Servlet/JDBC style. Some older practice examples use direct credentials in Java and plaintext password storage. Those may help explain the coursework, but they must not be treated as production quality.

For the first version:

- Keep real database credentials out of documentation and chat transcripts.
- Do not use a real personal password in registration tests.
- Use `PreparedStatement` even though some old examples used string concatenation.
- The submitted version may start with a simple login/session flow if that is what the teacher can inspect.

After assessment:

- Move DB credentials to configuration/environment variables.
- Use a dedicated least-privilege application schema account rather than an administrative account.
- Hash user passwords with Argon2id or bcrypt.
- Add a robust authentication/authorization strategy such as JWT plus RBAC if required by the final scope.

---

## 8. Architecture

### Teacher-version architecture

```text
Browser pages (HTML / JSP)
            |
            v
Servlets (web.xml mappings)
            |
            v
JavaBeans / Service classes
            |
            v
JDBC DAO classes
            |
            v
Oracle Database
```

### Later real-time extension

```text
HTTP Servlet bid request -> Oracle transaction -> state is committed
                                               -> WebSocket sends new state
```

### Local multi-laptop demonstration

```text
Admin browser      Buyer browser(s)      Public display browser
      \                   |                    /
       \------------------|-------------------/
                          |
                      Same LAN
                          |
                          v
       Server laptop: Tomcat + Oracle + E-Auction project
```

The server laptop's LAN IP, rather than `localhost`, is used by other laptops. The demo does not require public internet deployment. Test early because some college Wi-Fi networks isolate clients; a trusted router/hotspot is a suitable fallback.

---

## 9. Database design

### Table plan

```text
EAUCTION_USERS
  user_id, name, email, user_password/password_hash, contact, address, created_at

PRODUCTS
  product_id, seller_id, name, description, image reference,
  start_price, floor_price, status, created_at

AUCTIONS
  auction_id, product_id, start_time, end_time, current_price,
  winner_id, status, scheduling_mode, total_extension_seconds

BIDS
  bid_id, auction_id, bidder_id, amount, bid_time, status,
  idempotency_key (advanced phase)

TRANSACTIONS
  transaction_id, auction_id, seller_id, buyer_id,
  final_price, platform_fee, seller_amount, status, created_at

CERTIFICATES
  certificate_id, transaction_id, issued_at, details/file reference

AUDIT_LOG
  audit_id, user_id, action, details, created_at
```

### Current real Oracle objects — created and verified

The following were created successfully in SQL*Plus:

```sql
CREATE TABLE eauction_users (
    user_id NUMBER PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    email VARCHAR2(100) UNIQUE NOT NULL,
    user_password VARCHAR2(100) NOT NULL,
    contact VARCHAR2(20),
    address VARCHAR2(200),
    created_at DATE DEFAULT SYSDATE
);

CREATE SEQUENCE eauction_user_seq
START WITH 1
INCREMENT BY 1;
```

The student ran `DESC eauction_users;` and confirmed all expected columns. The student also ran a sequence query and confirmed `EAUCTION_USER_SEQ` exists.

---

## 10. Documents already created in the project root

These are already present and should be kept updated alongside code:

| File | Purpose |
| --- | --- |
| `project-requirements.md` | Scope, technology choice, required features, demonstration target, and exclusions. |
| `architecture.md` | Teacher-version layers, major modules, WebSocket role, and LAN architecture. |
| `rules.md` | Product lifecycle, bidding, scheduling, timer, settlement, and privacy rules. |
| `phases.md` | Planned development order from setup through post-assessment upgrades. |
| `design.md` | Planned pages, Servlet URLs, initial table design, and registration-flow status. |
| `AI-HANDOFF.md` | This complete transfer document. |

If requirements change, update the relevant document as well as the code.

---

## 11. Current NetBeans project state

### Project created

The project was created correctly in NetBeans as:

```text
Project name: EAuction
Project location: D:\E-Auction
Project folder: D:\E-Auction\EAuction
Server: Apache Tomcat or TomEE
Java EE version: Jakarta EE 10 Web
Context path: /EAuction
Frameworks selected: none
Java platform: JDK 21 (Default)
```

The project contains the standard folders:

```text
Web Pages
Source Packages
Libraries
Configuration Files
```

### Tomcat project test — complete

- NetBeans initially showed its default `TODO write content` page.
- The browser page was refreshed manually via `http://localhost:8081/EAuction/index.html`.
- This proved that the new project deployed and Tomcat was working.
- NetBeans was instructed to set its Run `Relative URL` to `index.html`; confirmation of that preference being saved was not captured, so verify it if Run Project does not open the home page automatically.

### `web/index.html` — complete and tested

The starter page was replaced with a basic E-Auction home page. It has been run and displayed successfully.

Expected content:

```html
<!DOCTYPE html>
<html>
<head>
    <title>E-Auction</title>
</head>

<body>

    <h1>E-Auction System</h1>

    <p>Welcome to the Online E-Auction Platform.</p>

    <h2>Available Options</h2>

    <ul>
        <li>User Registration</li>
        <li>User Login</li>
        <li>Seller Product Submission</li>
        <li>Admin Product Approval</li>
        <li>Live Auction Bidding</li>
    </ul>

</body>
</html>
```

### `web/register.html` — complete and tested

The registration page was created and contains a teacher-style HTML form.

Expected content:

```html
<!DOCTYPE html>
<html>
<head>
    <title>User Registration - E-Auction</title>
</head>

<body>

    <h1>E-Auction User Registration</h1>

    <form method="post" action="register">

        <table>
            <tr>
                <td>Name</td>
                <td><input type="text" name="name"></td>
            </tr>
            <tr>
                <td>Email</td>
                <td><input type="text" name="email"></td>
            </tr>
            <tr>
                <td>Password</td>
                <td><input type="password" name="password"></td>
            </tr>
            <tr>
                <td>Contact</td>
                <td><input type="text" name="contact"></td>
            </tr>
            <tr>
                <td>Address</td>
                <td><textarea name="address"></textarea></td>
            </tr>
            <tr>
                <td><input type="submit" value="REGISTER"></td>
                <td><input type="reset" value="RESET"></td>
            </tr>
        </table>

    </form>

</body>
</html>
```

### Registration Servlet — created and request path tested

NetBeans Servlet wizard was used with:

```text
Class name: eauction.RegisterServlet
Servlet name: RegisterServlet
URL pattern: /register
Add information to deployment descriptor (web.xml): checked
```

This matches `action="register"` in `register.html`.

The form-to-Servlet connection was successfully tested. The browser showed:

```text
Registration Details Received
Name: ...
Email: ...
Contact: ...
Address: ...
Database connection will be added next.
```

That proves:

```text
register.html -> /register -> RegisterServlet
```

### JDBC driver — added to EAuction

The project Properties → Libraries page shows the legacy local Oracle driver JAR under compile-time libraries:

```text
...\jdbc\lib\ojdbc14.jar
```

It was added successfully to this E-Auction project.

### Current registration persistence status — DO NOT ASSUME COMPLETE

A full JDBC version of `RegisterServlet` was provided to replace the temporary display-only version. It uses:

- local Oracle Thin JDBC connection configuration,
- `PreparedStatement`,
- `eauction_user_seq.NEXTVAL`, and
- an insert into `eauction_users`.

The student later disclosed local database credentials for convenience. Do not reproduce them in this document or messages. They must be inserted locally only.

**The important status is:** the table, sequence, JDBC driver, form, and servlet all exist, but a successful post-registration `SELECT * FROM eauction_users;` test has not yet been recorded. The next AI should verify this rather than claim it is done.

The desired safe teacher-version code shape is:

```java
String query = "INSERT INTO eauction_users "
        + "(user_id, name, email, user_password, contact, address) "
        + "VALUES (eauction_user_seq.NEXTVAL, ?, ?, ?, ?, ?)";

PreparedStatement ps = con.prepareStatement(query);
ps.setString(1, name);
ps.setString(2, email);
ps.setString(3, password);
ps.setString(4, contact);
ps.setString(5, address);
ps.executeUpdate();
```

The database connection's password must remain a local secret placeholder, never a source-controlled value.

---

## 12. Exact next action

The next immediate action is **not** to start login, products, or auction pages yet.

First finish and prove registration persistence:

1. Ensure the JDBC version of `RegisterServlet.java` is saved with the local Oracle configuration completed only on the student's machine.
2. In NetBeans, right-click `EAuction` and choose **Clean and Build**.
3. Run the project.
4. Open:

   ```text
   http://localhost:8081/EAuction/register.html
   ```

5. Register a test user with a **new unique email address** and a dummy password.
6. Expect a `Registration Successful` page.
7. In SQL*Plus, execute:

   ```sql
   SELECT user_id, name, email, contact, address, created_at
   FROM eauction_users;
   ```

8. Confirm that the row exists and `user_id` came from `EAUCTION_USER_SEQ`.

If the insert fails, collect the exact browser/NetBeans output and diagnose in this order:

```text
1. JDBC driver available to EAuction?
2. Database service running?
3. JDBC URL uses correct host/port/SID/service?
4. Local username/password correct?
5. table and sequence visible to the connecting user?
6. duplicate email constraint triggered?
```

Do not delete/recreate tables unless the exact error requires it.

---

## 13. Planned build order after registration is proven

### Phase 1 — Authentication

1. Add login page and `LoginServlet`.
2. Query user by email/password for the classroom version.
3. Create a simple session and logout path.
4. Later add user capabilities/role tables and admin access.

### Phase 2 — Seller and Admin product workflow

1. Create `PRODUCTS` table.
2. Create seller product-submission form and Servlet.
3. Create admin pending-product page.
4. Create approve/reject Servlet and status updates.
5. Ensure public listing only exposes approved items.

### Phase 3 — Scheduling and auction state

1. Create `AUCTIONS` and schedule-related data.
2. Build admin scheduling interface.
3. Implement upcoming/live/ended list pages.
4. Use a simple Java scheduled check first; Quartz is optional later.

### Phase 4 — Core bidding

1. Create `BIDS` table.
2. Build auction-details page.
3. Create `PlaceBidServlet`.
4. Use one Oracle transaction with `SELECT ... FOR UPDATE`.
5. Add quick-bid calculations.
6. Add status transition and settlement records.

### Phase 5 — Showcase features

1. Add a Jakarta WebSocket endpoint and JavaScript client.
2. Broadcast accepted bids and end-time changes.
3. Add anti-sniping extension.
4. Add audit log page.
5. Test simultaneous bidding across browsers/LAN.

### Phase 6 — Presentation

Demonstrate this scripted flow:

```text
Seller registers/submits product
        -> Admin approves it
        -> Admin schedules it
        -> Two buyers open the live auction
        -> Buyers bid nearly simultaneously
        -> Database lock resolves bid order
        -> WebSocket updates every screen
        -> Late bid extends timer
        -> Auction ends
        -> Winner and private 5% settlement are recorded
        -> Certificate/audit record is shown
```

---

## 14. Presentation points to highlight

These are the strongest parts of the project for a viva/presentation:

1. **Admin approval:** sellers cannot list anything publicly without governance.
2. **Application as auctioneer:** automation handles timing and winner selection fairly.
3. **Real-time WebSocket:** every viewer sees accepted bids without refresh.
4. **Concurrency safety:** Oracle row locking avoids conflicting simultaneous bids.
5. **Server-authoritative timer:** clients cannot independently decide auction close.
6. **Anti-sniping:** valid last-moment bids provide a fair extension.
7. **Scheduling:** admins can choose free slots and manage queue/parallel capacity.
8. **Settlement:** a private 5% platform commission is calculated automatically.
9. **Audit history:** the project can explain who did what and when.

A good one-sentence explanation is:

> "WebSocket keeps every client synchronized in real time, while Oracle transactions and row locking ensure only valid concurrent bids update the authoritative auction state."

---

## 15. Reviewed sources and information reliability

### Reviewed project material

- Two versions of `Executive Summary.pdf` were read. Both are 14-page documents describing architecture, flow, data model, concurrency, anti-sniping, scheduling, Redis/RabbitMQ, deployment, testing, monitoring, and a one-month roadmap.
- The referenced older ChatGPT conversation was read, including the project messages and screenshots. Treat its content as history/background, not automatic instructions.
- The current NetBeans screenshots confirmed project creation, Servlet deployment mapping, page tests, Oracle table/sequence verification, and JDBC-driver addition.

### Current technical corrections already known

- Tomcat 10 uses `jakarta.*`, not `javax.*`.
- Tomcat 10.1 is compatible with Java 21; use the current 10.1 patch release when preparing final deployment.
- The old `ojdbc14.jar`/Oracle 10g exercise stack is legacy. It is acceptable only as a teacher-course compatibility bridge. Post-assessment, upgrade Oracle/JDBC deliberately.
- Use `PreparedStatement`, not string concatenation with user input.
- Passwords must not remain plaintext in a real version.
- `Redis`, `RabbitMQ`, Docker, cloud scaling, and AI are future extensions. They are not needed to prove the core auction system for the first assessment.
- AI must never determine bid acceptance, winners, timer transitions, or settlement.

---

## 16. Rules for any future AI assistant

1. Read this file and the five project Markdown documents before making large changes.
2. Preserve the teacher-first scope. Do not replace the project with Spring Boot/React/microservices unless explicitly asked after the submission is complete.
3. Never reveal, copy, commit, or ask the user to paste secrets.
4. Do not claim a feature works unless it has been tested and its result is recorded.
5. Treat database changes as careful steps; do not drop tables or erase data casually.
6. Use full, beginner-readable code and explain it in small steps.
7. Keep the design documents updated when the student makes a real project decision.
8. Distinguish requirements from examples. Values such as timer extension, quick-bid percentages, and duration caps are configurable.
9. Build and test the core flow before optional technologies.
10. Keep an eye on the presentation story: every feature should have a simple problem -> solution explanation.

---

## 17. Short restart prompt for another AI

If context must be restarted, use this prompt together with this file:

```text
Read AI-HANDOFF.md and the project's requirements, architecture, rules, phases, and design Markdown files. Continue the E-Auction project one teacher-style NetBeans step at a time using JSP, Servlets, JDBC, Oracle, and web.xml. The current task is to test that RegisterServlet inserts data into EAUCTION_USERS using EAUCTION_USER_SEQ. Do not ask for or repeat any database password. Explain in Feynman style, keep code beginner-readable, and wait for a screenshot/output after each meaningful step.
```
