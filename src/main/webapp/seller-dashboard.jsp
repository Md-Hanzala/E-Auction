<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.math.BigDecimal" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Seller Console | HASHbid Merchant Hub</title>
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
            </a>

            <div class="nav-links bs-nav-links">
                <!-- Seller Identity Badge -->
                <div class="bs-user-chip">
                    <div style="width: 32px; height: 32px; border-radius: 50%; background: var(--bs-color-primary-soft); color: var(--bs-color-primary-light); display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 800; font-family: Georgia, serif;">
                        <%= (session.getAttribute("userName") != null && !session.getAttribute("userName").toString().isEmpty()) ? session.getAttribute("userName").toString().substring(0, 1).toUpperCase() : "S" %>
                    </div>
                    <div class="bs-user-chip__info">
                        <div class="bs-user-chip__name">
                            <%= session.getAttribute("userName") != null ? session.getAttribute("userName") : "Merchant" %>
                        </div>
                        <div class="bs-user-chip__meta">
                            <span class="bs-status bs-status--warning" style="padding: 1px 6px; font-size: 10px;">
                                SELLER
                            </span>
                            <span>#<%= session.getAttribute("userId") %></span>
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
        <!-- Console Header -->
        <section class="form-intro bs-page-header" style="text-align: left; margin-bottom: 28px;">
            <span class="bs-eyebrow auth-tag" style="background: var(--bs-color-primary-soft); color: var(--bs-color-primary-light); border-color: rgba(181, 141, 90, 0.4);">
                MERCHANT OPERATIONS
            </span>
            <h1>Inventory & Fulfillment Console</h1>
            <p>Submit catalog lots for administrative verification, monitor bidding trajectories, and access shipping details for settled auctions.</p>
        </section>

        <!-- Dynamic Feedback Alerts -->
        <%
            String success = request.getParameter("success");
            String error = request.getParameter("error");
            if ("product_submitted".equals(success)) {
        %>
            <div class="bs-alert bs-alert--success">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10"/><path d="m9 12 2 2 4-4"/>
                </svg>
                Item submitted successfully! It has entered the administrative vetting queue. Once approved, it will be scheduled on the live floor.
            </div>
        <% } else if (error != null) { %>
            <div class="bs-alert bs-alert--error">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
                Operation failed: <%= error.replace('_', ' ') %>
            </div>
        <% } %>

        <!-- SECTION 1: LIST NEW PRODUCT FORM -->
        <section class="registration-form-card bs-card" style="padding: 28px; margin-bottom: 32px;">
            <div style="margin-bottom: 22px;">
                <h2 style="color: var(--bs-color-text); font-size: 20px; font-weight: 800; margin: 0 0 4px 0;">
                    List a New Asset for Auction
                </h2>
                <p style="color: var(--bs-color-muted); font-size: 13px; margin: 0;">
                    Fill out item specifications and upload high-resolution provenance imagery.
                </p>
            </div>

            <form action="add-product" method="post" enctype="multipart/form-data" id="addProductForm">
                <div class="form-grid bs-form-grid">
                    <div class="form-group bs-field">
                        <label for="productName" class="bs-field__label">Asset Title</label>
                        <input type="text" id="productName" name="productName" class="bs-field__control" placeholder="e.g. Sony Alpha 7 IV Mirrorless" required>
                    </div>

                    <div class="form-group bs-field">
                        <label for="category" class="bs-field__label">Asset Category</label>
                        <select id="category" name="category" class="bs-field__control" required>
                            <option value="Electronics & Cameras">Electronics & Cameras</option>
                            <option value="Luxury Watches">Luxury Watches</option>
                            <option value="Art & Paintings">Art & Paintings</option>
                            <option value="Antiques & Collectibles">Antiques & Collectibles</option>
                            <option value="Sports Memorabilia">Sports Memorabilia</option>
                        </select>
                    </div>

                    <div class="form-group bs-field">
                        <label for="startingPrice" class="bs-field__label">Starting Reserve Price (₹)</label>
                        <input type="number" step="1" id="startingPrice" name="startingPrice" class="bs-field__control" placeholder="e.g. 15000" min="1" required>
                    </div>

                    <div class="form-group bs-field">
                        <label for="productImageInput" class="bs-field__label">Asset Photography</label>
                        <input type="file" id="productImageInput" name="productImage" class="bs-field__control" accept="image/*" required>
                        <p id="productImageHelp" class="bs-field__help">Max 5MB (JPG, PNG, WebP). Image preview renders below.</p>
                        <img id="productImagePreview" class="bs-image-preview" alt="Selected product image preview" hidden>
                    </div>

                    <div class="form-group full-width bs-field bs-col-span-2">
                        <label for="description" class="bs-field__label">Condition, Provenance & Technical Specifications</label>
                        <textarea id="description" name="description" class="bs-field__control" rows="3" placeholder="Provide accurate provenance, cosmetic condition, serials, and warranty details..." required></textarea>
                    </div>
                </div>

                <div style="margin-top: 24px; display: flex; justify-content: flex-end;">
                    <button type="submit" class="bs-button bs-button--primary form-submit-button" id="submitProductBtn" style="width: auto; padding: 11px 28px;">
                        <span class="bs-spinner" aria-hidden="true"></span>
                        <span>Submit Asset for Vetting</span>
                    </button>
                </div>
            </form>
        </section>

        <!-- SECTION 2: SELLER INVENTORY TABLE -->
        <section class="registration-form-card bs-card" style="padding: 28px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 22px; flex-wrap: wrap; gap: 12px;">
                <div>
                    <h2 style="color: var(--bs-color-text); font-size: 20px; font-weight: 800; margin: 0 0 4px 0;">
                        Merchant Catalog & Settlement Pipeline
                    </h2>
                    <p style="color: var(--bs-color-muted); font-size: 13px; margin: 0;">
                        Track review statuses, live floor lots, and access winning buyer delivery details.
                    </p>
                </div>
            </div>

            <%
                List<Map<String, Object>> inventory = (List<Map<String, Object>>) request.getAttribute("inventory");
                if (inventory == null || inventory.isEmpty()) {
            %>
                <div style="text-align: center; padding: 50px 0;">
                    <div style="font-size: 38px; margin-bottom: 12px;">📦</div>
                    <p style="color: var(--bs-color-muted); font-size: 14px;">
                        You have not submitted any items yet. Fill out the form above to submit your first asset.
                    </p>
                </div>
            <% } else { %>
                <div class="bs-table-wrap">
                    <table class="bs-table">
                        <thead>
                            <tr>
                                <th>Item ID</th>
                                <th>Asset Title</th>
                                <th>Category</th>
                                <th>Reserve Price</th>
                                <th>Top Bid</th>
                                <th>Review / Lot Status</th>
                                <th style="text-align: right;">Fulfillment Contact</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (Map<String, Object> item : inventory) {
                                String pStatus = (String) item.get("productStatus");
                                BigDecimal currentBid = (BigDecimal) item.get("currentBid");
                                boolean isSold = "SOLD".equalsIgnoreCase(pStatus);
                            %>
                                <tr>
                                    <td style="color: var(--bs-color-muted); font-weight: 700; font-family: ui-monospace, monospace;">
                                        #<%= item.get("productId") %>
                                    </td>
                                    <td style="font-weight: 700; color: var(--bs-color-text);"><%= item.get("productName") %></td>
                                    <td style="color: var(--bs-color-muted);"><%= item.get("category") %></td>
                                    <td style="font-family: ui-monospace, monospace;">₹<%= item.get("startingPrice") %></td>
                                    <td style="font-weight: 800; color: var(--bs-color-primary-light); font-family: ui-monospace, monospace;">
                                        <%= currentBid != null ? "₹" + currentBid : "—" %>
                                    </td>
                                    <td>
                                        <% if ("APPROVED".equalsIgnoreCase(pStatus)) { %>
                                            <span class="bs-status bs-status--live">
                                                Live on Floor
                                            </span>
                                        <% } else if ("PENDING".equalsIgnoreCase(pStatus)) { %>
                                            <span class="bs-status bs-status--warning">
                                                Under Review
                                            </span>
                                        <% } else if (isSold) { %>
                                            <span class="bs-status bs-status--won">
                                                🏆 Sold Lot
                                            </span>
                                        <% } else { %>
                                            <span class="bs-status bs-status--ended">
                                                <%= pStatus %>
                                            </span>
                                        <% } %>
                                    </td>
                                    <td style="text-align: right;">
                                        <% if (isSold && item.get("winnerName") != null) { %>
                                            <button type="button"
                                                    onclick="showDeliveryModal('<%= item.get("productName") %>', '₹<%= currentBid %>', '<%= item.get("winnerName") %>', '<%= item.get("winnerEmail") %>', '<%= item.get("winnerPhone") %>', '<%= item.get("winnerAddress") %>')"
                                                    class="bs-button bs-button--secondary bs-button--sm"
                                                    style="border-color: var(--bs-color-border); color: var(--bs-color-primary-light);">
                                                📦 Buyer Details
                                            </button>
                                        <% } else { %>
                                            <span style="color: var(--bs-color-muted); font-size: 12px;">—</span>
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

    <!-- Modal: Buyer Order & Delivery Details (Component 6) -->
    <div id="deliveryModal" class="modal-overlay bs-modal-overlay" onclick="closeDeliveryModal(event)">
        <div class="modal-card bs-modal-card">
            <div style="display: flex; justify-content: space-between; align-items: baseline; border-bottom: 1px solid var(--bs-color-border); padding-bottom: 14px; margin-bottom: 18px;">
                <h3 id="modalProductName" style="margin: 0; font-size: 18px; font-weight: 700; color: var(--bs-color-text); font-family: Georgia, serif;">Item Name</h3>
                <span id="modalFinalPrice" style="color: var(--bs-color-primary-light); font-weight: 800; font-size: 16px; font-family: ui-monospace, monospace;">₹0</span>
            </div>

            <span class="bs-eyebrow" style="margin-bottom: 14px;">WINNING BUYER DISPATCH RECORD</span>

            <div style="display: flex; flex-direction: column; gap: 12px; font-size: 13.5px; color: var(--bs-color-text); margin-bottom: 24px;">
                <div style="display: flex; justify-content: space-between; border-bottom: 1px solid var(--bs-color-border-subtle); padding-bottom: 6px;">
                    <span style="color: var(--bs-color-muted);">Recipient Name:</span>
                    <strong id="modalWinnerName" style="color: var(--bs-color-primary-light);"><%= session.getAttribute("userName") != null ? session.getAttribute("userName") : "—" %></strong>
                </div>
                <div style="display: flex; justify-content: space-between; border-bottom: 1px solid var(--bs-color-border-subtle); padding-bottom: 6px;">
                    <span style="color: var(--bs-color-muted);">Recipient Email:</span>
                    <span id="modalWinnerEmail">—</span>
                </div>
                <div style="display: flex; justify-content: space-between; border-bottom: 1px solid var(--bs-color-border-subtle); padding-bottom: 6px;">
                    <span style="color: var(--bs-color-muted);">Contact Phone:</span>
                    <span id="modalWinnerPhone">—</span>
                </div>
                <div style="margin-top: 6px;">
                    <span style="color: var(--bs-color-muted); display: block; margin-bottom: 4px; font-size: 12px; text-transform: uppercase;">
                        Verified Delivery Address:
                    </span>
                    <div id="modalWinnerAddress" style="background: var(--bs-color-ink-canvas); padding: 12px; border-radius: var(--bs-radius-sm); border: 1px solid var(--bs-color-border); color: var(--bs-color-text-secondary); font-family: ui-monospace, monospace; line-height: 1.5;">
                        —
                    </div>
                </div>
            </div>

            <div style="display: flex; justify-content: flex-end;">
                <button type="button" onclick="document.getElementById('deliveryModal').style.display='none'"
                        class="bs-button bs-button--primary bs-button--sm">
                    Close Details
                </button>
            </div>
        </div>
    </div>

    <script src="assets/js/app.js"></script>
    <script>
        function showDeliveryModal(product, price, name, email, phone, address) {
            document.getElementById('modalProductName').innerText = product;
            document.getElementById('modalFinalPrice').innerText = price;
            document.getElementById('modalWinnerName').innerText = name || 'N/A';
            document.getElementById('modalWinnerEmail').innerText = email || 'N/A';
            document.getElementById('modalWinnerPhone').innerText = phone || 'Not provided';
            document.getElementById('modalWinnerAddress').innerText = address || 'No physical address on file';
            document.getElementById('deliveryModal').style.display = 'flex';
        }

        function closeDeliveryModal(e) {
            if (e.target.id === 'deliveryModal') {
                document.getElementById('deliveryModal').style.display = 'none';
            }
        }

        document.getElementById('addProductForm')?.addEventListener('submit', function() {
            const btn = document.getElementById('submitProductBtn');
            if (btn) {
                btn.classList.add('is-loading');
                btn.disabled = true;
            }
        });
    </script>
</body>
</html>
