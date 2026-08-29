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

public class RegisterServlet extends HttpServlet {

    protected void processRequest(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        String name =
                request.getParameter("name");

        String email =
                request.getParameter("email");

        String password =
                request.getParameter("password");

        String contact =
                request.getParameter("contact");

        String address =
                request.getParameter("address");

        String url =
                "jdbc:oracle:thin:@localhost:1521:xe";

        String username =
                "system";

        String dbPassword =
                "manager";

        String query =
                "INSERT INTO eauction_users "
                + "(user_id, name, email, user_password, "
                + "contact, address, role) "
                + "VALUES "
                + "(eauction_user_seq.NEXTVAL, ?, ?, ?, ?, ?, 'USER')";

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

                ps.setString(
                        1,
                        name
                );

                ps.setString(
                        2,
                        email
                );

                ps.setString(
                        3,
                        password
                );

                ps.setString(
                        4,
                        contact
                );

                ps.setString(
                        5,
                        address
                );

                ps.executeUpdate();
            }

            PrintWriter out =
                    response.getWriter();

            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>Registration Successful</title>");
            out.println("</head>");

            out.println("<body>");

            out.println(
                    "<h1>Registration Successful</h1>"
            );

            out.println(
                    "<p>Welcome "
                    + escapeHtml(name)
                    + "</p>"
            );

            out.println(
                    "<p>Account Type: USER</p>"
            );

            out.println("<br>");

            out.println(
                    "<a href='login.html'>"
                    + "Go to Sign In"
                    + "</a>"
            );

            out.println("</body>");
            out.println("</html>");

        } catch (Exception e) {

            PrintWriter out =
                    response.getWriter();

            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>Registration Error</title>");
            out.println("</head>");

            out.println("<body>");

            out.println(
                    "<h1>Registration Failed</h1>"
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