package com.techmart.api;

import com.techmart.model.Order;
import com.techmart.service.OrderManagementService;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet({
    "/api/orders",
    "/api/orders/*"
})
public class OrderManagementEndpoint extends HttpServlet {

    @EJB
    private OrderManagementService orderManagementService;

    @EJB
    private com.techmart.messaging.NotificationProducer notificationProducer;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (SessionAuthHelper.sendUnauthorizedIfNotLoggedIn(req, resp)) {
            return;
        }
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String sessionEmail = SessionAuthHelper.getSessionEmail(req);
        String email = req.getParameter("email");
        String path = req.getPathInfo();

        if (SessionAuthHelper.isAdmin(req)) {
            if (email != null && !email.isBlank()) {
                List<Order> orders = orderManagementService.getOrdersByEmail(email);
                resp.getWriter().write(renderOrderList(orders));
                return;
            }

            if (path == null || path.equals("/")) {
                List<Order> orders = orderManagementService.getAllOrders();
                resp.getWriter().write(renderOrderList(orders));
                return;
            }

            String id = path.substring(1);
            Order order = orderManagementService.getOrderById(id);
            if (order == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"success\":false,\"message\":\"Order not found\"}");
                return;
            }
            resp.getWriter().write(renderOrder(order));
            return;
        }

        // Customer access
        if (email == null || email.isBlank()) {
            email = sessionEmail;
        } else if (!email.equals(sessionEmail)) {
            SessionAuthHelper.sendForbiddenIfNotCustomer(req, resp);
            return;
        }

        if (path == null || path.equals("/")) {
            List<Order> orders = orderManagementService.getOrdersByEmail(email);
            resp.getWriter().write(renderOrderList(orders));
            return;
        }

        String id = path.substring(1);
        Order order = orderManagementService.getOrderById(id);
        if (order == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Order not found\"}");
            return;
        }
        if (!sessionEmail.equals(order.getCustomerEmail())) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("{\"success\":false,\"message\":\"Cannot view another customer's order\"}");
            return;
        }
        resp.getWriter().write(renderOrder(order));
    }

    private String renderOrderList(List<Order> orders) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            if (i > 0) json.append(",");
            com.techmart.model.Product product = orderManagementService.getProductById(order.getProductId());
            String productName = product != null ? product.getName().replace("\"", "\\\"") : order.getProductId();
            double unitPrice = product != null ? product.getPrice() : 0.0;
            double totalPrice = unitPrice * order.getQuantity();
            json.append(String.format(
                "{\"id\":\"%s\",\"productId\":\"%s\",\"productName\":\"%s\",\"quantity\":%d,\"unitPrice\":%.2f,\"totalPrice\":%.2f,\"customerEmail\":\"%s\",\"status\":\"%s\"}",
                order.getId(), order.getProductId(), productName, order.getQuantity(),
                unitPrice, totalPrice, order.getCustomerEmail(), order.getStatus()
            ));
        }
        json.append("]");
        return json.toString();
    }

    private String renderOrder(Order order) {
        com.techmart.model.Product product = orderManagementService.getProductById(order.getProductId());
        String productName = product != null ? product.getName().replace("\"", "\\\"") : order.getProductId();
        double unitPrice = product != null ? product.getPrice() : 0.0;
        double totalPrice = unitPrice * order.getQuantity();
        com.techmart.user.User user = orderManagementService.getCustomerByEmail(order.getCustomerEmail());
        String address = (user != null && user.getAddress() != null) ? user.getAddress().replace("\"", "\\\"") : "";
        return String.format(
            "{\"success\":true,\"order\":{\"id\":\"%s\",\"productId\":\"%s\",\"productName\":\"%s\",\"quantity\":%d,\"unitPrice\":%.2f,\"totalPrice\":%.2f,\"customerEmail\":\"%s\",\"status\":\"%s\",\"deliveryAddress\":\"%s\"}}",
            order.getId(), order.getProductId(), productName, order.getQuantity(),
            unitPrice, totalPrice, order.getCustomerEmail(), order.getStatus(), address
        );
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
            resp.getWriter().write("{\"success\":false,\"message\":\"Order id is required\"}");
            return;
        }

        Map<String, String> params = SessionAuthHelper.readFormParameters(req);
        String status = params.get("status");
        if (status == null || status.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"status is required\"}");
            return;
        }

        Order updated = orderManagementService.updateOrderStatus(id, status);
        if (updated == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Order not found\"}");
            return;
        }

        String messageText;
        String titleText = "Order Status Updated";
        if ("CANCELLED".equalsIgnoreCase(status)) {
            titleText = "Order Cancelled";
            com.techmart.model.Product product = orderManagementService.getProductById(updated.getProductId());
            String productName = product != null ? product.getName() : updated.getProductId();
            double price = product != null ? product.getPrice() : 0.0;
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = java.time.LocalDateTime.now().format(formatter);
            
            messageText = String.format("Order Cancelled Details - Date/Time: %s | Item: %s | Qty: %d | Total Price: LKR %.2f",
                formattedDateTime, productName, updated.getQuantity(), price * updated.getQuantity());
        } else {
            messageText = "Your order " + id + " has been updated to: " + status;
        }

        notificationProducer.sendNotification(
            titleText,
            messageText,
            updated.getCustomerEmail()
        );

        resp.getWriter().write(String.format(
            "{\"success\":true,\"order\":{\"id\":\"%s\",\"productId\":\"%s\",\"quantity\":%d,\"customerEmail\":\"%s\",\"status\":\"%s\"}}",
            updated.getId(), updated.getProductId(), updated.getQuantity(), updated.getCustomerEmail(), updated.getStatus()
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
            resp.getWriter().write("{\"success\":false,\"message\":\"Order id is required\"}");
            return;
        }

        boolean deleted = orderManagementService.deleteOrder(id);
        if (!deleted) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Order not found\"}");
            return;
        }

        resp.getWriter().write("{\"success\":true,\"message\":\"Order deleted successfully\"}");
    }
}