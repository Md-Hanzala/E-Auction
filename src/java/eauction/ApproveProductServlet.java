package eauction;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class ApproveProductServlet extends HttpServlet {

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
                    "<p>Only the Admin can approve products.</p>"
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

        if (productId == null ||
                productId.trim().isEmpty()) {

            showMessage(
                    response,
                    "Approval Failed",
                    "Product ID is required."
            );

            return;
        }

        String url =
                "jdbc:oracle:thin:@localhost:1521:xe";

        String username =
                "system";

        String dbPassword =
                "manager";

        String query =
                "UPDATE products "
                + "SET status = 'APPROVED' "
                + "WHERE product_id = ? "
                + "AND status = 'PENDING'";

        try {

            Class.forName(
                    "oracle.jdbc.OracleDriver"
            );

            try (
                Connection con =
                        DriverManager.getConnection(
                                url,
                                username,
                                dbPassword
                        );

                PreparedStatement ps =
                        con.prepareStatement(query)
            ) {

                ps.setInt(
                        1,
                        Integer.parseInt(productId)
                );

                int rows =
                        ps.executeUpdate();

                if (rows > 0) {

                    response.sendRedirect(
                            "schedule-auction.html?product_id="
                            + productId
                    );

                    return;

                } else {

                    showMessage(
                            response,
                            "Product Approval Failed",
                            "Product was not found or is no longer pending."
                    );
                }
            }

        } catch (NumberFormatException e) {

            showMessage(
                    response,
                    "Approval Failed",
                    "Invalid Product ID."
            );

        } catch (Exception e) {

            showMessage(
                    response,
                    "Product Approval Error",
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

        out.println("<br>");

        out.println(
                "<a href='admin-products'>"
                + "Back to Admin Products"
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