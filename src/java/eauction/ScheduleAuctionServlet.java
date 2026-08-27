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

public class ScheduleAuctionServlet extends HttpServlet {

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

        Object role =
                session.getAttribute("role");

        if (role == null ||
                !"ADMIN".equals(role.toString())) {

            response.setStatus(
                    HttpServletResponse.SC_FORBIDDEN
            );

            PrintWriter out =
                    response.getWriter();

            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>Access Denied</title>");
            out.println("</head>");

            out.println("<body>");

            out.println("<h1>Access Denied</h1>");

            out.println(
                    "<p>Only the Admin can schedule auctions.</p>"
            );

            out.println("<br>");

            out.println(
                    "<a href='index.html'>"
                    + "Go to Dashboard"
                    + "</a>"
            );

            out.println("</body>");
            out.println("</html>");

            return;
        }

        String productId =
                request.getParameter("product_id");

        String startTime =
                request.getParameter("start_time");

        String endTime =
                request.getParameter("end_time");

        String schedulingMode =
                request.getParameter("scheduling_mode");

        if (productId == null ||
                productId.trim().isEmpty() ||
                startTime == null ||
                startTime.trim().isEmpty() ||
                endTime == null ||
                endTime.trim().isEmpty() ||
                schedulingMode == null ||
                schedulingMode.trim().isEmpty()) {

            showMessage(
                    response,
                    "Scheduling Failed",
                    "All auction scheduling fields are required."
            );

            return;
        }

        String url =
                "jdbc:oracle:thin:@localhost:1521:xe";

        String username =
                "system";

        String dbPassword =
                "manager";

        String productQuery =
                "SELECT start_price "
                + "FROM products "
                + "WHERE product_id = ? "
                + "AND status = 'APPROVED'";

        String auctionQuery =
                "INSERT INTO auctions "
                + "(auction_id, product_id, start_time, end_time, "
                + "current_price, status, scheduling_mode, "
                + "total_extension_seconds) "
                + "VALUES "
                + "(auction_seq.NEXTVAL, ?, ?, ?, ?, "
                + "'SCHEDULED', ?, 0)";

        double startPrice;

        try {

            Class.forName(
                    "oracle.jdbc.OracleDriver"
            );

            try (Connection con =
                    DriverManager.getConnection(
                            url,
                            username,
                            dbPassword
                    )) {

                try (
                    PreparedStatement productPs =
                            con.prepareStatement(
                                    productQuery
                            )
                ) {

                    productPs.setInt(
                            1,
                            Integer.parseInt(productId)
                    );

                    try (
                        ResultSet rs =
                                productPs.executeQuery()
                    ) {

                        if (!rs.next()) {

                            showMessage(
                                    response,
                                    "Scheduling Failed",
                                    "Product does not exist or is not APPROVED."
                            );

                            return;
                        }

                        startPrice =
                                rs.getDouble(
                                        "start_price"
                                );
                    }
                }

                Timestamp parsedStartTime =
                        Timestamp.valueOf(
                                startTime.replace(
                                        "T",
                                        " "
                                ) + ":00"
                        );

                Timestamp parsedEndTime =
                        Timestamp.valueOf(
                                endTime.replace(
                                        "T",
                                        " "
                                ) + ":00"
                        );

                if (!parsedEndTime.after(
                        parsedStartTime
                )) {

                    showMessage(
                            response,
                            "Scheduling Failed",
                            "End time must be after start time."
                    );

                    return;
                }

                try (
                    PreparedStatement auctionPs =
                            con.prepareStatement(
                                    auctionQuery
                            )
                ) {

                    auctionPs.setInt(
                            1,
                            Integer.parseInt(productId)
                    );

                    auctionPs.setTimestamp(
                            2,
                            parsedStartTime
                    );

                    auctionPs.setTimestamp(
                            3,
                            parsedEndTime
                    );

                    auctionPs.setDouble(
                            4,
                            startPrice
                    );

                    auctionPs.setString(
                            5,
                            schedulingMode
                    );

                    auctionPs.executeUpdate();
                }
            }

            showMessage(
                    response,
                    "Auction Scheduled Successfully",
                    "Product ID: "
                    + productId
                    + "<br>Starting Price: "
                    + startPrice
                    + "<br>Scheduling Mode: "
                    + schedulingMode
                    + "<br>Status: SCHEDULED"
            );

        } catch (NumberFormatException e) {

            showMessage(
                    response,
                    "Scheduling Failed",
                    "Invalid Product ID."
            );

        } catch (Exception e) {

            showMessage(
                    response,
                    "Auction Scheduling Failed",
                    e.getMessage()
            );
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

        out.println("<br>");

        out.println(
                "<a href='schedule-auction.html'>"
                + "Back to Schedule Auction"
                + "</a>"
        );

        out.println("<br><br>");

        out.println(
                "<a href='index.html'>"
                + "Go to Dashboard"
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