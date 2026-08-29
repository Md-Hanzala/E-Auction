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

public class CertificateServlet extends HttpServlet {

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

        String transactionId = request.getParameter("transaction_id");

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
                    "SELECT transaction_id, auction_id, buyer_id, "
                    + "final_price, status "
                    + "FROM transactions "
                    + "WHERE transaction_id = ? "
                    + "FOR UPDATE";

            PreparedStatement checkPs =
                    con.prepareStatement(checkQuery);

            checkPs.setInt(1, Integer.parseInt(transactionId));

            ResultSet rs = checkPs.executeQuery();

            if (!rs.next()) {

                con.rollback();

                showMessage(
                        response,
                        "Certificate Failed",
                        "Transaction not found."
                );

                rs.close();
                checkPs.close();
                con.close();

                return;
            }

            int auctionId = rs.getInt("auction_id");
            int buyerId = rs.getInt("buyer_id");
            double finalPrice = rs.getDouble("final_price");
            String status = rs.getString("status");

            rs.close();
            checkPs.close();

            if (!"COMPLETED".equals(status)) {

                con.rollback();

                showMessage(
                        response,
                        "Certificate Failed",
                        "Transaction is not COMPLETED."
                );

                con.close();

                return;
            }

            String existingQuery =
                    "SELECT certificate_id, certificate_number "
                    + "FROM certificates "
                    + "WHERE transaction_id = ? "
                    + "FOR UPDATE";

            PreparedStatement existingPs =
                    con.prepareStatement(existingQuery);

            existingPs.setInt(1, Integer.parseInt(transactionId));

            ResultSet existingRs =
                    existingPs.executeQuery();

            if (existingRs.next()) {

                int certificateId =
                        existingRs.getInt("certificate_id");

                String certificateNumber =
                        existingRs.getString("certificate_number");

                existingRs.close();
                existingPs.close();

                con.rollback();

                showMessage(
                        response,
                        "Certificate Already Exists",
                        "Certificate ID: " + certificateId
                        + ", Certificate Number: " + certificateNumber
                );

                con.close();

                return;
            }

            existingRs.close();
            existingPs.close();

            String certificateNumber =
                    "EA-CERT-" + transactionId + "-"
                    + System.currentTimeMillis();

            String insertQuery =
                    "INSERT INTO certificates "
                    + "(certificate_id, transaction_id, "
                    + "certificate_number, status) "
                    + "VALUES (certificate_seq.NEXTVAL, ?, ?, 'ISSUED')";

            PreparedStatement insertPs =
                    con.prepareStatement(insertQuery);

            insertPs.setInt(
                    1,
                    Integer.parseInt(transactionId)
            );

            insertPs.setString(
                    2,
                    certificateNumber
            );

            insertPs.executeUpdate();

            insertPs.close();

            String updateAuctionQuery =
                    "UPDATE auctions "
                    + "SET status = 'SETTLED' "
                    + "WHERE auction_id = ? "
                    + "AND status = 'SOLD'";

            PreparedStatement updateAuctionPs =
                    con.prepareStatement(updateAuctionQuery);

            updateAuctionPs.setInt(1, auctionId);

            int updatedRows =
                    updateAuctionPs.executeUpdate();

            updateAuctionPs.close();

            if (updatedRows == 0) {

                con.rollback();

                showMessage(
                        response,
                        "Certificate Failed",
                        "Auction could not be settled."
                );

                con.close();

                return;
            }

            con.commit();

            showCertificate(
                    response,
                    transactionId,
                    auctionId,
                    buyerId,
                    finalPrice,
                    certificateNumber
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
                    "Certificate Error",
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

    private void showCertificate(
            HttpServletResponse response,
            String transactionId,
            int auctionId,
            int buyerId,
            double finalPrice,
            String certificateNumber)
            throws IOException {

        PrintWriter out = response.getWriter();

        out.println("<html>");
        out.println("<head>");
        out.println("<title>E-Auction Certificate</title>");
        out.println("</head>");
        out.println("<body>");

        out.println("<h1>E-Auction Certificate</h1>");

        out.println("<h2>Certificate of Purchase</h2>");

        out.println("<p><strong>Certificate Number:</strong> "
                + certificateNumber + "</p>");

        out.println("<p><strong>Transaction ID:</strong> "
                + transactionId + "</p>");

        out.println("<p><strong>Auction ID:</strong> "
                + auctionId + "</p>");

        out.println("<p><strong>Buyer ID:</strong> "
                + buyerId + "</p>");

        out.println("<p><strong>Final Price:</strong> "
                + finalPrice + "</p>");

        out.println("<p><strong>Status:</strong> ISSUED</p>");

        out.println("<br>");

        out.println("<a href='index.html'>Go to Home Page</a>");

        out.println("</body>");
        out.println("</html>");
    }

    private void showMessage(
            HttpServletResponse response,
            String title,
            String message)
            throws IOException {

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