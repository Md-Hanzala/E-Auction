package com.eauction.controller;

import com.eauction.dao.UserDAO;
import com.eauction.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Consolidated Authentication Controller for HASHbid.
 * Manages user authentication, session invalidation, and role-based registrations.
 */
@WebServlet(urlPatterns = {
    "/login",
    "/logout",
    "/buyer-register",
    "/seller-register"
})
public class AuthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/logout".equals(path)) {
            handleLogout(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        switch (path) {
            case "/login":
                handleLogin(request, response);
                break;
            case "/buyer-register":
                handleBuyerRegister(request, response);
                break;
            case "/seller-register":
                handleSellerRegister(request, response);
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                break;
        }
    }

    /**
     * Authenticates credentials, creates clean session with 5 attributes,
     * and performs role-based redirection.
     */
    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        // 1. Validate inputs
        if (email == null || password == null || email.trim().isEmpty() || password.trim().isEmpty()) {
            response.sendRedirect("login.html?error=empty_credentials");
            return;
        }

        // 2. Query Oracle Database via UserDAO
        User user = userDAO.authenticate(email.trim(), password);

        // 3. Reject invalid credentials
        if (user == null) {
            response.sendRedirect("login.html?error=invalid_credentials");
            return;
        }

        // 4. Invalidate any existing session to prevent account contamination
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        // 5. Generate a completely clean session and bind all 5 attributes
        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userRole", user.getRole());
        session.setAttribute("userName", user.getFullName());
        session.setAttribute("userEmail", user.getEmail());

        // 6. Role-Based Access Control (RBAC) Routing
        String role = (user.getRole() != null) ? user.getRole().trim().toUpperCase() : "";

        switch (role) {
            case "ADMIN":
                response.sendRedirect("admin-dashboard");
                break;

            case "BUYER":
                response.sendRedirect("buyer-dashboard");
                break;

            case "SELLER":
                if ("APPROVED".equalsIgnoreCase(user.getStatus())) {
                    response.sendRedirect("seller-dashboard");
                } else {
                    response.sendRedirect("seller-pending.html");
                }
                break;

            default:
                response.sendRedirect("login.html?error=unknown_role");
                break;
        }
    }

    /**
     * Terminates the active HTTP session, sends browser cache invalidation headers,
     * and redirects to login with status indicator.
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        response.sendRedirect("login.html?status=logged_out");
    }

    /**
     * Registers a new buyer account with APPROVED status.
     */
    private void handleBuyerRegister(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Invalidate any leftover session before registration
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String address = request.getParameter("shippingAddress");

        // Validation check
        if (fullName == null || email == null || password == null ||
            fullName.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty()) {
            response.sendRedirect("buyer-register.html?error=missing_fields");
            return;
        }

        // Construct User model
        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPhone(phone != null ? phone.trim() : null);
        user.setPassword(password.trim());
        user.setRole("BUYER");
        user.setStatus("APPROVED");
        user.setAddress(address != null ? address.trim() : null);

        // Commit to database
        boolean created = userDAO.registerUser(user);

        if (created) {
            response.sendRedirect("login.html?status=registration_success");
        } else {
            response.sendRedirect("buyer-register.html?error=email_exists");
        }
    }

    /**
     * Registers a new seller account with PENDING status for admin vetting.
     */
    private void handleSellerRegister(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String address = request.getParameter("address");

        // Validation check
        if (fullName == null || email == null || password == null ||
            fullName.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty()) {
            response.sendRedirect("seller-register.html?error=missing_fields");
            return;
        }

        // Check if email already taken
        if (userDAO.emailExists(email)) {
            response.sendRedirect("seller-register.html?error=email_exists");
            return;
        }

        // Construct User model
        User newSeller = new User();
        newSeller.setFullName(fullName.trim());
        newSeller.setEmail(email.trim());
        newSeller.setPhone(phone != null ? phone.trim() : "");
        newSeller.setPassword(password);
        newSeller.setRole("SELLER");
        newSeller.setStatus("PENDING");
        newSeller.setAddress(address != null ? address.trim() : "");

        // Commit to database
        boolean success = userDAO.registerUser(newSeller);

        if (success) {
            response.sendRedirect("seller-pending.html");
        } else {
            response.sendRedirect("seller-register.html?error=db_error");
        }
    }
}
