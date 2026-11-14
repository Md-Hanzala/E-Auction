package com.eauction.controller;

import com.eauction.dao.AuctionDAO;
import com.eauction.model.Auction;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.List;

/**
 * Consolidated Buyer Operations Controller for HASHbid.
 * Manages the buyer live floor, personal bid history, AJAX/form bid submission,
 * real-time floor updates polling, and the public homepage featured auction card.
 */
@WebServlet(urlPatterns = {
    "/buyer-dashboard",
    "/place-bid",
    "/live-auction-updates",
    "/featured-live-auction"
})
public class BuyerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final AuctionDAO auctionDAO = new AuctionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        switch (path) {
            case "/buyer-dashboard":
                showDashboard(request, response);
                break;
            case "/live-auction-updates":
                sendLiveAuctionUpdates(request, response);
                break;
            case "/featured-live-auction":
                sendFeaturedLiveAuction(request, response);
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

        if ("/place-bid".equals(path)) {
            placeBid(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Closes expired lots, queries active floor lots and the buyer's bidding history,
     * then forwards to buyer-dashboard.jsp.
     */
    private void showDashboard(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.html?error=session_expired");
            return;
        }

        int buyerId = (int) session.getAttribute("userId");

        // Auto-close any auctions whose end_time has passed
        auctionDAO.closeExpiredAuctions();

        // 1. Fetch live auctions for bidding floor
        List<Auction> activeAuctions = auctionDAO.getActiveAuctions();
        request.setAttribute("activeAuctions", activeAuctions);

        // 2. Fetch buyer's personal bid activity
        List<Auction> buyerHistory = auctionDAO.getBuyerBiddingHistory(buyerId);
        request.setAttribute("buyerHistory", buyerHistory);

        // 3. Forward to JSP view
        request.getRequestDispatcher("buyer-dashboard.jsp").forward(request, response);
    }

    /**
     * Handles live auction updates for the 3-second client polling loop.
     * Emits cache-invalidation headers and streams a JSON array of active lots.
     */
    private void sendLiveAuctionUpdates(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Disable client & proxy caching so each poll hits live Oracle state
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        List<Auction> activeAuctions = auctionDAO.getActiveAuctions();

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < activeAuctions.size(); i++) {
            Auction a = activeAuctions.get(i);
            BigDecimal minInc = (a.getMinIncrement() != null) ? a.getMinIncrement() : BigDecimal.valueOf(50);
            BigDecimal minAllowed = a.getCurrentBid().add(minInc);

            json.append("{")
                .append("\"auctionId\":").append(a.getAuctionId()).append(",")
                .append("\"currentBid\":").append(a.getCurrentBid()).append(",")
                .append("\"minAllowedBid\":").append(minAllowed).append(",")
                .append("\"status\":\"").append(a.getStatus()).append("\",")
                .append("\"endTime\":").append(a.getEndTime().getTime())
                .append("}");

            if (i < activeAuctions.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");

        PrintWriter out = response.getWriter();
        out.print(json.toString());
        out.flush();
    }

    /**
     * Public read-only endpoint for the homepage Live Lot / Featured Auction card.
     * Closes expired lots, queries the live lot ending soonest from Oracle, and returns JSON.
     */
    private void sendFeaturedLiveAuction(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        // Auto-close any expired auctions to maintain database consistency
        auctionDAO.closeExpiredAuctions();

        Auction featured = auctionDAO.getFeaturedLiveAuction();
        PrintWriter out = response.getWriter();

        if (featured == null) {
            out.print("{\"hasLiveAuction\":false}");
        } else {
            StringBuilder json = new StringBuilder("{");
            json.append("\"hasLiveAuction\":true,")
                .append("\"auctionId\":").append(featured.getAuctionId()).append(",")
                .append("\"productId\":").append(featured.getProductId()).append(",")
                .append("\"productName\":").append(escapeJson(featured.getProductName())).append(",")
                .append("\"category\":").append(escapeJson(featured.getCategory())).append(",")
                .append("\"description\":").append(escapeJson(featured.getDescription())).append(",")
                .append("\"imagePath\":").append(escapeJson(featured.getImagePath())).append(",")
                .append("\"startingPrice\":").append(featured.getStartingPrice() != null ? featured.getStartingPrice() : BigDecimal.ZERO).append(",")
                .append("\"currentBid\":").append(featured.getCurrentBid()).append(",")
                .append("\"minIncrement\":").append(featured.getMinIncrement() != null ? featured.getMinIncrement() : BigDecimal.valueOf(50)).append(",")
                .append("\"endTime\":").append(featured.getEndTime().getTime())
                .append("}");
            out.print(json.toString());
        }
        out.flush();
    }

    /**
     * Utility to safely escape strings for JSON serialization.
     */
    private String escapeJson(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\")
                           .replace("\"", "\\\"")
                           .replace("\r", "\\r")
                           .replace("\n", "\\n") + "\"";
    }

    /**
     * Processes bid submissions via in-place AJAX fetch or standard HTML form.
     * Validates BUYER role authorization, executes transactional database update,
     * and returns structured JSON or redirects.
     */
    private void placeBid(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            if (isAjax) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"Session expired. Please log in again.\"}");
            } else {
                response.sendRedirect("login.html?error=session_expired");
            }
            return;
        }

        String role = (String) session.getAttribute("userRole");
        if (!"BUYER".equalsIgnoreCase(role)) {
            if (isAjax) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized: Only registered Buyers may place bids.\"}");
            } else {
                response.sendRedirect("login.html?error=unauthorized_role");
            }
            return;
        }

        int buyerId = (int) session.getAttribute("userId");

        try {
            int auctionId = Integer.parseInt(request.getParameter("auctionId"));
            BigDecimal bidAmount = new BigDecimal(request.getParameter("bidAmount"));

            boolean success = auctionDAO.placeBid(auctionId, buyerId, bidAmount);

            if (isAjax) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                PrintWriter out = response.getWriter();
                if (success) {
                    out.write("{\"success\":true,\"message\":\"Bid placed successfully! You are currently the leading bidder.\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    out.write("{\"success\":false,\"message\":\"Bid rejected: Your bid must exceed the current leading bid, or this auction has ended.\"}");
                }
                out.flush();
            } else {
                if (success) {
                    response.sendRedirect("buyer-dashboard?bidSuccess=true");
                } else {
                    response.sendRedirect("buyer-dashboard?error=low_or_expired");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (isAjax) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"Invalid bid amount format.\"}");
            } else {
                response.sendRedirect("buyer-dashboard?error=invalid_bid_format");
            }
        }
    }
}
