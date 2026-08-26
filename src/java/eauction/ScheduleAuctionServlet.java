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

public class ScheduleAuctionServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        String productId = request.getParameter("product_id");
        String startTime = request.getParameter("start_time");
        String endTime = request.getParameter("end_time");
        String schedulingMode = request.getParameter("scheduling_mode");

        String url = "jdbc:oracle:thin:@localhost:1521:xe";
        String username = "system";
        String dbPassword = "manager";

        String productQuery = "SELECT start_price FROM products "
                + "WHERE product_id = ? AND status = 'APPROVED'";

        String auctionQuery = "INSERT INTO auctions "
                + "(auction_id, product_id, start_time, end_time, "
                + "current_price, status, scheduling_mode, total_extension_seconds) "
                + "VALUES (auction_seq.NEXTVAL, ?, ?, ?, ?, 'SCHEDULED', ?, 0)";

        try {
            Class.forName("oracle.jdbc.OracleDriver");

            Connection con = DriverManager.getConnection(
                    url, username, dbPassword);

            PreparedStatement productPs = con.prepareStatement(productQuery);

            productPs.setInt(1, Integer.parseInt(productId));

            ResultSet rs = productPs.executeQuery();

            if (!rs.next()) {

                PrintWriter out = response.getWriter();

                out.println("<html>");
                out.println("<body>");
                out.println("<h1>Scheduling Failed</h1>");
                out.println("<p>Product does not exist or is not APPROVED.</p>");
                out.println("<a href='schedule-auction.html'>Try Again</a>");
                out.println("</body>");
                out.println("</html>");

                rs.close();
                productPs.close();
                con.close();

                return;
            }

            double startPrice = rs.getDouble("start_price");

            rs.close();
            productPs.close();

            PreparedStatement auctionPs = con.prepareStatement(auctionQuery);

            auctionPs.setInt(1, Integer.parseInt(productId));
            auctionPs.setTimestamp(2, Timestamp.valueOf(startTime.replace("T", " ") + ":00"));
            auctionPs.setTimestamp(3, Timestamp.valueOf(endTime.replace("T", " ") + ":00"));
            auctionPs.setDouble(4, startPrice);
            auctionPs.setString(5, schedulingMode);

            auctionPs.executeUpdate();

            auctionPs.close();
            con.close();

            PrintWriter out = response.getWriter();

            out.println("<html>");
            out.println("<head>");
            out.println("<title>Auction Scheduled</title>");
            out.println("</head>");
            out.println("<body>");

            out.println("<h1>Auction Scheduled Successfully</h1>");
            out.println("<p>Product ID: " + productId + "</p>");
            out.println("<p>Starting Price: " + startPrice + "</p>");
            out.println("<p>Scheduling Mode: " + schedulingMode + "</p>");
            out.println("<p>Status: SCHEDULED</p>");

            out.println("<br>");
            out.println("<a href='schedule-auction.html'>Schedule Another Auction</a>");
            out.println("<br><br>");
            out.println("<a href='index.html'>Go to Home Page</a>");

            out.println("</body>");
            out.println("</html>");

        } catch (Exception e) {

            PrintWriter out = response.getWriter();

            out.println("<html>");
            out.println("<body>");

            out.println("<h1>Auction Scheduling Failed</h1>");
            out.println("<p>" + e.getMessage() + "</p>");

            out.println("</body>");
            out.println("</html>");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}