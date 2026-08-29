package eauction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AuctionScheduler {

    private Timer timer;

    public AuctionScheduler() {

        timer = new Timer(true);

        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                updateAuctionStatus();
            }

        }, 0, 10000);
    }

    private void updateAuctionStatus() {

        String url =
                "jdbc:oracle:thin:@localhost:1521:xe";

        String username = "system";
        String dbPassword = "manager";

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

            List<String> events =
                    new ArrayList<>();


            /*
             * 1. Move SCHEDULED auctions to LIVE
             */

            String liveSelectQuery =
                    "SELECT auction_id "
                    + "FROM auctions "
                    + "WHERE status = 'SCHEDULED' "
                    + "AND start_time <= SYSTIMESTAMP "
                    + "AND end_time > SYSTIMESTAMP";

            try (
                PreparedStatement liveSelectPs =
                        con.prepareStatement(liveSelectQuery)
            ) {

                try (ResultSet liveRs =
                        liveSelectPs.executeQuery()) {

                    while (liveRs.next()) {

                        int liveAuctionId =
                                liveRs.getInt(
                                        "auction_id"
                                );

                        events.add(
                                "{\"type\":\"LIFECYCLE\","
                                + "\"auction_id\":"
                                + liveAuctionId
                                + ",\"status\":\"LIVE\"}"
                        );
                    }
                }
            }

            String liveQuery =
                    "UPDATE auctions "
                    + "SET status = 'LIVE' "
                    + "WHERE status = 'SCHEDULED' "
                    + "AND start_time <= SYSTIMESTAMP "
                    + "AND end_time > SYSTIMESTAMP";

            try (
                PreparedStatement livePs =
                        con.prepareStatement(liveQuery)
            ) {

                livePs.executeUpdate();
            }


            /*
             * 2. Find LIVE auctions whose end time has passed.
             *
             * Lock them so the status change and settlement
             * creation happen safely in the same transaction.
             */

            String endedQuery =
                    "SELECT auction_id, "
                    + "winner_id, "
                    + "current_price "
                    + "FROM auctions "
                    + "WHERE status = 'LIVE' "
                    + "AND end_time <= SYSTIMESTAMP "
                    + "FOR UPDATE";

            try (
                PreparedStatement endedPs =
                        con.prepareStatement(endedQuery);

                ResultSet rs =
                        endedPs.executeQuery()
            ) {

                while (rs.next()) {

                    int auctionId =
                            rs.getInt("auction_id");

                    int winnerId =
                            rs.getInt("winner_id");

                    double amount =
                            rs.getDouble("current_price");


                    /*
                     * 3. Mark auction SOLD when it has a winner,
                     *    otherwise mark it UNSOLD. A NULL
                     *    winner_id is read as 0 by getInt, so
                     *    winnerId <= 0 means no bids/winner.
                     */

                    String finalStatus =
                            (winnerId > 0)
                                    ? "SOLD"
                                    : "UNSOLD";

                    String soldQuery =
                            "UPDATE auctions "
                            + "SET status = ? "
                            + "WHERE auction_id = ? "
                            + "AND status = 'LIVE'";

                    try (
                        PreparedStatement soldPs =
                                con.prepareStatement(
                                        soldQuery
                                )
                    ) {

                        soldPs.setString(
                                1,
                                finalStatus
                        );

                        soldPs.setInt(
                                2,
                                auctionId
                        );

                        soldPs.executeUpdate();
                    }


                    /*
                     * 4. If there is a winner, create a
                     * settlement with PENDING status.
                     *
                     * If winner_id is 0, there is no buyer,
                     * so no settlement is created.
                     */

                    if (winnerId > 0) {

                        String existingSettlementQuery =
                                "SELECT settlement_id "
                                + "FROM settlements "
                                + "WHERE auction_id = ?";

                        boolean settlementExists =
                                false;

                        try (
                            PreparedStatement existingPs =
                                    con.prepareStatement(
                                            existingSettlementQuery
                                    )
                        ) {

                            existingPs.setInt(
                                    1,
                                    auctionId
                            );

                            try (
                                ResultSet existingRs =
                                        existingPs.executeQuery()
                            ) {

                                if (existingRs.next()) {

                                    settlementExists =
                                            true;
                                }
                            }
                        }


                        /*
                         * Create settlement only once.
                         */

                        if (!settlementExists) {

                            String insertSettlement =
                                    "INSERT INTO settlements "
                                    + "(settlement_id, auction_id, "
                                    + "buyer_id, amount, "
                                    + "settlement_status, created_at) "
                                    + "VALUES "
                                    + "(settlement_seq.NEXTVAL, ?, ?, ?, "
                                    + "'PENDING', SYSTIMESTAMP)";

                            try (
                                PreparedStatement settlementPs =
                                        con.prepareStatement(
                                                insertSettlement
                                        )
                            ) {

                                settlementPs.setInt(
                                        1,
                                        auctionId
                                );

                                settlementPs.setInt(
                                        2,
                                        winnerId
                                );

                                settlementPs.setDouble(
                                        3,
                                        amount
                                );

                                settlementPs.executeUpdate();
                            }


                            System.out.println(
                                    "Settlement created for auction "
                                    + auctionId
                                    + " | Buyer ID: "
                                    + winnerId
                                    + " | Amount: "
                                    + amount
                            );
                        }
                    } else {

                        System.out.println(
                                "Auction "
                                + auctionId
                                + " ended without a winner. "
                                + "Product remains with owner."
                        );
                    }

                    if ("SOLD".equals(finalStatus)) {

                        events.add(
                                "{\"type\":\"LIFECYCLE\","
                                + "\"auction_id\":"
                                + auctionId
                                + ",\"status\":\"SOLD\","
                                + "\"winner_id\":"
                                + winnerId
                                + ",\"current_price\":"
                                + amount
                                + "}"
                        );
                    } else {

                        events.add(
                                "{\"type\":\"LIFECYCLE\","
                                + "\"auction_id\":"
                                + auctionId
                                + ",\"status\":\"UNSOLD\"}"
                        );
                    }
                }
            }


            con.commit();

            for (String event : events) {

                AuctionWebSocket.broadcast(event);
            }


        } catch (Exception e) {

            try {

                if (con != null) {
                    con.rollback();
                }

            } catch (Exception ignored) {
            }


            System.out.println(
                    "Auction Scheduler Error: "
                    + e.getMessage()
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


    public void stop() {

        if (timer != null) {

            timer.cancel();
        }
    }
}