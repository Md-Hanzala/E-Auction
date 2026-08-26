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

public class RejectProductServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        String productId = request.getParameter("product_id");

        String url = "jdbc:oracle:thin:@localhost:1521:xe";
        String username = "system";
        String dbPassword = "manager";

        String query = "UPDATE products "
                + "SET status = 'REJECTED' "
                + "WHERE product_id = ? "
                + "AND status = 'PENDING'";

        try {
            Class.forName("oracle.jdbc.OracleDriver");

            Connection con = DriverManager.getConnection(
                    url, username, dbPassword);

            PreparedStatement ps = con.prepareStatement(query);

            ps.setInt(1, Integer.parseInt(productId));

            int rows = ps.executeUpdate();

            ps.close();
            con.close();

            PrintWriter out = response.getWriter();

            out.println("<html>");
            out.println("<head>");
            out.println("<title>Product Rejection</title>");
            out.println("</head>");
            out.println("<body>");

            if (rows > 0) {
                out.println("<h1>Product Rejected Successfully</h1>");
                out.println("<p>Product ID: " + productId + "</p>");
                out.println("<p>Status: REJECTED</p>");
            } else {
                out.println("<h1>Product Rejection Failed</h1>");
                out.println("<p>Product was not found or is no longer pending.</p>");
            }

            out.println("<br>");
            out.println("<a href='admin-products'>Back to Admin Products</a>");

            out.println("</body>");
            out.println("</html>");

        } catch (Exception e) {

            PrintWriter out = response.getWriter();

            out.println("<html>");
            out.println("<body>");

            out.println("<h1>Product Rejection Error</h1>");
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