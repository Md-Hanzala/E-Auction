package eauction;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class UserSessionServlet extends HttpServlet {

    protected void processRequest(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        HttpSession session =
                request.getSession(false);

        if (session == null ||
                session.getAttribute("user_id") == null) {

            response.getWriter().write(
                    "{\"loggedIn\":false}"
            );

            return;
        }

        Object userId =
                session.getAttribute("user_id");

        Object userName =
                session.getAttribute("user_name");

        Object role =
                session.getAttribute("role");

        String name =
                userName == null
                ? ""
                : userName.toString();

        String userRole =
                role == null
                ? "USER"
                : role.toString();

        String json =
                "{"
                + "\"loggedIn\":true,"
                + "\"userId\":" + userId + ","
                + "\"userName\":\""
                + name.replace("\"", "\\\"")
                + "\","
                + "\"role\":\""
                + userRole.replace("\"", "\\\"")
                + "\""
                + "}";

        response.getWriter().write(json);
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