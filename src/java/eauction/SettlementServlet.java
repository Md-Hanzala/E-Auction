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

public class SettlementServlet extends HttpServlet {

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

            String checkQuery =
                    "SELECT winner_id, current_price, status "
                    + "FROM auctions "
                    + "WHERE auction_id = ? "
                    + "FOR UPDATE";

            PreparedStatement checkPs =
                    con.prepareStatement(checkQuery);

            checkPs.setInt(1, Integer.parseInt(auctionId));

            ResultSet rs = checkPs.executeQuery();

            if (!rs.next()) {

                con.rollback();

                showMessage(response,
                        "Settlement Failed",
                        "Auction not found.");

                rs.close();
                checkPs.close();
                con.close();

                return;
            }

            int winnerId = rs.getInt("winner_id");
            double amount = rs.getDouble("current_price");
            String status = rs.getString("status");

            rs.close();
            checkPs.close();

            if (!"SOLD".equals(status)) {

                con.rollback();

                showMessage(response,
                        "Settlement Failed",
                        "Auction is not SOLD.");

                con.close();

                return;
            }

            if (winnerId == 0) {

                con.rollback();

                showMessage(response,
                        "Settlement Failed",
                        "No winner exists for this auction.");

                con.close();

                return;
            }

            String existingQuery =
                    "SELECT settlement_id "
                    + "FROM settlements "
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

                showMessage(response,
                        "Settlement Failed",
                        "Settlement already exists for this auction.");

                con.close();

                return;
            }

            existingRs.close();
            existingPs.close();

            String insertQuery =
                    "INSERT INTO settlements "
                    + "(settlement_id, auction_id, buyer_id, amount, settlement_status) "
                    + "VALUES (settlement_seq.NEXTVAL, ?, ?, ?, 'PENDING')";

            PreparedStatement insertPs =
                    con.prepareStatement(insertQuery);

            insertPs.setInt(1, Integer.parseInt(auctionId));
            insertPs.setInt(2, winnerId);
            insertPs.setDouble(3, amount);

            insertPs.executeUpdate();

            insertPs.close();

            con.commit();

            showMessage(response,
                    "Settlement Created",
                    "Settlement created successfully. "
                    + "Buyer ID: " + winnerId
                    + ", Amount: " + amount
                    + ", Status: PENDING");

            con.close();

        } catch (Exception e) {

            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (Exception ignored) {
            }

            showMessage(response,
                    "Settlement Error",
                    e.getMessage());

        } finally {

            try {
                if (con != null && !con.isClosed()) {
                    con.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void showMessage(HttpServletResponse response,
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
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        processRequest(request, response);
    }
}