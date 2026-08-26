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

public class PaymentServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        String settlementId = request.getParameter("settlement_id");

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
                    "SELECT settlement_status, amount, buyer_id "
                    + "FROM settlements "
                    + "WHERE settlement_id = ? "
                    + "FOR UPDATE";

            PreparedStatement checkPs =
                    con.prepareStatement(checkQuery);

            checkPs.setInt(1, Integer.parseInt(settlementId));

            ResultSet rs = checkPs.executeQuery();

            if (!rs.next()) {

                con.rollback();

                showMessage(response,
                        "Payment Failed",
                        "Settlement not found.");

                rs.close();
                checkPs.close();
                con.close();

                return;
            }

            String status = rs.getString("settlement_status");
            double amount = rs.getDouble("amount");
            int buyerId = rs.getInt("buyer_id");

            rs.close();
            checkPs.close();

            if (!"PENDING".equals(status)) {

                con.rollback();

                showMessage(response,
                        "Payment Failed",
                        "Settlement is not in PENDING status.");

                con.close();

                return;
            }

            String updateQuery =
                    "UPDATE settlements "
                    + "SET settlement_status = 'PAID' "
                    + "WHERE settlement_id = ?";

            PreparedStatement updatePs =
                    con.prepareStatement(updateQuery);

            updatePs.setInt(1, Integer.parseInt(settlementId));

            updatePs.executeUpdate();

            updatePs.close();

            con.commit();

            showMessage(response,
                    "Payment Successful",
                    "Settlement ID: " + settlementId
                    + ", Buyer ID: " + buyerId
                    + ", Amount: " + amount
                    + ", Status: PAID");

            con.close();

        } catch (Exception e) {

            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (Exception ignored) {
            }

            showMessage(response,
                    "Payment Error",
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