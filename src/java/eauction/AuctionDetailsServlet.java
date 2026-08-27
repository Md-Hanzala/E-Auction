package eauction;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuctionDetailsServlet extends HttpServlet {

    protected void processRequest(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        String auctionId =
                request.getParameter("auction_id");

        if (auctionId == null ||
                auctionId.trim().isEmpty()) {

            response.getWriter().write(
                    "{\"error\":\"Auction ID is required\"}"
            );

            return;
        }

        String url =
                "jdbc:oracle:thin:@localhost:1521:xe";

        String username =
                "system";

        String dbPassword =
                "manager";

        try {

            Class.forName(
                    "oracle.jdbc.OracleDriver"
            );

            try (Connection con =
                    DriverManager.getConnection(
                            url,
                            username,
                            dbPassword)) {

                String sql =
                        "SELECT "
                        + "a.auction_id, "
                        + "a.product_id, "
                        + "a.current_price, "
                        + "a.start_time, "
                        + "a.end_time, "
                        + "a.status, "
                        + "a.winner_id, "
                        + "p.name, "
                        + "p.description, "
                        + "p.image_reference, "
                        + "p.start_price "
                        + "FROM auctions a "
                        + "JOIN products p "
                        + "ON a.product_id = p.product_id "
                        + "WHERE a.auction_id = ?";

                try (PreparedStatement ps =
                        con.prepareStatement(sql)) {

                    ps.setInt(
                            1,
                            Integer.parseInt(auctionId)
                    );

                    try (ResultSet rs =
                            ps.executeQuery()) {

                        if (!rs.next()) {

                            response.getWriter().write(
                                    "{\"error\":\"Auction not found\"}"
                            );

                            return;
                        }

                        int id =
                                rs.getInt("auction_id");

                        int productId =
                                rs.getInt("product_id");

                        double currentPrice =
                                rs.getDouble(
                                        "current_price"
                                );

                        double startPrice =
                                rs.getDouble(
                                        "start_price"
                                );

                        String startTime =
                                rs.getTimestamp(
                                        "start_time"
                                ).toString();

                        String endTime =
                                rs.getTimestamp(
                                        "end_time"
                                ).toString();

                        String status =
                                rs.getString("status");

                        int winnerId =
                                rs.getInt("winner_id");

                        String name =
                                rs.getString("name");

                        String description =
                                rs.getString(
                                        "description"
                                );

                        String imageReference =
                                rs.getString(
                                        "image_reference"
                                );

                        String json =
                                "{"
                                + "\"auction_id\":"
                                + id
                                + ","

                                + "\"product_id\":"
                                + productId
                                + ","

                                + "\"name\":\""
                                + escapeJson(name)
                                + "\","

                                + "\"description\":\""
                                + escapeJson(description)
                                + "\","

                                + "\"image_reference\":\""
                                + escapeJson(
                                        imageReference
                                )
                                + "\","

                                + "\"start_price\":"
                                + startPrice
                                + ","

                                + "\"current_price\":"
                                + currentPrice
                                + ","

                                + "\"start_time\":\""
                                + startTime
                                + "\","

                                + "\"end_time\":\""
                                + endTime
                                + "\","

                                + "\"status\":\""
                                + escapeJson(status)
                                + "\","

                                + "\"winner_id\":"
                                + winnerId

                                + "}";

                        response.getWriter()
                                .write(json);
                    }
                }
            }

        } catch (NumberFormatException e) {

            response.getWriter().write(
                    "{\"error\":\"Invalid auction ID\"}"
            );

        } catch (Exception e) {

            response.getWriter().write(
                    "{\"error\":\""
                    + escapeJson(e.getMessage())
                    + "\"}"
            );
        }
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        processRequest(
                request,
                response
        );
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        processRequest(
                request,
                response
        );
    }
}