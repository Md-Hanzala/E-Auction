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
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;

public class TransactionServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        HttpSession session =
                request.getSession(false);

        if (session == null ||
                session.getAttribute("user_id") == null) {

            response.sendRedirect("login.html");
            return;
        }

        String auctionId = request.getParameter("auction_id");

        String url = "jdbc:oracle:thin:@localhost:1521:xe";
        String username = "system";
        String dbPassword = "manager";

        Connection con = null;

        try {
            Class.forName("oracle.jdbc.OracleDriver");

            con = DriverManager.getConnection(
                    url, username, dbPassword);

            con.setAutoCommit(false);

            String auctionQuery =
                    "SELECT a.product_id, a.winner_id, a.current_price, a.status, "
                    + "p.seller_id "
                    + "FROM auctions a "
                    + "JOIN products p ON a.product_id = p.product_id "
                    + "WHERE a.auction_id = ? "
                    + "FOR UPDATE";

            PreparedStatement auctionPs =
                    con.prepareStatement(auctionQuery);

            auctionPs.setInt(1, Integer.parseInt(auctionId));

            ResultSet rs = auctionPs.executeQuery();

            if (!rs.next()) {

                con.rollback();

                showMessage(
                        response,
                        "Transaction Failed",
                        "Auction not found."
                );

                rs.close();
                auctionPs.close();
                con.close();

                return;
            }

            int sellerId = rs.getInt("seller_id");
            int buyerId = rs.getInt("winner_id");
            double finalPrice = rs.getDouble("current_price");
            String status = rs.getString("status");

            rs.close();
            auctionPs.close();

            if (!"SOLD".equals(status)) {

                con.rollback();

                showMessage(
                        response,
                        "Transaction Failed",
                        "Auction is not SOLD."
                );

                con.close();

                return;
            }

            if (buyerId == 0) {

                con.rollback();

                showMessage(
                        response,
                        "Transaction Failed",
                        "No winning buyer exists."
                );

                con.close();

                return;
            }

            String settlementQuery =
                    "SELECT settlement_id "
                    + "FROM settlements "
                    + "WHERE auction_id = ? "
                    + "AND settlement_status = 'PAID' "
                    + "FOR UPDATE";

            PreparedStatement settlementPs =
                    con.prepareStatement(settlementQuery);

            settlementPs.setInt(
                    1,
                    Integer.parseInt(auctionId)
            );

            ResultSet settlementRs =
                    settlementPs.executeQuery();

            if (!settlementRs.next()) {

                settlementRs.close();
                settlementPs.close();

                con.rollback();

                showMessage(
                        response,
                        "Transaction Failed",
                        "No PAID settlement exists for this auction."
                );

                con.close();

                return;
            }

            settlementRs.close();
            settlementPs.close();

            String existingQuery =
                    "SELECT transaction_id "
                    + "FROM transactions "
                    + "WHERE auction_id = ? "
                    + "FOR UPDATE";

            PreparedStatement existingPs =
                    con.prepareStatement(existingQuery);

            existingPs.setInt(1, Integer.parseInt(auctionId));

            ResultSet existingRs =
                    existingPs.executeQuery();

            if (existingRs.next()) {

                existingRs.close();
                existingPs.close();

                con.rollback();

                showMessage(
                        response,
                        "Transaction Failed",
                        "Transaction already exists."
                );

                con.close();

                return;
            }

            existingRs.close();
            existingPs.close();

            double platformFee = Math.floor(finalPrice * 0.05);
            double sellerAmount = finalPrice - platformFee;

            String insertQuery =
                    "INSERT INTO transactions "
                    + "(transaction_id, auction_id, seller_id, buyer_id, "
                    + "final_price, platform_fee, seller_amount, status) "
                    + "VALUES (transaction_seq.NEXTVAL, ?, ?, ?, ?, ?, ?, 'COMPLETED')";

            PreparedStatement insertPs =
                    con.prepareStatement(insertQuery);

            insertPs.setInt(1, Integer.parseInt(auctionId));
            insertPs.setInt(2, sellerId);
            insertPs.setInt(3, buyerId);
            insertPs.setDouble(4, finalPrice);
            insertPs.setDouble(5, platformFee);
            insertPs.setDouble(6, sellerAmount);

            insertPs.executeUpdate();

            insertPs.close();

            con.commit();

            showMessage(
                    response,
                    "Transaction Created",
                    "Transaction created successfully. "
                    + "Final Price: " + finalPrice
                    + ", Platform Fee: " + platformFee
                    + ", Seller Amount: " + sellerAmount
            );

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
                    "Transaction Error",
                    e.getMessage()
            );

        } finally {

            try {
                if (con != null && !con.isClosed()) {
                    con.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void showMessage(
            HttpServletResponse response,
            String title,
            String message) throws IOException {

        PrintWriter out = response.getWriter();

        out.println("<html>");
        out.println("<head>");
        out.println("<title>" + title + "</title>");
        out.println("</head>");
        out.println("<body>");

        out.println("<h1>" + title + "</h1>");
        out.println("<p>" + message + "</p>");

        out.println("<a href='index.html'>Go to Home Page</a>");

        out.println("</body>");
        out.println("</html>");
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