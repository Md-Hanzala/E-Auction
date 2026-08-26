package eauction;

import java.io.IOException;
import java.io.PrintWriter;
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

        String auctionId = request.getParameter("auction_id");

        if (auctionId == null || auctionId.trim().isEmpty()) {

            response.getWriter().write(
                    "{\"error\":\"Auction ID is required\"}"
            );

            return;
        }

        String url = "jdbc:oracle:thin:@localhost:1521:xe";
        String username = "system";
        String dbPassword = "manager";

        try {

            Class.forName("oracle.jdbc.OracleDriver");

            try (Connection con = DriverManager.getConnection(
                    url,
                    username,
                    dbPassword)) {

                String sql =
                        "SELECT auction_id, "
                        + "current_price, "
                        + "start_time, "
                        + "end_time, "
                        + "status, "
                        + "winner_id "
                        + "FROM auctions "
                        + "WHERE auction_id = ?";

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

                        double currentPrice =
                                rs.getDouble("current_price");

                        String startTime =
                                rs.getTimestamp("start_time")
                                .toString();

                        String endTime =
                                rs.getTimestamp("end_time")
                                .toString();

                        String status =
                                rs.getString("status");

                        int winnerId =
                                rs.getInt("winner_id");

                        String json =
                                "{"
                                + "\"auction_id\":" + id + ","
                                + "\"current_price\":" + currentPrice + ","
                                + "\"start_time\":\"" + startTime + "\","
                                + "\"end_time\":\"" + endTime + "\","
                                + "\"status\":\"" + status + "\","
                                + "\"winner_id\":" + winnerId
                                + "}";

                        PrintWriter out =
                                response.getWriter();

                        out.write(json);
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
                    + e.getMessage()
                    .replace("\"", "\\\"")
                    + "\"}"
            );
        }
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        processRequest(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        processRequest(request, response);
    }
}