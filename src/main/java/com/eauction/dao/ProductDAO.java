package com.eauction.dao;

import com.eauction.model.Product;
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

public class ProductDAO {

    /**
     * Inserts a new seller-submitted product into the PRODUCTS table.
     */
    public boolean addProduct(Product product) {
        String sql = "INSERT INTO products (product_id, seller_id, product_name, category, description, starting_price, image_path, status) "
                   + "VALUES (product_seq.NEXTVAL, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, product.getSellerId());
            ps.setString(2, product.getProductName());
            ps.setString(3, product.getCategory());
            ps.setString(4, product.getDescription());
            ps.setBigDecimal(5, product.getStartingPrice());
            ps.setString(6, product.getImagePath());
            ps.setString(7, product.getStatus());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Fetches all products uploaded by a specific seller.
     */
    public List<Product> getProductsBySeller(int sellerId) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT product_id, seller_id, product_name, category, description, starting_price, image_path, status, created_at "
                   + "FROM products WHERE seller_id = ? ORDER BY created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product();
                    p.setProductId(rs.getInt("product_id"));
                    p.setSellerId(rs.getInt("seller_id"));
                    p.setProductName(rs.getString("product_name"));
                    p.setCategory(rs.getString("category"));
                    p.setDescription(rs.getString("description"));
                    p.setStartingPrice(rs.getBigDecimal("starting_price"));
                    p.setImagePath(rs.getString("image_path"));
                    p.setStatus(rs.getString("status"));
                    p.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Fetches all pending products for Admin review.
     */
    public List<Product> getPendingProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.product_id, p.seller_id, p.product_name, p.category, p.description, "
                   + "       p.starting_price, p.image_path, p.status, p.created_at, u.full_name AS seller_name "
                   + "FROM products p JOIN users u ON p.seller_id = u.user_id "
                   + "WHERE p.status = 'PENDING' ORDER BY p.created_at ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Product p = new Product();
                p.setProductId(rs.getInt("product_id"));
                p.setSellerId(rs.getInt("seller_id"));
                p.setProductName(rs.getString("product_name"));
                p.setCategory(rs.getString("category"));
                p.setDescription(rs.getString("description"));
                p.setStartingPrice(rs.getBigDecimal("starting_price"));
                p.setImagePath(rs.getString("image_path"));
                p.setStatus(rs.getString("status"));
                p.setCreatedAt(rs.getTimestamp("created_at"));
                p.setSellerName(rs.getString("seller_name"));
                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Updates status to APPROVED or REJECTED by an Admin.
     */
    public boolean updateStatus(int productId, String status) {
        String sql = "UPDATE products SET status = ? WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves all products listed by a seller with active auction details
     * and winner shipping info for fulfilled lots.
     */
    public List<Map<String, Object>> getSellerInventory(int sellerId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT p.product_id, p.product_name, p.category, p.starting_price, p.status AS product_status, "
                   + "       a.auction_id, a.current_bid, a.status AS auction_status, a.end_time, "
                   + "       u.full_name AS winner_name, u.email AS winner_email, u.phone AS winner_phone, u.address AS winner_address "
                   + "FROM products p "
                   + "LEFT JOIN auctions a ON p.product_id = a.product_id "
                   + "LEFT JOIN users u ON a.highest_bidder_id = u.user_id "
                   + "WHERE p.seller_id = ? "
                   + "ORDER BY p.product_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("productId", rs.getInt("product_id"));
                    row.put("productName", rs.getString("product_name"));
                    row.put("category", rs.getString("category"));
                    row.put("startingPrice", rs.getBigDecimal("starting_price"));
                    row.put("productStatus", rs.getString("product_status"));
                    row.put("auctionId", rs.getInt("auction_id"));
                    row.put("currentBid", rs.getBigDecimal("current_bid"));
                    row.put("auctionStatus", rs.getString("auction_status"));
                    row.put("endTime", rs.getTimestamp("end_time"));
                    row.put("winnerName", rs.getString("winner_name"));
                    row.put("winnerEmail", rs.getString("winner_email"));
                    row.put("winnerPhone", rs.getString("winner_phone"));
                    row.put("winnerAddress", rs.getString("winner_address"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Approves a seller's item and schedules the auction in a single transaction.
     */
    public boolean approveAndLaunchAuction(int productId, int durationHours, BigDecimal minIncrement) {
        String fetchPriceSql = "SELECT starting_price FROM products WHERE product_id = ?";
        String updateProductSql = "UPDATE products SET status = 'APPROVED' WHERE product_id = ?";
        String insertAuctionSql = "INSERT INTO auctions ("
                                + "    auction_id, product_id, start_time, end_time, "
                                + "    current_bid, min_increment, execution_mode, status"
                                + ") VALUES ("
                                + "    auction_seq.NEXTVAL, ?, CURRENT_TIMESTAMP, "
                                + "    CURRENT_TIMESTAMP + NUMTODSINTERVAL(?, 'HOUR'), "
                                + "    ?, ?, 'AUTOMATIC', 'ACTIVE'"
                                + ")";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            BigDecimal startingPrice = null;
            try (PreparedStatement psFetch = conn.prepareStatement(fetchPriceSql)) {
                psFetch.setInt(1, productId);
                try (ResultSet rs = psFetch.executeQuery()) {
                    if (rs.next()) {
                        startingPrice = rs.getBigDecimal("starting_price");
                    }
                }
            }

            if (startingPrice == null) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement psUpdate = conn.prepareStatement(updateProductSql);
                 PreparedStatement psInsert = conn.prepareStatement(insertAuctionSql)) {

                // 1. Mark product as APPROVED
                psUpdate.setInt(1, productId);
                psUpdate.executeUpdate();

                // 2. Insert into AUCTIONS with dynamic duration and minimum increment
                psInsert.setInt(1, productId);
                psInsert.setInt(2, durationHours);
                psInsert.setBigDecimal(3, startingPrice);
                psInsert.setBigDecimal(4, minIncrement);
                psInsert.executeUpdate();

                conn.commit();
                return true;
            }
        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rbEx) {
                    rbEx.printStackTrace();
                }
            }
            ex.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }
}
