# E-Auction — Architecture

## Teacher-version architecture

```text
Browser pages (HTML / JSP)
            |
            v
Servlets (mapped through web.xml)
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

This is a simple layered design: pages collect and display data, Servlets receive requests, Java classes apply rules, JDBC sends SQL to Oracle, and Oracle keeps the permanent records.

## Main modules

| Module | Responsibility |
| --- | --- |
| Authentication | Registration, login, logout, and user identity. |
| Product | Seller submission, admin approval/rejection, and product visibility. |
| Scheduler | Stores start/end times, suggests slots, and starts/ends auctions automatically. |
| Auction engine | Validates bids, updates the current bid, applies late-bid extensions, and selects a winner. |
| Settlement | Records final price, 5% platform fee, seller amount, and a certificate. |
| Audit | Records key actions and outcomes. |

## Real-time extension

Normal operations use HTTP requests to Servlets. The live-auction module will add a Jakarta WebSocket endpoint later in the project.

```text
Buyer submits bid by HTTP
        |
        v
Servlet -> Service -> Oracle transaction
        |
        v
WebSocket broadcasts the accepted state to every viewer
```

WebSocket communicates updates; Oracle transactions decide which bid is valid. They solve different problems.

## LAN demonstration architecture

```text
Admin browser      Buyer browser(s)
      \                 /
       \               /
        ---- same LAN ----
                 |
                 v
      Server laptop: Tomcat + Oracle
```

All clients use the same application instance and database. The server is the authority for time, bids, and auction status.
