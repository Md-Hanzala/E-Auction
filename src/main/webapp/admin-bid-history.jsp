<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.math.BigDecimal" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bid Audit Log | HASHbid Administration</title>
    <link rel="icon" type="image/svg+xml" href="assets/images/hashbid-logo.svg">
    <link rel="stylesheet" href="assets/css/style.css">
</head>
<body class="auth-page">

    <!-- Navigation Header -->
    <header>
        <nav class="navbar bs-navbar">
            <a href="admin-dashboard" class="logo bs-logo">
                <img src="assets/images/hashbid-logo.svg" alt="HASHbid Emblem" width="24" height="24" style="border-radius: 6px;">
                HASH<span>bid</span>
                <span class="logo-badge" style="background: var(--bs-color-danger-soft); color: var(--bs-color-danger); border-color: rgba(200, 121, 121, 0.4);">Audit</span>
            </a>

            <div class="nav-links bs-nav-links">
                <a href="admin-dashboard" class="bs-button bs-button--secondary bs-button--sm">
                    ← Back to Operations
                </a>

                <div class="bs-user-chip">
                    <div style="width: 32px; height: 32px; border-radius: 50%; background: var(--bs-color-danger-soft); color: var(--bs-color-danger); display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 800;">
                        🛡️
                    </div>
                    <div class="bs-user-chip__info">
                        <div class="bs-user-chip__name">
                            <%= session.getAttribute("userName") != null ? session.getAttribute("userName") : "Administrator" %>
                        </div>
                        <div class="bs-user-chip__meta">
                            <span class="bs-status bs-status--danger" style="padding: 1px 6px; font-size: 10px;">
                                ADMIN
                            </span>
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
        <% Integer selectedAuctionId = (Integer) request.getAttribute("selectedAuctionId"); %>

        <!-- Audit Header -->
        <section class="form-intro bs-page-header" style="text-align: left; margin-bottom: 28px; display: flex; justify-content: space-between; align-items: flex-end; flex-wrap: wrap; gap: 16px;">
            <div>
                <span class="bs-eyebrow auth-tag" style="background: var(--bs-color-danger-soft); color: var(--bs-color-danger); border-color: rgba(200, 121, 121, 0.35);">
                    CHRONOLOGICAL AUDIT TRAIL
                </span>
                <h1>Bid Transaction Ledger</h1>
                <p>Complete tamper-evident log of all buyer bidding transactions, timestamps, and hammer amounts.</p>
            </div>

            <div style="display: flex; gap: 12px; align-items: center; flex-wrap: wrap;">
                <% if (selectedAuctionId != null) { %>
                    <a href="admin-bid-history" class="bs-button bs-button--primary bs-button--sm">
                        ← Show All Lots
                    </a>
                <% } %>

                <!-- Quick Filter Input (Component 9) -->
                <div style="min-width: 260px;">
                    <input type="search" id="bidAuditSearchInput" class="bs-field__control" placeholder="Search bids, lots, or emails..." style="padding: 8px 12px; font-size: 13px;">
                    <p id="bidAuditSearchStatus" class="bs-field__help" style="text-align: right;"></p>
                </div>
            </div>
        </section>

        <!-- Audit Table Card -->
        <section class="registration-form-card bs-card" style="padding: 28px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                <h2 style="color: var(--bs-color-text); font-size: 20px; font-weight: 800; margin: 0;">
                    <%= selectedAuctionId != null ? "Bidding Events for Lot #" + selectedAuctionId : "All Recorded Bid Transactions" %>
                </h2>
                <span class="bs-status bs-status--won">
                    Database Verified
                </span>
            </div>

            <%
                List<Map<String, Object>> bidHistory = (List<Map<String, Object>>) request.getAttribute("bidHistory");
                if (bidHistory == null || bidHistory.isEmpty()) {
            %>
                <div style="text-align: center; padding: 50px 0;">
                    <p style="color: var(--bs-color-muted); font-size: 14px;">
                        No bids recorded in the audit ledger yet.
                    </p>
                </div>
            <% } else { %>
                <div class="bs-table-wrap">
                    <table class="bs-table">
                        <thead>
                            <tr>
                                <th>Bid ID</th>
                                <th>Lot ID</th>
                                <th>Asset Title</th>
                                <th>Participant Name</th>
                                <th>Participant Email</th>
                                <th>Submitted Amount</th>
                                <th>Recorded Timestamp</th>
                                <th>Lot Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (Map<String, Object> b : bidHistory) {
                                String status = (String) b.get("auctionStatus");
                            %>
                                <tr data-bs-filter-item>
                                    <td style="color: var(--bs-color-muted); font-weight: 700; font-family: ui-monospace, monospace;">
                                        #<%= b.get("bidId") %>
                                    </td>
                                    <td style="font-weight: 700; color: var(--bs-color-primary-light); font-family: ui-monospace, monospace;">
                                        #<%= b.get("auctionId") %>
                                    </td>
                                    <td style="font-weight: 700; color: var(--bs-color-text);"><%= b.get("productName") %></td>
                                    <td style="color: var(--bs-color-text); font-weight: 600;">👤 <%= b.get("bidderName") %></td>
                                    <td style="color: var(--bs-color-muted); font-size: 13px;"><%= b.get("bidderEmail") %></td>
                                    <td style="font-weight: 800; color: var(--bs-color-primary-light); font-size: 15px; font-family: ui-monospace, monospace;">
                                        ₹<%= b.get("bidAmount") %>
                                    </td>
                                    <td style="color: var(--bs-color-muted); font-family: ui-monospace, monospace; font-size: 13px;">
                                        <%= b.get("bidTime") %>
                                    </td>
                                    <td>
                                        <% if ("ACTIVE".equalsIgnoreCase(status)) { %>
                                            <span class="bs-status bs-status--live">
                                                ACTIVE
                                            </span>
                                        <% } else { %>
                                            <span class="bs-status bs-status--ended">
                                                CLOSED
                                            </span>
                                        <% } %>
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
    <script>
        initBidSphereFilter('bidAuditSearchInput', '[data-bs-filter-item]', 'bidAuditSearchStatus');
    </script>
</body>
</html>
