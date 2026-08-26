package eauction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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

        String url = "jdbc:oracle:thin:@localhost:1521:xe";
        String username = "system";
        String dbPassword = "manager";

        try {

            Class.forName("oracle.jdbc.OracleDriver");

            Connection con = DriverManager.getConnection(
                    url, username, dbPassword);

            String liveQuery =
                    "UPDATE auctions "
                    + "SET status = 'LIVE' "
                    + "WHERE status = 'SCHEDULED' "
                    + "AND start_time <= SYSTIMESTAMP "
                    + "AND end_time > SYSTIMESTAMP";

            PreparedStatement livePs =
                    con.prepareStatement(liveQuery);

            livePs.executeUpdate();

            livePs.close();

            String endedQuery =
                    "UPDATE auctions "
                    + "SET status = 'ENDED' "
                    + "WHERE status = 'LIVE' "
                    + "AND end_time <= SYSTIMESTAMP";

            PreparedStatement endedPs =
                    con.prepareStatement(endedQuery);

            endedPs.executeUpdate();

            endedPs.close();

            String soldQuery =
                    "UPDATE auctions a "
                    + "SET status = 'SOLD' "
                    + "WHERE a.status = 'ENDED' "
                    + "AND EXISTS ("
                    + "SELECT 1 FROM bids b "
                    + "WHERE b.auction_id = a.auction_id "
                    + "AND b.status = 'ACCEPTED'"
                    + ")";

            PreparedStatement soldPs =
                    con.prepareStatement(soldQuery);

            soldPs.executeUpdate();

            soldPs.close();

            String unsoldQuery =
                    "UPDATE auctions a "
                    + "SET status = 'UNSOLD' "
                    + "WHERE a.status = 'ENDED' "
                    + "AND NOT EXISTS ("
                    + "SELECT 1 FROM bids b "
                    + "WHERE b.auction_id = a.auction_id "
                    + "AND b.status = 'ACCEPTED'"
                    + ")";

            PreparedStatement unsoldPs =
                    con.prepareStatement(unsoldQuery);

            unsoldPs.executeUpdate();

            unsoldPs.close();

            con.close();

        } catch (Exception e) {

            System.out.println(
                    "Auction Scheduler Error: "
                    + e.getMessage());
        }
    }

    public void stop() {

        if (timer != null) {
            timer.cancel();
        }
    }
}