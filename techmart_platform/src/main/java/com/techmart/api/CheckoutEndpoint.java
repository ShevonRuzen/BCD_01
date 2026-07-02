package com.techmart.api;

import com.techmart.order.OrderProcessingEngine;
import com.techmart.order.OrderResult;
import com.techmart.service.CartService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/checkout")
public class CheckoutEndpoint extends HttpServlet {

    @jakarta.ejb.EJB
    private CartService cartService;

    @jakarta.ejb.EJB
    private OrderProcessingEngine orderProcessingEngine;

    @jakarta.ejb.EJB
    private com.techmart.user.UserAccountService userAccountService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (SessionAuthHelper.sendUnauthorizedIfNotLoggedIn(req, resp)) {
            return;
        }
        if (SessionAuthHelper.sendForbiddenIfNotCustomer(req, resp)) {
            return;
        }

        String email = SessionAuthHelper.getSessionEmail(req);
        if (email == null || email.isBlank()) {
            SessionAuthHelper.sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        String action = req.getParameter("action");
        String prodId = req.getParameter("id");
        String qtyStr = req.getParameter("qty");
        int qty = 1;
        if (qtyStr != null && !qtyStr.isBlank()) {
            try {
                qty = Integer.parseInt(qtyStr);
            } catch (NumberFormatException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write("{\"success\":false,\"message\":\"qty must be a whole number\"}");
                return;
            }
        }
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if ("add".equalsIgnoreCase(action)) {
            if (prodId != null) {
                try {
                    cartService.addToCart(email, prodId, qty);
                    Map<String, Integer> cart = cartService.getCartMap(email);
                    resp.getWriter().write(String.format(
                        "{\"success\": true, \"message\": \"Added to cart: %d of %s\", \"cart\": %s}",
                        qty, prodId, getCartJson(cart)
                    ));
                } catch (IllegalArgumentException e) {
                    Map<String, Integer> cart = cartService.getCartMap(email);
                    resp.getWriter().write(String.format(
                        "{\"success\": false, \"message\": \"%s\", \"cart\": %s}",
                        e.getMessage(), getCartJson(cart)
                    ));
                }
            }
        } else if ("update".equalsIgnoreCase(action)) {
            if (prodId != null) {
                try {
                    cartService.setQuantity(email, prodId, qty);
                    Map<String, Integer> cart = cartService.getCartMap(email);
                    resp.getWriter().write(String.format(
                        "{\"success\": true, \"message\": \"Updated %s to %d\", \"cart\": %s}",
                        prodId, qty, getCartJson(cart)
                    ));
                } catch (IllegalArgumentException e) {
                    Map<String, Integer> cart = cartService.getCartMap(email);
                    resp.getWriter().write(String.format(
                        "{\"success\": false, \"message\": \"%s\", \"cart\": %s}",
                        e.getMessage(), getCartJson(cart)
                    ));
                }
            }
        } else if ("remove".equalsIgnoreCase(action)) {
            if (prodId != null) {
                cartService.removeItem(email, prodId);
            }
            Map<String, Integer> cart = cartService.getCartMap(email);
            resp.getWriter().write(String.format(
                "{\"success\": true, \"message\": \"Removed %s from cart\", \"cart\": %s}",
                prodId, getCartJson(cart)
            ));
        } else if ("clear".equalsIgnoreCase(action)) {
            cartService.clearCart(email);
            resp.getWriter().write("{\"success\": true, \"message\": \"Cart cleared.\"}");
        } else if ("cart".equalsIgnoreCase(action)) {
            Map<String, Integer> cart = cartService.getCartMap(email);
            resp.getWriter().write(String.format(
                "{\"success\": true, \"cart\": %s, \"email\": \"%s\"}",
                getCartJson(cart),
                email
            ));
        } else {
            com.techmart.user.User user = userAccountService.findByEmail(email);
            if (user == null || user.getAddress() == null || user.getAddress().isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\": false, \"message\": \"Please fill your address in your profile before placing an order!\"}");
                return;
            }

            Map<String, Integer> cart = cartService.getCartMap(email);
            if (cart.isEmpty()) {
                resp.getWriter().write("{\"success\": false, \"message\": \"Your cart is empty.\"}");
                return;
            }

            OrderResult result = orderProcessingEngine.processCartCheckout(cart, email);
            if (result.isSuccess()) {
                cartService.clearCart(email);
            }
            resp.getWriter().write(String.format(
                "{\"success\": %b, \"orderId\": \"%s\", \"status\": \"%s\", \"serverExecutionTimeMs\": %.4f}",
                result.isSuccess(),
                result.getOrderId() != null ? result.getOrderId() : "",
                result.getStatus(),
                result.getExecutionTime()
            ));
        }
    }

    private String getCartJson(Map<String, Integer> cart) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}