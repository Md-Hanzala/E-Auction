# E-Auction — Business Rules

## Roles and visibility

1. A normal user may both buy and sell using one account.
2. An admin does not manually conduct bids or choose winners.
3. The application is the auctioneer: it controls the timer, validates bids, updates current price, and ends auctions.
4. A submitted product is private until an admin approves it.

## Product lifecycle

```text
PENDING -> APPROVED -> SCHEDULED -> LIVE -> ENDED -> SOLD or UNSOLD -> SETTLED
                 \
                  -> REJECTED
```

Only an approved product may become an auction. A rejected product must never appear in the public marketplace.

## Bidding

1. A bid is valid only when the auction status is `LIVE` and server time is before its end time.
2. A bid must be strictly greater than the current valid bid.
3. Typed bids and quick bids follow the same validation path.
4. The database is authoritative. The browser cannot accept its own bid.
5. Simultaneous bids must be processed inside one short Oracle transaction that locks the auction row, checks the latest price, inserts the bid, and updates the auction atomically.
6. A duplicate retry must not create a second bid; this will be handled with an idempotency key in the advanced bidding phase.

## Time and anti-sniping

1. Start and end times are stored by the server/database.
2. Clients display the remaining time but do not decide the end time.
3. A valid bid in the final configurable window extends the end time by a configurable amount.
4. Total extension may be capped so an auction cannot continue forever.

## Scheduling

1. Admin sees existing scheduled/live auctions and available time gaps.
2. Queue mode places an auction in the next suitable free gap.
3. Parallel mode permits overlap only when the configured capacity allows it.
4. Scheduling data must survive a server restart because it is stored in Oracle.

## Settlement and privacy

1. After a sold auction, platform fee is `floor(final_price × 0.05)`.
2. Seller amount is `final_price - platform_fee`.
3. Fee and seller-net values are private; they are not displayed on public auction pages.
4. Public pages show auction result, final bid, and a masked winner identity only.
