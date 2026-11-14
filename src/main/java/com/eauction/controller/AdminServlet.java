package com.eauction.controller;

import com.eauction.dao.AuctionDAO;
import com.eauction.dao.ProductDAO;
import com.eauction.dao.UserDAO;
import com.eauction.model.Product;
import com.eauction.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Consolidated Administration Controller for HASHbid.
 * Manages moderation queues, auction scheduling, emergency termination, and bid ledger auditing.
 */
@WebServlet(urlPatterns = {
    "/admin-dashboard",
    "/admin-actions",
    "/admin-bid-history"
})
public class AdminServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final AuctionDAO auctionDAO = new AuctionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        switch (path) {
            case "/admin-dashboard":
                showDashboard(request, response);
                break;
            case "/admin-bid-history":
                showBidHistory(request, response);
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/admin-actions".equals(path)) {
            handleAdminAction(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Closes expired lots, queries pending seller applications, unapproved products,
     * and live floor monitoring data, then forwards to admin-dashboard.jsp.
     */
    private void showDashboard(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.html?error=session_expired");
            return;
        }

        String role = (String) session.getAttribute("userRole");
        if (!"ADMIN".equalsIgnoreCase(role)) {
            response.sendRedirect("login.html?error=unauthorized");
            return;
        }

        // Auto-close any expired auctions
        auctionDAO.closeExpiredAuctions();

        // 1. Seller account requests
        List<User> pendingSellers = userDAO.getPendingSellers();
        request.setAttribute("pendingSellers", pendingSellers);

        // 2. Unapproved product inventory
        List<Product> pendingProducts = productDAO.getPendingProducts();
        request.setAttribute("pendingProducts", pendingProducts);

        // 3. Live active floor data (current bids, top bidders, timestamps)
        List<Map<String, Object>> activeAuctions = auctionDAO.getActiveAuctionsWithDetails();
        request.setAttribute("activeAuctions", activeAuctions);

        request.getRequestDispatcher("admin-dashboard.jsp").forward(request, response);
    }

    /**
     * Handles administrative moderation actions: seller approval/rejection,
     * product vetting & auction scheduling, and emergency early termination.
     */
    private void handleAdminAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !"ADMIN".equalsIgnoreCase((String) session.getAttribute("userRole"))) {
            response.sendRedirect("login.html?error=unauthorized");
            return;
        }

        String action = request.getParameter("action");

        try {
            if ("verifySeller".equalsIgnoreCase(action)) {
                int sellerId = Integer.parseInt(request.getParameter("sellerId"));
                String status = request.getParameter("status");
                userDAO.updateUserStatus(sellerId, status);
                response.sendRedirect("admin-dashboard?success=seller_status_updated");
                return;

            } else if ("approveProduct".equalsIgnoreCase(action)) {
                int productId = Integer.parseInt(request.getParameter("productId"));
                int durationHours = Integer.parseInt(request.getParameter("durationHours"));
                BigDecimal minIncrement = new BigDecimal(request.getParameter("minIncrement"));

                boolean launched = productDAO.approveAndLaunchAuction(productId, durationHours, minIncrement);
                if (launched) {
                    response.sendRedirect("admin-dashboard?success=auction_launched");
                } else {
                    response.sendRedirect("admin-dashboard?error=launch_failed");
                }
                return;

            } else if ("forceClose".equalsIgnoreCase(action)) {
                int auctionId = Integer.parseInt(request.getParameter("auctionId"));
                boolean closed = auctionDAO.forceCloseAuction(auctionId);

                if (closed) {
                    response.sendRedirect("admin-dashboard?success=auction_terminated_early");
                } else {
                    response.sendRedirect("admin-dashboard?error=termination_failed");
                }
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("admin-dashboard?error=action_exception");
            return;
        }

        response.sendRedirect("admin-dashboard");
    }

    /**
     * Queries chronological bid history across all auctions or filtered by auction ID,
     * then forwards to admin-bid-history.jsp.
     */
    private void showBidHistory(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.html?error=session_expired");
            return;
        }

        String role = (String) session.getAttribute("userRole");
        if (!"ADMIN".equalsIgnoreCase(role)) {
            response.sendRedirect("login.html?error=unauthorized");
            return;
        }

        String auctionIdParam = request.getParameter("auctionId");
        Integer auctionId = null;
        if (auctionIdParam != null && !auctionIdParam.trim().isEmpty()) {
            try {
                auctionId = Integer.parseInt(auctionIdParam);
            } catch (NumberFormatException ignored) {}
        }

        List<Map<String, Object>> bidHistory = auctionDAO.getBidHistory(auctionId);
        request.setAttribute("bidHistory", bidHistory);
        request.setAttribute("selectedAuctionId", auctionId);

        request.getRequestDispatcher("admin-bid-history.jsp").forward(request, response);
    }
}
