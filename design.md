# E-Auction — Initial Design

## Pages

| Page | Purpose |
| --- | --- |
| `index.html` | Home page and project entry point. |
| `register.html` | New-user registration form. |
| `login.html` | Login form. |
| `seller_product.html` | Seller product-submission form. |
| `admin_dashboard.jsp` | Product approval and auction scheduling. |
| `auctions.jsp` | Upcoming and live auction list. |
| `auction_details.jsp` | Product, current bid, timer, bid form, and quick-bid buttons. |
| `my_transactions.jsp` | User transaction history. |

## First Servlet mappings

| Servlet | URL | Job |
| --- | --- | --- |
| `RegisterServlet` | `/register` | Receives registration data. |
| `LoginServlet` | `/login` | Validates login data. |
| `SubmitProductServlet` | `/submitProduct` | Receives seller product data. |
| `ApproveProductServlet` | `/approveProduct` | Admin approves a product. |
| `ScheduleAuctionServlet` | `/scheduleAuction` | Admin schedules an approved product. |
| `PlaceBidServlet` | `/placeBid` | Validates and records a bid. |

## Initial database design

```text
EAUCTION_USERS
  user_id, name, email, password, contact, address, created_at

PRODUCTS
  product_id, seller_id, name, description, start_price, floor_price, status

AUCTIONS
  auction_id, product_id, start_time, end_time, current_price, winner_id, status

BIDS
  bid_id, auction_id, bidder_id, amount, bid_time, status

TRANSACTIONS
  transaction_id, auction_id, seller_id, buyer_id, final_price, platform_fee, seller_amount

CERTIFICATES
  certificate_id, transaction_id, issued_at

AUDIT_LOG
  audit_id, user_id, action, details, created_at
```

## Registration flow already started

```text
register.html
      |
      v
/register
      |
      v
RegisterServlet
      |
      v
EAUCTION_USERS in Oracle
```

The form and Servlet connection works. The next implementation step is to create the Oracle table and update `RegisterServlet` to insert a record with JDBC.
