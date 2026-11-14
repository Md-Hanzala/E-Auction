package com.eauction.controller;

import com.eauction.dao.ProductDAO;
import com.eauction.model.Product;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Consolidated Seller Operations Controller for HASHbid.
 * Manages seller inventory display and multipart product listing submissions.
 */
@WebServlet(urlPatterns = {
    "/seller-dashboard",
    "/add-product"
})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2 MB memory buffer
    maxFileSize = 1024 * 1024 * 10,       // 10 MB max file size
    maxRequestSize = 1024 * 1024 * 50     // 50 MB max request size
)
public class SellerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/seller-dashboard".equals(path)) {
            showDashboard(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/add-product".equals(path)) {
            addProduct(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Loads seller inventory, auction statuses, and winning buyer dispatch details,
     * then forwards to seller-dashboard.jsp.
     */
    private void showDashboard(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.html?error=session_expired");
            return;
        }

        String userRole = (String) session.getAttribute("userRole");
        if (!"SELLER".equalsIgnoreCase(userRole)) {
            response.sendRedirect("login.html?error=unauthorized");
            return;
        }

        int sellerId = (int) session.getAttribute("userId");

        // Retrieve inventory, review status, and winning buyer delivery details
        List<Map<String, Object>> inventory = productDAO.getSellerInventory(sellerId);
        request.setAttribute("inventory", inventory);

        request.getRequestDispatcher("seller-dashboard.jsp").forward(request, response);
    }

    /**
     * Handles multipart form submission for listing a new auction item.
     * Enforces explicit SELLER role authorization prior to processing files.
     */
    private void addProduct(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.html?error=session_expired");
            return;
        }

        // Explicit role guard: /add-product path is not prefixed with 'seller'
        String userRole = (String) session.getAttribute("userRole");
        if (!"SELLER".equalsIgnoreCase(userRole)) {
            response.sendRedirect("login.html?error=unauthorized");
            return;
        }

        int sellerId = (int) session.getAttribute("userId");

        try {
            // 1. Parse text inputs
            String productName = request.getParameter("productName");
            String category = request.getParameter("category");
            String description = request.getParameter("description");
            BigDecimal startingPrice = new BigDecimal(request.getParameter("startingPrice"));

            // 2. Resolve deployment upload directory
            String applicationPath = request.getServletContext().getRealPath("");
            String uploadDirectoryPath = applicationPath + File.separator + "uploads";
            File uploadDir = new File(uploadDirectoryPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 3. Process uploaded image
            Part filePart = request.getPart("productImage");
            String savedRelativePath = null;

            if (filePart != null && filePart.getSize() > 0) {
                String submittedFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                // Sanitize and generate unique filename to prevent collisions
                String uniqueFileName = System.currentTimeMillis() + "_" + submittedFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
                String fullSavePath = uploadDirectoryPath + File.separator + uniqueFileName;

                filePart.write(fullSavePath);
                savedRelativePath = "uploads/" + uniqueFileName;
            }

            // 4. Construct Product model
            Product product = new Product();
            product.setSellerId(sellerId);
            product.setProductName(productName);
            product.setCategory(category);
            product.setDescription(description);
            product.setStartingPrice(startingPrice);
            product.setImagePath(savedRelativePath);
            product.setStatus("PENDING");

            // 5. Commit record to database
            boolean success = productDAO.addProduct(product);

            if (success) {
                response.sendRedirect("seller-dashboard?success=product_submitted");
            } else {
                response.sendRedirect("seller-dashboard?error=db_error");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("seller-dashboard?error=upload_failed");
        }
    }
}
