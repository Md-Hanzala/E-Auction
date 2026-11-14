<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="com.eauction.model.Auction" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Buyer Floor | HASHbid</title>
    <link rel="icon" type="image/svg+xml" href="assets/images/hashbid-logo.svg">
    <link rel="stylesheet" href="assets/css/style.css">
    <style>
        /* Print Stylesheet for Official Invoice */
        @media print {
            body * { visibility: hidden; }
            #printableInvoice, #printableInvoice * { visibility: visible; }
            #printableInvoice { position: absolute; left: 0; top: 0; width: 100%; }
            .no-print { display: none !important; }
        }
    </style>
</head>
<body class="auth-page">

    <!-- Floating Feedback Toast Notification -->
    <div id="toast" class="toast-notification bs-toast" role="status" aria-live="polite"></div>

    <!-- Navigation Header -->
    <header>
        <nav class="navbar bs-navbar">
            <a href="index.html" class="logo bs-logo">
                <img src="assets/images/hashbid-logo.svg" alt="HASHbid Emblem" width="24" height="24" style="border-radius: 6px;">
                HASH<span>bid</span>
            </a>

            <div class="nav-links bs-nav-links">
                <!-- User Identity Badge -->
                <div class="bs-user-chip">
                    <div style="width: 32px; height: 32px; border-radius: 50%; background: var(--bs-color-primary-soft); color: var(--bs-color-primary-light); display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 800; font-family: Georgia, serif;">
                        <%= (session.getAttribute("userName") != null && !session.getAttribute("userName").toString().isEmpty()) ? session.getAttribute("userName").toString().substring(0, 1).toUpperCase() : "B" %>
                    </div>
                    <div class="bs-user-chip__info">
                        <div class="bs-user-chip__name">
                            <%= session.getAttribute("userName") != null ? session.getAttribute("userName") : "Authenticated Buyer" %>
                        </div>
                        <div class="bs-user-chip__meta">
                            <span class="bs-status bs-status--won" style="padding: 1px 6px; font-size: 10px;">
                                <%= session.getAttribute("userRole") != null ? session.getAttribute("userRole") : "BUYER" %>
                            </span>
                            <span>#<%= session.getAttribute("userId") != null ? session.getAttribute("userId") : "0" %></span>
                        </div>
                    </div>
                </div>

                <a href="logout" class="bs-button bs-button--danger bs-button--sm">
                    Sign Out
                </a>
            </div>
        </nav>
    </header>

    <main class="form-page-container bs-page-container" style="max-width: 1240px; width: 94%;">
        <!-- Floor Header -->
        <section class="form-intro bs-page-header" style="text-align: left; margin-bottom: 28px; display: flex; justify-content: space-between; align-items: flex-end; flex-wrap: wrap; gap: 16px;">
            <div>
                <span class="bs-eyebrow auth-tag">LIVE MARKETPLACE</span>
                <h1>Live Auction Floor</h1>
                <p>Participate in active auctions, place real-time bids, and track your winning hammer prices.</p>
            </div>

            <!-- In-DOM Filter Control (Component 9) -->
            <div style="min-width: 260px;">
                <input type="search" id="lotFilterInput" class="bs-field__control" placeholder="Search visible lots..." style="padding: 9px 14px; font-size: 13px;">
                <p id="lotFilterStatus" class="bs-field__help" style="text-align: right;"></p>
            </div>
        </section>

        <!-- Server Feedback Banners -->
        <%
            String successMsg = request.getParameter("bidSuccess");
            String errorMsg = request.getParameter("error");
            if ("true".equals(successMsg)) {
        %>
            <div class="bs-alert bs-alert--success">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10"/><path d="m9 12 2 2 4-4"/>
                </svg>
                Bid accepted! You are currently the highest bidder on this lot.
            </div>
        <% } else if (errorMsg != null) { %>
            <div class="bs-alert bs-alert--error">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
                Bid rejected: Your offer must exceed the current bid plus the required minimum increment.
            </div>
        <% } %>

        <!-- SECTION 1: ACTIVE AUCTION LOTS -->
        <section class="registration-form-card bs-card" style="padding: 28px; margin-bottom: 32px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 22px;">
                <div>
                    <h2 style="color: var(--bs-color-text); font-size: 20px; font-weight: 800; margin: 0 0 4px 0;">
                        Live Active Lots
                    </h2>
                    <p style="color: var(--bs-color-muted); font-size: 13px; margin: 0;">
                        Bids update continuously. Timers reflect remaining session time.
                    </p>
                </div>
                <span class="bs-status bs-status--live status-badge status-live">
                    Auto-Polling Active
                </span>
            </div>

            <%
                List<Auction> activeAuctions = (List<Auction>) request.getAttribute("activeAuctions");
                if (activeAuctions == null || activeAuctions.isEmpty()) {
            %>
                <div style="text-align: center; padding: 60px 0;">
                    <div style="font-size: 42px; margin-bottom: 12px;">🏷️</div>
                    <h3 style="color: var(--bs-color-text); font-size: 18px; margin-bottom: 6px;">No Live Auctions Scheduled</h3>
                    <p style="color: var(--bs-color-muted); font-size: 14px;">The floor is currently quiet. Check back soon for approved seller lots.</p>
                </div>
            <%
                } else {
            %>
                <div class="bs-auction-grid" id="auctionGrid">
                    <%
                        for (Auction a : activeAuctions) {
                            BigDecimal minInc = (a.getMinIncrement() != null) ? a.getMinIncrement() : BigDecimal.valueOf(50);
                            BigDecimal minAllowedBid = a.getCurrentBid().add(minInc);

                            String rawPath = a.getImagePath();
                            String displayImg;
                            if (rawPath == null || rawPath.trim().isEmpty()) {
                                displayImg = "assets/images/auction-gavel.avif";
                            } else if (rawPath.startsWith("http://") || rawPath.startsWith("https://")) {
                                displayImg = rawPath;
                            } else if (rawPath.startsWith("/")) {
                                displayImg = request.getContextPath() + rawPath;
                            } else {
                                displayImg = request.getContextPath() + "/" + rawPath;
                            }
                    %>
                        <article class="bs-auction-card" data-bs-filter-item>
                            <div class="bs-auction-card__media">
                                <img src="<%= displayImg %>"
                                     alt="<%= a.getProductName() %>"
                                     class="bs-auction-card__img"
                                     onerror="this.onerror=null; this.src='assets/images/auction-gavel.avif';">

                                <div class="bs-auction-card__badge-overlay">
                                    <span class="bs-status bs-status--live status-badge status-live">
                                        LOT #<%= a.getAuctionId() %>
                                    </span>
                                </div>
                            </div>

                            <div class="bs-auction-card__body">
                                <div>
                                    <div class="bs-auction-card__header">
                                        <h3 class="bs-auction-card__title"><%= a.getProductName() %></h3>
                                        <span class="bs-auction-card__category"><%= a.getCategory() %></span>
                                    </div>

                                    <p class="bs-auction-card__desc" title="<%= a.getDescription() != null ? a.getDescription() : "" %>">
                                        <%= a.getDescription() != null && !a.getDescription().trim().isEmpty() ? a.getDescription() : "Verified merchant asset presented on the floor." %>
                                    </p>
                                </div>

                                <div>
                                    <div class="bs-auction-card__stats">
                                        <div>
                                            <span style="font-size: 11px; font-weight: 700; color: var(--bs-color-muted); text-transform: uppercase; display: block; margin-bottom: 4px;">
                                                Current Top Bid
                                            </span>
                                            <span id="bid-display-<%= a.getAuctionId() %>"
                                                  data-current="<%= a.getCurrentBid() %>"
                                                  class="bs-auction-card__current-bid">
                                                ₹<%= a.getCurrentBid() %>
                                            </span>
                                        </div>

                                        <div style="text-align: right;">
                                            <span style="font-size: 11px; font-weight: 700; color: var(--bs-color-muted); text-transform: uppercase; display: block; margin-bottom: 4px;">
                                                Time Remaining
                                            </span>
                                            <span class="countdown-timer bs-auction-card__timer"
                                                  id="timer-<%= a.getAuctionId() %>"
                                                  data-endtime="<%= a.getEndTime().getTime() %>">
                                                Syncing...
                                            </span>
                                        </div>
                                    </div>

                                    <!-- In-Place AJAX Bid Placement Form -->
                                    <div class="bs-auction-card__footer">
                                        <form id="bid-form-<%= a.getAuctionId() %>" action="place-bid" method="post">
                                            <input type="hidden" name="auctionId" value="<%= a.getAuctionId() %>">

                                            <div style="display: flex; gap: 8px;">
                                                <input type="number"
                                                       step="1"
                                                       id="input-bid-<%= a.getAuctionId() %>"
                                                       name="bidAmount"
                                                       min="<%= minAllowedBid %>"
                                                       placeholder="Min ₹<%= minAllowedBid %>"
                                                       required
                                                       class="bs-field__control"
                                                       style="padding: 8px 10px; font-size: 13px; font-family: ui-monospace, SFMono-Regular, Menlo, monospace;">

                                                <button type="submit"
                                                        id="btn-bid-<%= a.getAuctionId() %>"
                                                        class="bs-button bs-button--primary form-submit-button"
                                                        style="width: auto; padding: 8px 16px; font-size: 13px; white-space: nowrap;">
                                                    <span class="bs-spinner" aria-hidden="true"></span>
                                                    <span>Place Bid</span>
                                                </button>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        </article>
                    <% } %>
                </div>
            <% } %>
        </section>

        <!-- SECTION 2: BUYER BIDDING HISTORY -->
        <section class="registration-form-card bs-card" style="padding: 28px;">
            <div style="margin-bottom: 20px;">
                <h2 style="color: var(--bs-color-text); font-size: 20px; font-weight: 800; margin: 0 0 4px 0;">
                    Your Bidding Ledger
                </h2>
                <p style="color: var(--bs-color-muted); font-size: 13px; margin: 0;">
                    Complete record of lots you have contested, leading positions, and awarded hammer receipts.
                </p>
            </div>

            <%
                List<Auction> buyerHistory = (List<Auction>) request.getAttribute("buyerHistory");
                if (buyerHistory == null || buyerHistory.isEmpty()) {
            %>
                <div style="text-align: center; padding: 40px 0;">
                    <p style="color: var(--bs-color-muted); font-size: 14px;">
                        You have not placed bids on any lots yet. Enter an offer above to begin tracking.
                    </p>
                </div>
            <% } else { %>
                <div class="bs-table-wrap">
                    <table class="bs-table">
                        <thead>
                            <tr>
                                <th>Item Title</th>
                                <th>Category</th>
                                <th>Your Bid Amount</th>
                                <th>Position / Status</th>
                                <th>Scheduled Closing</th>
                                <th style="text-align: right;">Settlement Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <%
                                for (Auction b : buyerHistory) {
                                    boolean isActive = "ACTIVE".equalsIgnoreCase(b.getStatus());
                                    boolean isWon = "CLOSED".equalsIgnoreCase(b.getStatus());
                            %>
                                <tr>
                                    <td style="font-weight: 700; color: var(--bs-color-text);"><%= b.getProductName() %></td>
                                    <td style="color: var(--bs-color-muted);"><%= b.getCategory() %></td>
                                    <td style="font-weight: 800; color: var(--bs-color-primary-light); font-family: ui-monospace, monospace;">
                                        ₹<%= b.getCurrentBid() %>
                                    </td>
                                    <td>
                                        <% if (isActive) { %>
                                            <span class="bs-status bs-status--live">
                                                Leading Bid
                                            </span>
                                        <% } else if (isWon) { %>
                                            <span class="bs-status bs-status--won">
                                                🏆 Won Auction
                                            </span>
                                        <% } else { %>
                                            <span class="bs-status bs-status--ended">
                                                <%= b.getStatus() %>
                                            </span>
                                        <% } %>
                                    </td>
                                    <td style="color: var(--bs-color-muted); font-size: 13px;"><%= b.getEndTime() %></td>
                                    <td style="text-align: right;">
                                        <% if (isWon) { %>
                                            <button type="button"
                                                    onclick="openInvoiceModal('<%= b.getAuctionId() %>', '<%= b.getProductName() %>', '<%= b.getCategory() %>', '₹<%= b.getCurrentBid() %>', '<%= b.getSellerName() %>', '<%= b.getSellerEmail() %>', '<%= b.getSellerPhone() %>')"
                                                    class="bs-button bs-button--secondary bs-button--sm"
                                                    style="border-color: var(--bs-color-border); color: var(--bs-color-primary-light);">
                                                📄 View Invoice
                                            </button>
                                        <% } else { %>
                                            <span style="color: var(--bs-color-muted); font-size: 12px;">Active Competition</span>
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

    <!-- Modal: Official Settlement Invoice (Component 6) -->
    <div id="invoiceModal" class="invoice-modal-overlay bs-modal-overlay" onclick="closeInvoiceOnBackdrop(event)">
        <div id="printableInvoice" class="invoice-card bs-modal-card">
            <div style="display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 1px solid var(--bs-color-border); padding-bottom: 16px; margin-bottom: 20px;">
                <div>
                    <h2 style="margin: 0; font-size: 20px; font-weight: 700; color: var(--bs-color-text); font-family: Georgia, serif;">Official Settlement Receipt</h2>
                    <small id="invNumber" style="color: var(--bs-color-muted); font-family: ui-monospace, monospace; font-size: 12px;">INV-LOT-000</small>
                </div>
                <span class="bs-status bs-status--won">
                    AWARDED
                </span>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; font-size: 13px; margin-bottom: 24px;">
                <div>
                    <strong style="color: var(--bs-color-muted); display: block; margin-bottom: 4px; font-size: 11px; text-transform: uppercase;">Winning Buyer</strong>
                    <div style="font-weight: 700; color: var(--bs-color-text);"><%= session.getAttribute("userName") != null ? session.getAttribute("userName") : "Buyer" %></div>
                    <div style="color: var(--bs-color-muted); font-size: 12px;"><%= session.getAttribute("userEmail") != null ? session.getAttribute("userEmail") : "" %></div>
                </div>
                <div>
                    <strong style="color: var(--bs-color-muted); display: block; margin-bottom: 4px; font-size: 11px; text-transform: uppercase;">Originating Merchant</strong>
                    <div id="invSellerName" style="font-weight: 700; color: var(--bs-color-text);">—</div>
                    <div id="invSellerEmail" style="color: var(--bs-color-muted); font-size: 12px;">—</div>
                    <div id="invSellerPhone" style="color: var(--bs-color-muted); font-size: 12px;">—</div>
                </div>
            </div>

            <div class="bs-table-wrap" style="margin-bottom: 24px;">
                <table class="bs-table">
                    <thead>
                        <tr>
                            <th>Item Description</th>
                            <th>Category</th>
                            <th style="text-align: right;">Hammer Price</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td id="invProductName" style="font-weight: 700; color: var(--bs-color-text);">—</td>
                            <td id="invCategory" style="color: var(--bs-color-muted);">—</td>
                            <td id="invAmount" style="text-align: right; font-weight: 800; color: var(--bs-color-primary-light); font-size: 16px; font-family: ui-monospace, monospace;">₹0</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="no-print" style="display: flex; justify-content: space-between; align-items: center;">
                <button type="button" onclick="window.print()" class="bs-button bs-button--secondary bs-button--sm">
                    🖨 Print Receipt
                </button>
                <button type="button" onclick="document.getElementById('invoiceModal').style.display='none'" class="bs-button bs-button--primary bs-button--sm">
                    Close Receipt
                </button>
            </div>
        </div>
    </div>

    <script src="assets/js/app.js"></script>
    <script>
        // 1. Authoritative Countdown Timer Engine
        function updateTimers() {
            const now = new Date().getTime();
            const timers = document.querySelectorAll('.countdown-timer');

            timers.forEach(timer => {
                const targetTime = parseInt(timer.getAttribute('data-endtime'), 10);
                const diff = targetTime - now;
                const auctionId = timer.id.replace('timer-', '');
                const bidBtn = document.getElementById('btn-bid-' + auctionId);
                const bidInput = document.getElementById('input-bid-' + auctionId);

                if (diff <= 0) {
                    timer.innerText = "CLOSED";
                    timer.style.color = "var(--bs-color-muted)";
                    timer.style.background = "var(--bs-color-surface-elevated)";
                    timer.style.borderColor = "var(--bs-color-border)";
                    if (bidBtn) {
                        bidBtn.disabled = true;
                        bidBtn.innerText = "Closed";
                    }
                    if (bidInput) {
                        bidInput.disabled = true;
                    }
                } else {
                    const hours = Math.floor(diff / (1000 * 60 * 60));
                    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
                    const seconds = Math.floor((diff % (1000 * 60)) / 1000);

                    const pad = num => String(num).padStart(2, '0');
                    timer.innerText = pad(hours) + "h " + pad(minutes) + "m " + pad(seconds) + "s";
                }
            });
        }

        setInterval(updateTimers, 1000);
        updateTimers();

        // 2. Real-Time AJAX Bid Polling (3000ms Interval)
        async function pollLiveBids() {
            if (document.hidden) return;
            try {
                const response = await fetch('live-auction-updates');
                if (!response.ok) return;

                const auctions = await response.json();

                auctions.forEach(lot => {
                    const bidDisplay = document.getElementById('bid-display-' + lot.auctionId);
                    const bidInput = document.getElementById('input-bid-' + lot.auctionId);

                    if (bidDisplay) {
                        const prevBid = parseFloat(bidDisplay.getAttribute('data-current'));

                        if (lot.currentBid > prevBid) {
                            bidDisplay.innerText = '₹' + lot.currentBid;
                            bidDisplay.setAttribute('data-current', lot.currentBid);

                            // Flash bronze highlight on update
                            bidDisplay.style.color = 'var(--bs-color-primary-light)';
                            bidDisplay.style.transform = 'scale(1.15)';
                            setTimeout(() => {
                                bidDisplay.style.color = 'var(--bs-color-primary-light)';
                                bidDisplay.style.transform = 'scale(1)';
                            }, 800);
                        }
                    }

                    if (bidInput) {
                        bidInput.min = lot.minAllowedBid;
                        bidInput.placeholder = 'Min ₹' + lot.minAllowedBid;
                    }
                });
            } catch (err) {
                console.error('Bid poll sync error:', err);
            }
        }

        setInterval(pollLiveBids, 3000);

        // 3. In-Place AJAX Bidding Submission (No Page Reload)
        document.addEventListener('submit', async function(e) {
            const form = e.target;
            if (!form.id || !form.id.startsWith('bid-form-')) return;

            e.preventDefault();

            const submitBtn = form.querySelector('button[type="submit"]');
            submitBtn.classList.add('is-loading');
            submitBtn.disabled = true;

            const formData = new FormData(form);
            const params = new URLSearchParams(formData);

            try {
                const response = await fetch('place-bid', {
                    method: 'POST',
                    body: params,
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest',
                        'Content-Type': 'application/x-www-form-urlencoded'
                    }
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    showToast(data.message, 'success');

                    const display = document.getElementById('bid-display-' + data.auctionId);
                    const input = document.getElementById('input-bid-' + data.auctionId);

                    if (display) {
                        display.innerText = '₹' + data.currentBid;
                        display.setAttribute('data-current', data.currentBid);
                        display.style.color = 'var(--bs-color-primary-light)';
                        display.style.transform = 'scale(1.2)';
                        setTimeout(() => {
                            display.style.color = 'var(--bs-color-primary-light)';
                            display.style.transform = 'scale(1)';
                        }, 800);
                    }

                    if (input) {
                        input.value = '';
                        input.min = data.nextMinBid;
                        input.placeholder = 'Min ₹' + data.nextMinBid;
                    }
                } else {
                    showToast(data.message || 'Bid rejected.', 'error');
                }
            } catch (err) {
                console.error('AJAX Bid Error:', err);
                showToast('Network error while placing bid. Please try again.', 'error');
            } finally {
                submitBtn.classList.remove('is-loading');
                submitBtn.disabled = false;
            }
        });

        // 4. Toast Notification Engine
        function showToast(message, type) {
            const toast = document.getElementById('toast');
            if (!toast) return;
            toast.innerText = (type === 'success' ? '✓ ' : '⚠ ') + message;
            toast.className = 'toast-notification toast-' + type + ' toast-visible bs-toast bs-toast--' + type;
            setTimeout(() => {
                toast.classList.remove('toast-visible');
            }, 3800);
        }

        // 5. Invoice Modal Handlers
        function openInvoiceModal(auctionId, product, category, amount, sellerName, sellerEmail, sellerPhone) {
            document.getElementById('invNumber').innerText = 'INV-LOT-#' + auctionId;
            document.getElementById('invProductName').innerText = product;
            document.getElementById('invCategory').innerText = category;
            document.getElementById('invAmount').innerText = amount;
            document.getElementById('invSellerName').innerText = (sellerName && sellerName !== 'null') ? sellerName : 'Authorized Merchant';
            document.getElementById('invSellerEmail').innerText = (sellerEmail && sellerEmail !== 'null') ? sellerEmail : 'support@hashbid.com';
            document.getElementById('invSellerPhone').innerText = (sellerPhone && sellerPhone !== 'null') ? sellerPhone : 'On File';
            document.getElementById('invoiceModal').style.display = 'flex';
        }

        function closeInvoiceOnBackdrop(e) {
            if (e.target.id === 'invoiceModal') {
                document.getElementById('invoiceModal').style.display = 'none';
            }
        }

        // 6. In-DOM Search Filter
        initBidSphereFilter('lotFilterInput', '[data-bs-filter-item]', 'lotFilterStatus');
    </script>
</body>
</html>
