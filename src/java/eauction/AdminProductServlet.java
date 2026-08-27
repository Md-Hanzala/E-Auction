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
import jakarta.servlet.http.HttpSession;

public class AdminProductServlet extends HttpServlet {

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
                    "<p>Only the Admin can access Product Approval.</p>"
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

        String url =
                "jdbc:oracle:thin:@localhost:1521:xe";

        String username =
                "system";

        String dbPassword =
                "manager";

        String query =
                "SELECT product_id, seller_id, name, description, "
                + "image_reference, start_price, floor_price "
                + "FROM products "
                + "WHERE status = 'PENDING' "
                + "ORDER BY product_id";

        try {

            Class.forName(
                    "oracle.jdbc.OracleDriver"
            );

            Connection con =
                    DriverManager.getConnection(
                            url,
                            username,
                            dbPassword
                    );

            PreparedStatement ps =
                    con.prepareStatement(query);

            ResultSet rs =
                    ps.executeQuery();

            PrintWriter out =
                    response.getWriter();

            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>Admin - Product Approval</title>");
            out.println("</head>");

            out.println("<body>");

            out.println(
                    "<h1>Admin Product Approval</h1>"
            );

            out.println("<table border='1'>");

            out.println("<tr>");
            out.println("<th>Product ID</th>");
            out.println("<th>Seller ID</th>");
            out.println("<th>Name</th>");
            out.println("<th>Description</th>");
            out.println("<th>Image</th>");
            out.println("<th>Starting Price</th>");
            out.println("<th>Floor Price</th>");
            out.println("<th>Action</th>");
            out.println("</tr>");

            boolean found = false;

            while (rs.next()) {

                found = true;

                int productId =
                        rs.getInt("product_id");

                int sellerId =
                        rs.getInt("seller_id");

                String name =
                        rs.getString("name");

                String description =
                        rs.getString("description");

                String imageReference =
                        rs.getString("image_reference");

                double startPrice =
                        rs.getDouble("start_price");

                double floorPrice =
                        rs.getDouble("floor_price");

                out.println("<tr>");

                out.println(
                        "<td>"
                        + productId
                        + "</td>"
                );

                out.println(
                        "<td>"
                        + sellerId
                        + "</td>"
                );

                out.println(
                        "<td>"
                        + escapeHtml(name)
                        + "</td>"
                );

                out.println(
                        "<td>"
                        + escapeHtml(description)
                        + "</td>"
                );

                out.println("<td>");

                if (imageReference != null &&
                        !imageReference.trim().isEmpty()) {

                    out.println(
                            "<img src='"
                            + escapeHtml(imageReference)
                            + "' width='150'>"
                    );
                }

                out.println("</td>");

                out.println(
                        "<td>"
                        + startPrice
                        + "</td>"
                );

                out.println(
                        "<td>"
                        + floorPrice
                        + "</td>"
                );

                out.println("<td>");

                out.println(
                        "<form method='post' "
                        + "action='approve-product'>"
                );

                out.println(
                        "<input type='hidden' "
                        + "name='product_id' "
                        + "value='"
                        + productId
                        + "'>"
                );

                out.println(
                        "<input type='submit' "
                        + "value='APPROVE'>"
                );

                out.println("</form>");

                out.println("<br>");

                out.println(
                        "<form method='post' "
                        + "action='reject-product'>"
                );

                out.println(
                        "<input type='hidden' "
                        + "name='product_id' "
                        + "value='"
                        + productId
                        + "'>"
                );

                out.println(
                        "<input type='submit' "
                        + "value='REJECT'>"
                );

                out.println("</form>");

                out.println("</td>");

                out.println("</tr>");
            }

            out.println("</table>");

            if (!found) {

                out.println(
                        "<p>No pending products found.</p>"
                );
            }

            out.println("<br>");

            out.println(
                    "<a href='index.html'>"
                    + "Go to Dashboard"
                    + "</a>"
            );

            out.println("</body>");
            out.println("</html>");

            rs.close();
            ps.close();
            con.close();

        } catch (Exception e) {

            PrintWriter out =
                    response.getWriter();

            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>Admin Product Error</title>");
            out.println("</head>");

            out.println("<body>");

            out.println(
                    "<h1>Admin Product Error</h1>"
            );

            out.println(
                    "<p>"
                    + escapeHtml(e.getMessage())
                    + "</p>"
            );

            out.println("</body>");
            out.println("</html>");
        }
    }

    private String escapeHtml(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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