package com.eauction.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());

        // Allow public assets, login, registration, home routes, and public live auction teaser
        if (path.startsWith("/assets/") ||
            path.startsWith("/uploads/") ||
            path.equals("/") ||
            path.equals("/index.html") ||
            path.equals("/auth.html") ||
            path.equals("/login.html") ||
            path.equals("/login") ||
            path.equals("/logout") ||
            path.equals("/buyer-register.html") ||
            path.equals("/buyer-register") ||
            path.equals("/seller-register.html") ||
            path.equals("/seller-register") ||
            path.equals("/seller-pending.html") ||
            path.equals("/featured-live-auction")) {
            chain.doFilter(req, res);
            return;
        }

        // Check active session
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            // Distinguish AJAX requests (e.g. In-place bidding) from standard browser navigation
            if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"Session expired. Please log in again.\"}");
                return;
            }
            response.sendRedirect(contextPath + "/login.html?error=session_required");
            return;
        }

        String role = (String) session.getAttribute("userRole");

        // Role-Based Route Protection
        if (path.contains("admin") && !"ADMIN".equalsIgnoreCase(role)) {
            response.sendRedirect(contextPath + "/login.html?error=unauthorized_admin_area");
            return;
        }

        if (path.contains("seller") && !"SELLER".equalsIgnoreCase(role)) {
            response.sendRedirect(contextPath + "/login.html?error=unauthorized_seller_area");
            return;
        }

        if (path.contains("buyer") && !"BUYER".equalsIgnoreCase(role)) {
            response.sendRedirect(contextPath + "/login.html?error=unauthorized_buyer_area");
            return;
        }

        // Prevent back-button caching of protected views after logout
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        chain.doFilter(req, res);
    }
}
