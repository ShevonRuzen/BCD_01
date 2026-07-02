package com.techmart.api;

import com.techmart.model.Product;
import com.techmart.inventory.InventoryCache;
import com.techmart.service.ProductCatalogService;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet({
    "/api/products",
    "/api/products/*"
})
public class ProductManagementEndpoint extends HttpServlet {

    @EJB
    private ProductCatalogService productCatalogService;

    @EJB
    private InventoryCache inventoryCache;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (SessionAuthHelper.sendUnauthorizedIfNotLoggedIn(req, resp)) {
            return;
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            List<Product> products = productCatalogService.getAllProducts();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < products.size(); i++) {
                Product product = products.get(i);
                if (i > 0) json.append(",");
                json.append(String.format(
                    "{\"id\":\"%s\",\"name\":\"%s\",\"price\":%.2f,\"stock\":%d}",
                    product.getId(), product.getName(), product.getPrice(), product.getStock()
                ));
            }
            json.append("]");
            resp.getWriter().write(json.toString());
            return;
        }

        String id = path.substring(1);
        Product product = productCatalogService.getProductById(id);
        if (product == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Product not found\"}");
            return;
        }

        resp.getWriter().write(String.format(
            "{\"success\":true,\"product\":{\"id\":\"%s\",\"name\":\"%s\",\"price\":%.2f,\"stock\":%d}}",
            product.getId(), product.getName(), product.getPrice(), product.getStock()
        ));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (SessionAuthHelper.sendUnauthorizedIfNotLoggedIn(req, resp)) {
            return;
        }
        if (SessionAuthHelper.sendForbiddenIfNotAdmin(req, resp)) {
            return;
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Map<String, String> params = SessionAuthHelper.readFormParameters(req);
        String id = params.get("id");
        String name = params.get("name");
        String priceStr = params.get("price");
        String stockStr = params.get("stock");

        if (id == null || name == null || priceStr == null || stockStr == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"id, name, price and stock are required\"}");
            return;
        }

        try {
            Product created = productCatalogService.createProduct(
                id,
                name,
                Double.parseDouble(priceStr),
                Integer.parseInt(stockStr)
            );
            if (created == null) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("{\"success\":false,\"message\":\"Product already exists or id is invalid\"}");
                return;
            }
            if (inventoryCache != null) {
                inventoryCache.setStock(created.getId(), created.getStock());
            }
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(String.format(
                "{\"success\":true,\"product\":{\"id\":\"%s\",\"name\":\"%s\",\"price\":%.2f,\"stock\":%d}}",
                created.getId(), created.getName(), created.getPrice(), created.getStock()
            ));
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"price and stock must be numeric\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (SessionAuthHelper.sendUnauthorizedIfNotLoggedIn(req, resp)) {
            return;
        }
        if (SessionAuthHelper.sendForbiddenIfNotAdmin(req, resp)) {
            return;
        }
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String id = req.getPathInfo() == null ? null : req.getPathInfo().substring(1);
        if (id == null || id.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Product id is required\"}");
            return;
        }

        // READ body via readFormParameters
        Map<String, String> params = SessionAuthHelper.readFormParameters(req);
        String name = params.get("name");
        String priceStr = params.get("price");
        String stockStr = params.get("stock");

        Double price = null;
        Integer stock = null;
        try {
            if (priceStr != null && !priceStr.isBlank()) {
                price = Double.parseDouble(priceStr);
            }
            if (stockStr != null && !stockStr.isBlank()) {
                stock = Integer.parseInt(stockStr);
            }
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"price and stock must be numeric\"}");
            return;
        }

        Product updated = productCatalogService.updateProduct(id, name, price, stock);
        if (updated == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Product not found\"}");
            return;
        }

        if (inventoryCache != null && stock != null) {
            inventoryCache.setStock(updated.getId(), updated.getStock());
        }

        resp.getWriter().write(String.format(
            "{\"success\":true,\"product\":{\"id\":\"%s\",\"name\":\"%s\",\"price\":%.2f,\"stock\":%d}}",
            updated.getId(), updated.getName(), updated.getPrice(), updated.getStock()
        ));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (SessionAuthHelper.sendUnauthorizedIfNotLoggedIn(req, resp)) {
            return;
        }
        if (SessionAuthHelper.sendForbiddenIfNotAdmin(req, resp)) {
            return;
        }
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String id = req.getPathInfo() == null ? null : req.getPathInfo().substring(1);
        if (id == null || id.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Product id is required\"}");
            return;
        }

        boolean deleted = productCatalogService.deleteProduct(id);
        if (!deleted) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Product not found\"}");
            return;
        }

        if (inventoryCache != null) {
            inventoryCache.setStock(id, 0);
        }

        resp.getWriter().write("{\"success\":true,\"message\":\"Product deleted successfully\"}");
    }
}