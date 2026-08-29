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
    private static final int BIDDER_COOLDOWN_SECONDS = 3;

    protected void processRequest(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        HttpSession session =
                request.getSession(false);

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

        String idempotencyKey =
                request.getParameter("idempotency_key");

        if (auctionId == null ||
                auctionId.trim().isEmpty()) {

            showMessage(
                    response,
                    "Bid Failed",
                    "Auction ID is required."
            );

            return;
        }

        if (bidAmount == null ||
                bidAmount.trim().isEmpty()) {

            showMessage(
                    response,
                    "Bid Failed",
                    "Bid amount is required."
            );

            return;
        }

        if (idempotencyKey == null ||
                idempotencyKey.trim().isEmpty()) {

            showMessage(
                    response,
                    "Bid Failed",
                    "Idempotency key is required."
            );

            return;
        }

        String url =
                "jdbc:oracle:thin:@localhost:1521:xe";

        String username =
                "system";

        String dbPassword =
                "manager";

        Connection con = null;

        try {

            Class.forName(
                    "oracle.jdbc.OracleDriver"
            );

            con =
                    DriverManager.getConnection(
                            url,
                            username,
                            dbPassword
                    );

            con.setAutoCommit(false);


            int auctionIdValue =
                    Integer.parseInt(auctionId);

            double newBid =
                    Double.parseDouble(bidAmount);


            if (newBid <= 0) {

                con.rollback();

                showMessage(
                        response,
                        "Bid Failed",
                        "Bid amount must be greater than zero."
                );

                return;
            }


            String auctionQuery =
                    "SELECT current_price, "
                    + "start_time, "
                    + "end_time, "
                    + "status, "
                    + "total_extension_seconds, "
                    + "SYSTIMESTAMP AS db_now "
                    + "FROM auctions "
                    + "WHERE auction_id = ? "
                    + "FOR UPDATE";


            PreparedStatement auctionPs =
                    con.prepareStatement(
                            auctionQuery
                    );


            auctionPs.setInt(
                    1,
                    auctionIdValue
            );


            ResultSet rs =
                    auctionPs.executeQuery();


            if (!rs.next()) {

                con.rollback();

                showMessage(
                        response,
                        "Bid Failed",
                        "Auction not found."
                );

                rs.close();
                auctionPs.close();

                return;
            }


            double currentPrice =
                    rs.getDouble(
                            "current_price"
                    );


            Timestamp startTime =
                    rs.getTimestamp(
                            "start_time"
                    );


            Timestamp endTime =
                    rs.getTimestamp(
                            "end_time"
                    );


            String status =
                    rs.getString(
                            "status"
                    );


            int totalExtensionSeconds =
                    rs.getInt(
                            "total_extension_seconds"
                    );


            Timestamp dbNow =
                    rs.getTimestamp(
                            "db_now"
                    );


            rs.close();
            auctionPs.close();


            /*
             * Idempotency check.
             *
             * Runs inside the held auction FOR UPDATE
             * lock, before any auction UPDATE caused by
             * the bid. A repeated request carrying the
             * same idempotency_key for the same auction
             * and bidder is treated as a retry of an
             * already-accepted bid.
             */

            String dupBidQuery =
                    "SELECT bid_id "
                    + "FROM bids "
                    + "WHERE auction_id = ? "
                    + "AND bidder_id = ? "
                    + "AND idempotency_key = ?";

            PreparedStatement dupBidPs =
                    con.prepareStatement(
                            dupBidQuery
                    );

            dupBidPs.setInt(
                    1,
                    auctionIdValue
            );

            dupBidPs.setInt(
                    2,
                    bidderId
            );

            dupBidPs.setString(
                    3,
                    idempotencyKey
            );

            ResultSet dupBidRs =
                    dupBidPs.executeQuery();

            if (dupBidRs.next()) {

                dupBidRs.close();
                dupBidPs.close();

                con.rollback();

                showMessage(
                        response,
                        "Bid Accepted",
                        "Your bid was already accepted."
                );

                return;
            }

            dupBidRs.close();
            dupBidPs.close();


            long currentTime =
                    dbNow.getTime();


            if (!"LIVE".equals(status)) {

                con.rollback();

                showMessage(
                        response,
                        "Bid Failed",
                        "Auction is not LIVE."
                );

                return;
            }


            if (currentTime >=
                    endTime.getTime()) {

                con.rollback();

                showMessage(
                        response,
                        "Bid Failed",
                        "Auction time has ended."
                );

                return;
            }


            if (currentTime <
                    startTime.getTime()) {

                con.rollback();

                showMessage(
                        response,
                        "Bid Failed",
                        "Auction has not started yet."
                );

                return;
            }


            if (newBid <= currentPrice) {

                con.rollback();

                showMessage(
                        response,
                        "Bid Failed",
                        "Bid must be greater than the current price."
                );

                return;
            }


            /*
             * 3-second bidder cooldown.
             *
             * Only the latest accepted bid in this
             * auction is considered.
             *
             * If the latest accepted bid belongs
             * to another bidder, this bidder can
             * bid immediately.
             *
             * If the latest accepted bid belongs
             * to this bidder, this bidder must wait
             * three seconds.
             *
             * This query is compatible with Oracle 10g.
             */

            String latestBidQuery =
                    "SELECT bidder_id, bid_time "
                    + "FROM ("
                    + "SELECT bidder_id, bid_time "
                    + "FROM bids "
                    + "WHERE auction_id = ? "
                    + "AND status = 'ACCEPTED' "
                    + "ORDER BY bid_time DESC"
                    + ") "
                    + "WHERE ROWNUM = 1";


            PreparedStatement latestBidPs =
                    con.prepareStatement(
                            latestBidQuery
                    );


            latestBidPs.setInt(
                    1,
                    auctionIdValue
            );


            ResultSet latestBidRs =
                    latestBidPs.executeQuery();


            if (latestBidRs.next()) {

                int lastBidderId =
                        latestBidRs.getInt(
                                "bidder_id"
                        );


                Timestamp lastBidTime =
                        latestBidRs.getTimestamp(
                                "bid_time"
                        );


                if (lastBidderId == bidderId &&
                        lastBidTime != null) {

                    long millisecondsSinceLastBid =
                            currentTime
                            - lastBidTime.getTime();


                    if (millisecondsSinceLastBid <
                            BIDDER_COOLDOWN_SECONDS * 1000L) {

                        long waitMilliseconds =
                                (
                                    BIDDER_COOLDOWN_SECONDS
                                    * 1000L
                                )
                                - millisecondsSinceLastBid;


                        long waitSeconds =
                                (
                                    waitMilliseconds
                                    + 999L
                                ) / 1000L;


                        latestBidRs.close();
                        latestBidPs.close();

                        con.rollback();

                        showMessage(
                                response,
                                "Bid Failed",
                                "Please wait "
                                + waitSeconds
                                + " second(s) before bidding again."
                        );

                        return;
                    }
                }
            }


            latestBidRs.close();
            latestBidPs.close();


            long remainingSeconds =
                    (
                        endTime.getTime()
                        - currentTime
                    ) / 1000L;


            boolean extended =
                    false;


            int extensionApplied =
                    0;


            Timestamp finalEndTime =
                    endTime;


            if (
                    remainingSeconds <=
                    TRIGGER_SECONDS
                    &&
                    totalExtensionSeconds <
                    MAX_TOTAL_EXTENSION_SECONDS
            ) {

                int remainingExtension =
                        MAX_TOTAL_EXTENSION_SECONDS
                        - totalExtensionSeconds;


                extensionApplied =
                        Math.min(
                                EXTENSION_SECONDS,
                                remainingExtension
                        );


                finalEndTime =
                        new Timestamp(
                                endTime.getTime()
                                + (
                                    extensionApplied
                                    * 1000L
                                )
                        );


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
                                extensionQuery
                        );


                extensionPs.setDouble(
                        1,
                        newBid
                );


                extensionPs.setInt(
                        2,
                        bidderId
                );


                extensionPs.setTimestamp(
                        3,
                        finalEndTime
                );


                extensionPs.setInt(
                        4,
                        extensionApplied
                );


                extensionPs.setInt(
                        5,
                        auctionIdValue
                );


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
                                updateAuction
                        );


                updatePs.setDouble(
                        1,
                        newBid
                );


                updatePs.setInt(
                        2,
                        bidderId
                );


                updatePs.setInt(
                        3,
                        auctionIdValue
                );


                updatePs.executeUpdate();

                updatePs.close();
            }


            String insertBid =
                    "INSERT INTO bids "
                    + "(bid_id, auction_id, bidder_id, "
                    + "amount, bid_time, status, "
                    + "idempotency_key) "
                    + "VALUES "
                    + "(bid_seq.NEXTVAL, ?, ?, ?, "
                    + "SYSTIMESTAMP, 'ACCEPTED', ?)";


            PreparedStatement bidPs =
                    con.prepareStatement(
                            insertBid
                    );


            bidPs.setInt(
                    1,
                    auctionIdValue
            );


            bidPs.setInt(
                    2,
                    bidderId
            );


            bidPs.setDouble(
                    3,
                    newBid
            );


            bidPs.setString(
                    4,
                    idempotencyKey
            );


            bidPs.executeUpdate();

            bidPs.close();


            con.commit();


            /*
             * Broadcast only after successful commit.
             */

            String websocketMessage =
                    "{"
                    + "\"type\":\"BID_UPDATE\","
                    + "\"auction_id\":"
                    + auctionIdValue
                    + ","
                    + "\"current_price\":"
                    + newBid
                    + ","
                    + "\"winner_id\":"
                    + bidderId
                    + ","
                    + "\"end_time\":\""
                    + finalEndTime.toInstant().toString()
                    + "\","
                    + "\"extended\":"
                    + extended
                    + ","
                    + "\"extension_seconds\":"
                    + extensionApplied
                    + "}";


            AuctionWebSocket.broadcast(
                    websocketMessage
            );


            if (extended) {

                showMessage(
                        response,
                        "Bid Accepted",
                        "Your bid of "
                        + newBid
                        + " was accepted. "
                        + "Auction extended by "
                        + extensionApplied
                        + " seconds."
                );


            } else {

                showMessage(
                        response,
                        "Bid Accepted",
                        "Your bid of "
                        + newBid
                        + " has been accepted."
                );
            }


        } catch (NumberFormatException e) {

            try {

                if (con != null) {
                    con.rollback();
                }

            } catch (Exception ignored) {
            }


            showMessage(
                    response,
                    "Bid Failed",
                    "Invalid auction ID or bid amount."
            );


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
                    e.getMessage()
            );


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

        out.println(
                "<title>"
                + title
                + "</title>"
        );

        out.println("</head>");

        out.println("<body>");

        out.println(
                "<h1>"
                + title
                + "</h1>"
        );

        out.println(
                "<p>"
                + message
                + "</p>"
        );

        out.println(
                "<a href='auction.html'>"
                + "Back to Auction"
                + "</a>"
        );

        out.println("<br><br>");

        out.println(
                "<a href='index.html'>"
                + "Go to Home Page"
                + "</a>"
        );

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