package eauction;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

@MultipartConfig
public class ProductSubmitServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("login.html");
            return;
        }

        int sellerId = (Integer) session.getAttribute("user_id");

        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String startPrice = request.getParameter("start_price");
        String floorPrice = request.getParameter("floor_price");

        Part imagePart = request.getPart("product_image");

        String fileName = imagePart.getSubmittedFileName();

        String uploadPath = getServletContext().getRealPath("") + File.separator + "uploads";

        File uploadFolder = new File(uploadPath);

        if (!uploadFolder.exists()) {
            uploadFolder.mkdir();
        }

        String imageReference = null;

        if (fileName != null && !fileName.isEmpty()) {

            String savedFileName = System.currentTimeMillis() + "_" + fileName;

            imagePart.write(uploadPath + File.separator + savedFileName);

            imageReference = "uploads/" + savedFileName;
        }

        String url = "jdbc:oracle:thin:@localhost:1521:xe";
        String username = "system";
        String dbPassword = "manager";

        String query = "INSERT INTO products "
                + "(product_id, seller_id, name, description, image_reference, "
                + "start_price, floor_price, status) "
                + "VALUES (product_seq.NEXTVAL, ?, ?, ?, ?, ?, ?, 'PENDING')";

        try {

            Class.forName("oracle.jdbc.OracleDriver");

            Connection con = DriverManager.getConnection(
                    url, username, dbPassword);

            PreparedStatement ps = con.prepareStatement(query);

            ps.setInt(1, sellerId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, imageReference);
            ps.setDouble(5, Double.parseDouble(startPrice));

            if (floorPrice == null || floorPrice.trim().isEmpty()) {
                ps.setNull(6, java.sql.Types.NUMERIC);
            } else {
                ps.setDouble(6, Double.parseDouble(floorPrice));
            }

            ps.executeUpdate();

            ps.close();
            con.close();

            PrintWriter out = response.getWriter();

            out.println("<html>");
            out.println("<head>");
            out.println("<title>Product Submitted</title>");
            out.println("</head>");

            out.println("<body>");

            out.println("<h1>Product Submitted Successfully</h1>");
            out.println("<p>Your product has been submitted for Admin approval.</p>");
            out.println("<p>Status: PENDING</p>");

            if (imageReference != null) {
                out.println("<p>Product Image:</p>");
                out.println("<img src=\"" + imageReference + "\" width=\"250\">");
            }

            out.println("<br><br>");
            out.println("<a href=\"product.html\">Submit Another Product</a>");
            out.println("<br><br>");
            out.println("<a href=\"index.html\">Go to Home Page</a>");

            out.println("</body>");
            out.println("</html>");

        } catch (Exception e) {

            PrintWriter out = response.getWriter();

            out.println("<html>");
            out.println("<body>");

            out.println("<h1>Product Submission Failed</h1>");
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