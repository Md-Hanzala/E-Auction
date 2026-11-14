<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="com.eauction.model.User" %>
<%@ page import="com.eauction.model.Product" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Operations Console | HASHbid Administration</title>
    <link rel="icon" type="image/svg+xml" href="assets/images/hashbid-logo.svg">
    <link rel="stylesheet" href="assets/css/style.css">
</head>
<body class="auth-page">

    <!-- Navigation Header -->
    <header>
        <nav class="navbar bs-navbar">
            <a href="index.html" class="logo bs-logo">
                <img src="assets/images/hashbid-logo.svg" alt="HASHbid Emblem" width="24" height="24" style="border-radius: 6px;">
                HASH<span>bid</span>
                <span class="logo-badge" style="background: var(--bs-color-danger-soft); color: var(--bs-color-danger); border-color: rgba(200, 121, 121, 0.4);">Control</span>
            </a>

            <div class="nav-links bs-nav-links">
                <a href="admin-bid-history" class="bs-button bs-button--secondary bs-button--sm">
                    📜 Bid Audit Trail
                </a>

                <div class="bs-user-chip">
                    <div style="width: 32px; height: 32px; border-radius: 50%; background: var(--bs-color-danger-soft); color: var(--bs-color-danger); display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 800;">
                        🛡️
                    </div>
                    <div class="bs-user-chip__info">
                        <div class="bs-user-chip__name">
                            <%= session.getAttribute("userName") != null ? session.getAttribute("userName") : "System Administrator" %>
                        </div>
                        <div class="bs-user-chip__meta">
                            <span class="bs-status bs-status--danger" style="padding: 1px 6px; font-size: 10px;">
                                ADMIN
                            </span>
                            <span>OPS ROOT</span>
                        </div>
                    </div>
                </div>

                <a href="logout" class="bs-button bs-button--danger bs-button--sm">
                    Sign Out
                </a>
            </div>
        </nav>
    </header>

    <main class="form-page-container bs-page-container" style="max-width: 1360px; width: 95%;">
        <!-- Operations Header -->
        <section class="form-intro bs-page-header" style="text-align: left; margin-bottom: 28px;">
            <span class="bs-eyebrow auth-tag" style="background: var(--bs-color-danger-soft); color: var(--bs-color-danger); border-color: rgba(200, 121, 121, 0.35);">
                OPERATIONS CONSOLE
            </span>
            <h1>Central Auction Supervision & Vetting</h1>
            <p>Supervise active bidding floors, execute manual lot settlements, schedule product auctions, and review merchant applications.</p>
        </section>

        <!-- Dynamic Feedback Banners -->
        <%
            String success = request.getParameter("success");
            String error = request.getParameter("error");
            if (success != null) {
        %>
            <div class="bs-alert bs-alert--success">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10"/><path d="m9 12 2 2 4-4"/>
                </svg>
                Action completed successfully: <%= success.replace('_', ' ') %>
            </div>
        <% } else if (error != null) { %>
            <div class="bs-alert bs-alert--error">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
                Action failed: <%= error.replace('_', ' ') %>
            </div>
        <% } %>

        <!-- SECTION 1: LIVE BID MONITOR & EARLY CLOSE -->
        <section class="registration-form-card bs-card" style="padding: 28px; margin-bottom: 32px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 12px;">
                <div>
                    <h2 style="color: var(--bs-color-text); font-size: 20px; font-weight: 800; margin: 0 0 4px 0;">
                        Live Bidding Floor Monitor
                    </h2>
                    <p style="color: var(--bs-color-muted); font-size: 13px; margin: 0;">
                        Live telemetry for all active auction lots with emergency manual termination controls.
                    </p>
                </div>
                <span class="bs-status bs-status--live">
                    Floor Supervisory Mode
                </span>
            </div>

            <%
                List<Map<String, Object>> activeAuctions = (List<Map<String, Object>>) request.getAttribute("activeAuctions");
                if (activeAuctions == null || activeAuctions.isEmpty()) {
            %>
                <div style="text-align: center; padding: 40px 0;">
                    <p style="color: var(--bs-color-muted); font-size: 14px;">
                        No active auctions currently running on the trading floor.
                    </p>
                </div>
            <% } else { %>
                <div class="bs-table-wrap">
                    <table class="bs-table">
                        <thead>
                            <tr>
                                <th>Lot ID</th>
                                <th>Asset Title</th>
                                <th>Description</th>
                                <th>Category</th>
                                <th>Starting Reserve</th>
                                <th>Current Top Bid</th>
                                <th>Leading Participant</th>
                                <th>Scheduled Closing</th>
                                <th style="text-align: right;">Supervisory Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (Map<String, Object> item : activeAuctions) {
                                String bidder = (String) item.get("highestBidderName");
                                String desc = (String) item.get("description");
                            %>
                                <tr>
                                    <td style="font-weight: 700; color: var(--bs-color-primary-light); font-family: ui-monospace, monospace;">
                                        #<%= item.get("auctionId") %>
                                    </td>
                                    <td style="font-weight: 700; color: var(--bs-color-text);"><%= item.get("productName") %></td>
                                    <td style="color: var(--bs-color-muted); max-width: 220px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" title="<%= desc != null ? desc : "" %>">
                                        <%= desc != null && !desc.trim().isEmpty() ? desc : "—" %>
                                    </td>
                                    <td style="color: var(--bs-color-muted);"><%= item.get("category") %></td>
                                    <td style="font-family: ui-monospace, monospace;">₹<%= item.get("startingPrice") %></td>
                                    <td style="font-weight: 800; color: var(--bs-color-primary-light); font-size: 15px; font-family: ui-monospace, monospace;">
                                        ₹<%= item.get("currentBid") %>
                                    </td>
                                    <td>
                                        <% if (bidder != null) { %>
                                            <span style="color: var(--bs-color-text); font-weight: 600;">👤 <%= bidder %></span>
                                        <% } else { %>
                                            <span style="color: var(--bs-color-faint); font-style: italic;">No bids yet</span>
                                        <% } %>
                                    </td>
                                    <td style="color: var(--bs-color-muted); font-family: ui-monospace, monospace; font-size: 13px;">
                                        <%= item.get("endTime") %>
                                    </td>
                                    <td style="text-align: right; white-space: nowrap;">
                                        <a href="admin-bid-history?auctionId=<%= item.get("auctionId") %>"
                                           class="bs-button bs-button--secondary bs-button--sm"
                                           style="margin-right: 6px;">
                                            📜 Logs
                                        </a>
                                        <form action="admin-actions" method="post" onsubmit="return confirm('Immediately terminate Lot #<%= item.get("auctionId") %> and award to the leading bidder?');" style="display: inline;">
                                            <input type="hidden" name="action" value="forceClose">
                                            <input type="hidden" name="auctionId" value="<%= item.get("auctionId") %>">
                                            <button type="submit" class="bs-button bs-button--danger bs-button--sm">
                                                ⏹ Force Close
                                            </button>
                                        </form>
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            <% } %>
        </section>

        <!-- SECTION 2: PRODUCT APPROVAL QUEUE -->
        <section class="registration-form-card bs-card" style="padding: 28px; margin-bottom: 32px;">
            <div style="margin-bottom: 20px;">
                <h2 style="color: var(--bs-color-text); font-size: 20px; font-weight: 800; margin: 0 0 4px 0;">
                    Pending Asset Moderation Queue
                </h2>
                <p style="color: var(--bs-color-muted); font-size: 13px; margin: 0;">
                    Review submitted merchant items, assign auction durations, configure minimum bid steps, and launch to the floor.
                </p>
            </div>

            <%
                List<Product> pendingProducts = (List<Product>) request.getAttribute("pendingProducts");
                if (pendingProducts == null || pendingProducts.isEmpty()) {
            %>
                <div style="text-align: center; padding: 40px 0;">
                    <p style="color: var(--bs-color-muted); font-size: 14px;">
                        Queue clear. No merchant assets awaiting moderation.
                    </p>
                </div>
            <% } else { %>
                <div class="bs-table-wrap">
                    <table class="bs-table">
                        <thead>
                            <tr>
                                <th>Item ID</th>
                                <th>Asset Title</th>
                                <th>Description</th>
                                <th>Category</th>
                                <th>Reserve Price</th>
                                <th>Auction Duration</th>
                                <th>Min Increment</th>
                                <th style="text-align: right;">Launch Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (Product p : pendingProducts) {
                                String pDesc = p.getDescription();
                            %>
                                <tr>
                                    <td style="color: var(--bs-color-muted); font-weight: 700; font-family: ui-monospace, monospace;">
                                        #<%= p.getProductId() %>
                                    </td>
                                    <td style="font-weight: 700; color: var(--bs-color-text);"><%= p.getProductName() %></td>
                                    <td style="color: var(--bs-color-muted); max-width: 220px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" title="<%= pDesc != null ? pDesc : "" %>">
                                        <%= pDesc != null && !pDesc.trim().isEmpty() ? pDesc : "—" %>
                                    </td>
                                    <td style="color: var(--bs-color-muted);"><%= p.getCategory() %></td>
                                    <td style="font-family: ui-monospace, monospace; font-weight: 600;">₹<%= p.getStartingPrice() %></td>
                                    <form action="admin-actions" method="post">
                                        <input type="hidden" name="action" value="approveProduct">
                                        <input type="hidden" name="productId" value="<%= p.getProductId() %>">
                                        <td>
                                            <select name="durationHours" class="bs-field__control" style="padding: 6px 10px; font-size: 12.5px; width: auto;">
                                                <option value="1">1 Hour</option>
                                                <option value="6">6 Hours</option>
                                                <option value="24" selected>24 Hours</option>
                                                <option value="48">48 Hours</option>
                                            </select>
                                        </td>
                                        <td>
                                            <input type="number" name="minIncrement" value="50" min="10" step="10" class="bs-field__control" style="padding: 6px 10px; font-size: 12.5px; width: 85px; font-family: ui-monospace, monospace;">
                                        </td>
                                        <td style="text-align: right;">
                                            <button type="submit" class="bs-button bs-button--primary bs-button--sm">
                                                Approve & Launch
                                            </button>
                                        </td>
                                    </form>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            <% } %>
        </section>

        <!-- SECTION 3: PENDING SELLER VERIFICATION QUEUE -->
        <section class="registration-form-card bs-card" style="padding: 28px;">
            <div style="margin-bottom: 20px;">
                <h2 style="color: var(--bs-color-text); font-size: 20px; font-weight: 800; margin: 0 0 4px 0;">
                    Pending Merchant Applications
                </h2>
                <p style="color: var(--bs-color-muted); font-size: 13px; margin: 0;">
                    Perform due-diligence verifications on prospective merchants before granting catalog listing rights.
                </p>
            </div>

            <%
                List<User> pendingSellers = (List<User>) request.getAttribute("pendingSellers");
                if (pendingSellers == null || pendingSellers.isEmpty()) {
            %>
                <div style="text-align: center; padding: 40px 0;">
                    <p style="color: var(--bs-color-muted); font-size: 14px;">
                        No merchant applications currently awaiting verification.
                    </p>
                </div>
            <% } else { %>
                <div class="bs-table-wrap">
                    <table class="bs-table">
                        <thead>
                            <tr>
                                <th>User ID</th>
                                <th>Business / Merchant Name</th>
                                <th>Contact Email</th>
                                <th style="text-align: right;">Review Decision</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (User s : pendingSellers) { %>
                                <tr>
                                    <td style="color: var(--bs-color-muted); font-weight: 700; font-family: ui-monospace, monospace;">
                                        #<%= s.getUserId() %>
                                    </td>
                                    <td style="font-weight: 700; color: var(--bs-color-text);"><%= s.getFullName() %></td>
                                    <td style="color: var(--bs-color-muted);"><%= s.getEmail() %></td>
                                    <td style="text-align: right; white-space: nowrap;">
                                        <form action="admin-actions" method="post" style="display: inline; margin-right: 6px;">
                                            <input type="hidden" name="action" value="verifySeller">
                                            <input type="hidden" name="sellerId" value="<%= s.getUserId() %>">
                                            <input type="hidden" name="status" value="APPROVED">
                                            <button type="submit" class="bs-button bs-button--primary bs-button--sm">
                                                ✓ Approve Merchant
                                            </button>
                                        </form>
                                        <form action="admin-actions" method="post" style="display: inline;">
                                            <input type="hidden" name="action" value="verifySeller">
                                            <input type="hidden" name="sellerId" value="<%= s.getUserId() %>">
                                            <input type="hidden" name="status" value="REJECTED">
                                            <button type="submit" class="bs-button bs-button--danger bs-button--sm">
                                                ✕ Reject
                                            </button>
                                        </form>
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            <% } %>
        </section>
    </main>

    <script src="assets/js/app.js"></script>
</body>
</html>
