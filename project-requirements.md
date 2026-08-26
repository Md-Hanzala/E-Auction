# E-Auction — Project Requirements

## Purpose

E-Auction is a college web application for running online auctions. Its first version will be built in the teaching style used in class so that every part can be demonstrated and explained during assessment. A later version may modernise the technology without changing the core business rules.

## First-submission technology stack

- Java 21
- Apache NetBeans
- Apache Tomcat / Jakarta EE 10
- HTML, CSS, JavaScript, JSP, and Servlets
- Oracle Database and JDBC
- `web.xml` servlet mappings where required by the teacher

## Users

- **User:** one account may buy items and submit items for sale.
- **Admin:** reviews products, manages the schedule, and sees private settlement records.
- **Auction engine:** the application itself; it validates bids, controls time, chooses winners, and records settlement.

## Required features

1. User registration and login.
2. Seller product submission with product details and an image reference.
3. Admin approval or rejection of submitted products.
4. Only approved products can be scheduled and shown to buyers.
5. Admin scheduling with free-slot suggestions and queue/parallel choices.
6. Upcoming, live, ended, sold, and unsold auction views.
7. Manual bids and percentage-based quick-bid buttons.
8. Server-side bid validation and Oracle transaction handling for simultaneous bids.
9. A server-authoritative countdown and configurable anti-sniping extension.
10. Winner selection, simulated 5% platform commission, seller settlement record, and certificate/receipt.
11. Audit records for important actions.

## Presentation target

The project must run on one laptop acting as the Tomcat/Oracle server, with several browsers or LAN-connected laptops acting as users and an admin. The demo must clearly show approval, scheduling, live bidding, timer extension, winner selection, and settlement.

## Explicitly out of scope for the first submission

- Real banking, payment-gateway processing, or real money transfer.
- Internet/cloud deployment.
- Redis, RabbitMQ, Docker, Kubernetes, and AI decision making.
- Production-scale clustering.

Those can be added after assessment. AI, if ever used, may provide recommendations only; it must never decide a valid bid, winner, or settlement.
