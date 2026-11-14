package com.eauction.dao;

import com.eauction.model.Auction;
import com.eauction.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionDAO {

    /**
     * Inserts a lot into AUCTIONS matching schema constraints.
     */
    public boolean createAuction(int productId, BigDecimal startingPrice, int durationHours) {
        String sql = "INSERT INTO auctions (auction_id, product_id, start_time, end_time, current_bid, min_increment, execution_mode, status) "
                   + "VALUES (auction_seq.NEXTVAL, ?, CURRENT_TIMESTAMP, NUMTODSINTERVAL(?, 'HOUR') + CURRENT_TIMESTAMP, ?, 50.00, 'AUTOMATIC', 'ACTIVE')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);
            ps.setInt(2, durationHours);
            ps.setBigDecimal(3, startingPrice);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves active auctions with product info including description and starting price.
     * Qualifies lots where STATUS = 'ACTIVE', START_TIME <= CURRENT_TIMESTAMP, and END_TIME > CURRENT_TIMESTAMP.
     * Ordered by END_TIME ASC so the lot ending soonest appears first.
     */
    public List<Auction> getActiveAuctions() {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.auction_id, a.product_id, a.start_time, a.end_time, a.current_bid, "
                   + "       a.min_increment, a.execution_mode, a.status, "
                   + "       p.product_name, p.category, p.description, p.image_path, p.starting_price "
                   + "FROM auctions a JOIN products p ON a.product_id = p.product_id "
                   + "WHERE a.status = 'ACTIVE' "
                   + "  AND a.start_time <= CURRENT_TIMESTAMP "
                   + "  AND a.end_time > CURRENT_TIMESTAMP "
                   + "ORDER BY a.end_time ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Auction a = new Auction();
                a.setAuctionId(rs.getInt("auction_id"));
                a.setProductId(rs.getInt("product_id"));
                a.setStartTime(rs.getTimestamp("start_time"));
                a.setEndTime(rs.getTimestamp("end_time"));
                a.setCurrentBid(rs.getBigDecimal("current_bid"));
                a.setMinIncrement(rs.getBigDecimal("min_increment"));
                a.setExecutionMode(rs.getString("execution_mode"));
                a.setStatus(rs.getString("status"));
                a.setProductName(rs.getString("product_name"));
                a.setCategory(rs.getString("category"));
                a.setDescription(rs.getString("description"));
                a.setImagePath(rs.getString("image_path"));
                a.setStartingPrice(rs.getBigDecimal("starting_price"));
                list.add(a);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves the single featured live auction ending soonest, or null if no active lot exists.
     */
    public Auction getFeaturedLiveAuction() {
        List<Auction> active = getActiveAuctions();
        return active.isEmpty() ? null : active.get(0);
    }

    /**
     * Atomically updates the auction leader and logs the bid entry into BIDS.
     */
    public boolean placeBid(int auctionId, int buyerId, BigDecimal bidAmount) {
        String updateAuctionSql = "UPDATE auctions "
                                + "SET current_bid = ?, highest_bidder_id = ? "
                                + "WHERE auction_id = ? "
                                + "  AND status = 'ACTIVE' "
                                + "  AND end_time > CURRENT_TIMESTAMP "
                                + "  AND ? > current_bid";

        String insertBidSql = "INSERT INTO bids (bid_id, auction_id, bidder_id, bid_amount, bid_time) "
                            + "VALUES (bid_seq.NEXTVAL, ?, ?, ?, CURRENT_TIMESTAMP)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Update the leading bid in AUCTIONS
            try (PreparedStatement psUpdate = conn.prepareStatement(updateAuctionSql)) {
                psUpdate.setBigDecimal(1, bidAmount);
                psUpdate.setInt(2, buyerId);
                psUpdate.setInt(3, auctionId);
                psUpdate.setBigDecimal(4, bidAmount);

                int rowsAffected = psUpdate.executeUpdate();
                if (rowsAffected == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // 2. Insert audit log record into BIDS
            try (PreparedStatement psInsert = conn.prepareStatement(insertBidSql)) {
                psInsert.setInt(1, auctionId);
                psInsert.setInt(2, buyerId);
                psInsert.setBigDecimal(3, bidAmount);
                psInsert.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {}
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    /**
     * Fetches lots where the buyer is currently the leading or winning bidder,
     * including seller details for invoicing won items.
     */
    public List<Auction> getBuyerBiddingHistory(int buyerId) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.auction_id, a.product_id, a.current_bid, a.status, a.end_time, "
                   + "       p.product_name, p.category, "
                   + "       s.full_name AS seller_name, s.email AS seller_email, s.phone AS seller_phone "
                   + "FROM auctions a "
                   + "JOIN products p ON a.product_id = p.product_id "
                   + "JOIN users s ON p.seller_id = s.user_id "
                   + "WHERE a.highest_bidder_id = ? "
                   + "ORDER BY a.auction_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Auction a = new Auction();
                    a.setAuctionId(rs.getInt("auction_id"));
                    a.setProductId(rs.getInt("product_id"));
                    a.setCurrentBid(rs.getBigDecimal("current_bid"));
                    a.setStatus(rs.getString("status"));
                    a.setEndTime(rs.getTimestamp("end_time"));
                    a.setProductName(rs.getString("product_name"));
                    a.setCategory(rs.getString("category"));
                    a.setSellerName(rs.getString("seller_name"));
                    a.setSellerEmail(rs.getString("seller_email"));
                    a.setSellerPhone(rs.getString("seller_phone"));
                    list.add(a);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Closes auctions past their end time and syncs product statuses.
     */
    public void closeExpiredAuctions() {
        String closeAuctionsSql = "UPDATE auctions "
                                + "SET status = 'CLOSED' "
                                + "WHERE status = 'ACTIVE' AND end_time <= CURRENT_TIMESTAMP";

        String updateProductsSql = "UPDATE products p "
                                 + "SET status = CASE "
                                 + "    WHEN (SELECT MAX(highest_bidder_id) FROM auctions WHERE product_id = p.product_id AND status = 'CLOSED') IS NOT NULL THEN 'SOLD' "
                                 + "    ELSE 'EXPIRED' "
                                 + "END "
                                 + "WHERE p.product_id IN ("
                                 + "    SELECT product_id FROM auctions WHERE status = 'CLOSED'"
                                 + ") AND p.status = 'APPROVED'";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps1 = conn.prepareStatement(closeAuctionsSql)) {
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(updateProductsSql)) {
                ps2.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Admin query: Fetches all currently active auctions with live bid, product info, and description.
     */
    public List<Map<String, Object>> getActiveAuctionsWithDetails() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT a.auction_id, a.product_id, a.current_bid, a.min_increment, "
                   + "       a.highest_bidder_id, a.end_time, a.status, "
                   + "       p.product_name, p.category, p.description, p.starting_price, "
                   + "       u.full_name AS highest_bidder_name "
                   + "FROM auctions a "
                   + "JOIN products p ON a.product_id = p.product_id "
                   + "LEFT JOIN users u ON a.highest_bidder_id = u.user_id "
                   + "WHERE a.status = 'ACTIVE' "
                   + "ORDER BY a.end_time ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("auctionId", rs.getInt("auction_id"));
                row.put("productId", rs.getInt("product_id"));
                row.put("productName", rs.getString("product_name"));
                row.put("category", rs.getString("category"));
                row.put("description", rs.getString("description"));
                row.put("startingPrice", rs.getBigDecimal("starting_price"));
                row.put("currentBid", rs.getBigDecimal("current_bid"));
                row.put("highestBidderId", rs.getInt("highest_bidder_id"));
                row.put("highestBidderName", rs.getString("highest_bidder_name"));
                row.put("endTime", rs.getTimestamp("end_time"));
                list.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Admin Action: Forces an early close on an active auction and settles the item.
     */
    public boolean forceCloseAuction(int auctionId) {
        String closeAuctionSql = "UPDATE auctions "
                               + "SET status = 'CLOSED', end_time = CURRENT_TIMESTAMP "
                               + "WHERE auction_id = ? AND status = 'ACTIVE'";

        String updateProductSql = "UPDATE products "
                                + "SET status = CASE "
                                + "    WHEN (SELECT MAX(highest_bidder_id) FROM auctions WHERE auction_id = ?) IS NOT NULL THEN 'SOLD' "
                                + "    ELSE 'EXPIRED' "
                                + "END "
                                + "WHERE product_id = (SELECT product_id FROM auctions WHERE auction_id = ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(closeAuctionSql);
                 PreparedStatement ps2 = conn.prepareStatement(updateProductSql)) {

                ps1.setInt(1, auctionId);
                int rows1 = ps1.executeUpdate();

                if (rows1 > 0) {
                    ps2.setInt(1, auctionId);
                    ps2.setInt(2, auctionId);
                    ps2.executeUpdate();

                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            } catch (SQLException ex) {
                if (conn != null) conn.rollback();
                ex.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {}
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * Admin Query: Retrieves the chronological bid log across all lots or filtered by auctionId.
     */
    public List<Map<String, Object>> getBidHistory(Integer auctionId) {
        List<Map<String, Object>> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT b.bid_id, b.auction_id, b.bid_amount, b.bid_time, ")
           .append("       p.product_name, p.category, ")
           .append("       u.full_name AS bidder_name, u.email AS bidder_email, ")
           .append("       a.status AS auction_status ")
           .append("FROM bids b ")
           .append("JOIN auctions a ON b.auction_id = a.auction_id ")
           .append("JOIN products p ON a.product_id = p.product_id ")
           .append("JOIN users u ON b.bidder_id = u.user_id ");

        if (auctionId != null) {
            sql.append("WHERE b.auction_id = ? ");
        }
        sql.append("ORDER BY b.bid_time DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (auctionId != null) {
                ps.setInt(1, auctionId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("bidId", rs.getInt("bid_id"));
                    row.put("auctionId", rs.getInt("auction_id"));
                    row.put("productName", rs.getString("product_name"));
                    row.put("category", rs.getString("category"));
                    row.put("bidAmount", rs.getBigDecimal("bid_amount"));
                    row.put("bidTime", rs.getTimestamp("bid_time"));
                    row.put("bidderName", rs.getString("bidder_name"));
                    row.put("bidderEmail", rs.getString("bidder_email"));
                    row.put("auctionStatus", rs.getString("auction_status"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
