package eauction;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class PlaceBidServlet extends HttpServlet {

    private static final int TRIGGER_SECONDS = 30;
    private static final int EXTENSION_SECONDS = 60;
    private static final int MAX_TOTAL_EXTENSION_SECONDS = 300;

    protected void processRequest(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null ||
                session.getAttribute("user_id") == null) {

            response.sendRedirect("login.html");
            return;
        }

        int bidderId =
                (Integer) session.getAttribute("user_id");

        String auctionId =
                request.getParameter("auction_id");

        String bidAmount =
                request.getParameter("amount");

        String url =
                "jdbc:oracle:thin:@localhost:1521:xe";

        String username = "system";
        String dbPassword = "manager";

        Connection con = null;

        try {

            Class.forName("oracle.jdbc.OracleDriver");

            con = DriverManager.getConnection(
                    url,
                    username,
                    dbPassword);

            con.setAutoCommit(false);

            String auctionQuery =
                    "SELECT current_price, "
                    + "start_time, "
                    + "end_time, "
                    + "status, "
                    + "total_extension_seconds "
                    + "FROM auctions "
                    + "WHERE auction_id = ? "
                    + "FOR UPDATE";

            PreparedStatement auctionPs =
                    con.prepareStatement(auctionQuery);

            auctionPs.setInt(
                    1,
                    Integer.parseInt(auctionId));

            ResultSet rs =
                    auctionPs.executeQuery();

            if (!rs.next()) {

                con.rollback();

                showMessage(
                        response,
                        "Bid Failed",
                        "Auction not found.");

                rs.close();
                auctionPs.close();
                con.close();

                return;
            }

            double currentPrice =
                    rs.getDouble("current_price");

            Timestamp startTime =
                    rs.getTimestamp("start_time");

            Timestamp endTime =
                    rs.getTimestamp("end_time");

            String status =
                    rs.getString("status");

            int totalExtensionSeconds =
                    rs.getInt(
                            "total_extension_seconds");

            rs.close();
            auctionPs.close();

            long currentTime =
                    System.currentTimeMillis();

            if (!"LIVE".equals(status)) {

                con.rollback();

                showMessage(
                        response,
                        "Bid Failed",
                        "Auction is not LIVE.");

                con.close();

                return;
            }

            if (currentTime >= endTime.getTime()) {

                con.rollback();

                showMessage(
                        response,
                        "Bid Failed",
                        "Auction time has ended.");

                con.close();

                return;
            }

            if (currentTime < startTime.getTime()) {

                con.rollback();

                showMessage(
                        response,
                        "Bid Failed",
                        "Auction has not started yet.");

                con.close();

                return;
            }

            double newBid =
                    Double.parseDouble(bidAmount);

            if (newBid <= currentPrice) {

                con.rollback();

                showMessage(
                        response,
                        "Bid Failed",
                        "Bid must be greater than the current price.");

                con.close();

                return;
            }

            long remainingSeconds =
                    (endTime.getTime()
                    - currentTime) / 1000;

            boolean extended = false;

            int extensionApplied = 0;

            Timestamp finalEndTime = endTime;

            if (remainingSeconds <= TRIGGER_SECONDS
                    && totalExtensionSeconds
                    < MAX_TOTAL_EXTENSION_SECONDS) {

                int remainingExtension =
                        MAX_TOTAL_EXTENSION_SECONDS
                        - totalExtensionSeconds;

                extensionApplied =
                        Math.min(
                                EXTENSION_SECONDS,
                                remainingExtension);

                finalEndTime =
                        new Timestamp(
                                endTime.getTime()
                                + extensionApplied * 1000L);

                String extensionQuery =
                        "UPDATE auctions "
                        + "SET current_price = ?, "
                        + "winner_id = ?, "
                        + "end_time = ?, "
                        + "total_extension_seconds = "
                        + "total_extension_seconds + ? "
                        + "WHERE auction_id = ?";

                PreparedStatement extensionPs =
                        con.prepareStatement(
                                extensionQuery);

                extensionPs.setDouble(
                        1,
                        newBid);

                extensionPs.setInt(
                        2,
                        bidderId);

                extensionPs.setTimestamp(
                        3,
                        finalEndTime);

                extensionPs.setInt(
                        4,
                        extensionApplied);

                extensionPs.setInt(
                        5,
                        Integer.parseInt(auctionId));

                extensionPs.executeUpdate();

                extensionPs.close();

                extended = true;

            } else {

                String updateAuction =
                        "UPDATE auctions "
                        + "SET current_price = ?, "
                        + "winner_id = ? "
                        + "WHERE auction_id = ?";

                PreparedStatement updatePs =
                        con.prepareStatement(
                                updateAuction);

                updatePs.setDouble(
                        1,
                        newBid);

                updatePs.setInt(
                        2,
                        bidderId);

                updatePs.setInt(
                        3,
                        Integer.parseInt(auctionId));

                updatePs.executeUpdate();

                updatePs.close();
            }

            String insertBid =
                    "INSERT INTO bids "
                    + "(bid_id, auction_id, bidder_id, "
                    + "amount, status) "
                    + "VALUES "
                    + "(bid_seq.NEXTVAL, ?, ?, ?, 'ACCEPTED')";

            PreparedStatement bidPs =
                    con.prepareStatement(insertBid);

            bidPs.setInt(
                    1,
                    Integer.parseInt(auctionId));

            bidPs.setInt(
                    2,
                    bidderId);

            bidPs.setDouble(
                    3,
                    newBid);

            bidPs.executeUpdate();

            bidPs.close();

            con.commit();

            /*
             * Broadcast ONLY AFTER COMMIT.
             * Oracle remains the source of truth.
             */

            String websocketMessage =
                    "{"
                    + "\"type\":\"BID_UPDATE\","
                    + "\"auction_id\":"
                    + Integer.parseInt(auctionId)
                    + ","
                    + "\"current_price\":"
                    + newBid
                    + ","
                    + "\"winner_id\":"
                    + bidderId
                    + ","
                    + "\"end_time\":\""
                    + finalEndTime
                    + "\","
                    + "\"extended\":"
                    + extended
                    + ","
                    + "\"extension_seconds\":"
                    + extensionApplied
                    + "}";

            AuctionWebSocket.broadcast(
                    websocketMessage);

            if (extended) {

                showMessage(
                        response,
                        "Bid Accepted",
                        "Your bid of "
                        + newBid
                        + " was accepted. "
                        + "Auction extended by "
                        + extensionApplied
                        + " seconds.");

            } else {

                showMessage(
                        response,
                        "Bid Accepted",
                        "Your bid of "
                        + newBid
                        + " has been accepted.");
            }

            con.close();

        } catch (Exception e) {

            try {

                if (con != null) {
                    con.rollback();
                }

            } catch (Exception ignored) {
            }

            showMessage(
                    response,
                    "Bid Error",
                    e.getMessage());

        } finally {

            try {

                if (con != null &&
                        !con.isClosed()) {

                    con.close();
                }

            } catch (Exception ignored) {
            }
        }
    }

    private void showMessage(
            HttpServletResponse response,
            String title,
            String message)
            throws IOException {

        PrintWriter out =
                response.getWriter();

        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>"
                + title
                + "</title>");
        out.println("</head>");

        out.println("<body>");

        out.println("<h1>"
                + title
                + "</h1>");

        out.println("<p>"
                + message
                + "</p>");

        out.println("<a href='auction.html'>"
                + "Back to Auction"
                + "</a>");

        out.println("<br><br>");

        out.println("<a href='index.html'>"
                + "Go to Home Page"
                + "</a>");

        out.println("</body>");
        out.println("</html>");
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        processRequest(
                request,
                response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        processRequest(
                request,
                response);
    }
}