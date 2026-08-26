package eauction;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class LoginServlet extends HttpServlet {

    protected void processRequest(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        String url = "jdbc:oracle:thin:@localhost:1521:xe";
        String username = "system";
        String dbPassword = "manager";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            Class.forName("oracle.jdbc.OracleDriver");

            con = DriverManager.getConnection(
                    url,
                    username,
                    dbPassword
            );

            String sql =
                    "SELECT user_id, name, role "
                    + "FROM eauction_users "
                    + "WHERE email = ? "
                    + "AND user_password = ?";

            ps = con.prepareStatement(sql);

            ps.setString(1, email);
            ps.setString(2, password);

            rs = ps.executeQuery();

            if (rs.next()) {

                int userId =
                        rs.getInt("user_id");

                String name =
                        rs.getString("name");

                String role =
                        rs.getString("role");

                HttpSession session =
                        request.getSession(true);

                session.setAttribute(
                        "user_id",
                        userId
                );

                session.setAttribute(
                        "user_name",
                        name
                );

                session.setAttribute(
                        "role",
                        role
                );

                response.sendRedirect("index.html");
                return;

            } else {

                response.sendRedirect(
                        "login.html?error=invalid"
                );
                return;
            }

        } catch (Exception e) {

            e.printStackTrace();

            response.sendRedirect(
                    "login.html?error=server"
            );

        } finally {

            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception ignored) {
            }

            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ignored) {
            }

            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception ignored) {
            }
        }
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