package com.techmart.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class SessionAuthHelper {

    public static String getSessionEmail(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (String) session.getAttribute("loggedInUser") : null;
    }

    public static String getSessionRole(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (String) session.getAttribute("loggedInUserRole") : null;
    }

    public static boolean isAdmin(HttpServletRequest req) {
        return "ADMIN".equals(getSessionRole(req));
    }

    public static boolean isCustomer(HttpServletRequest req) {
        return "CUSTOMER".equals(getSessionRole(req));
    }

    public static boolean sendForbiddenIfNotAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isAdmin(req)) {
            sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return true;
        }
        return false;
    }

    public static boolean sendForbiddenIfNotCustomer(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!isCustomer(req)) {
            sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "Customer access required");
            return true;
        }
        return false;
    }

    public static boolean sendUnauthorizedIfNotLoggedIn(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("loggedInUser") == null) {
            sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return true;
        }
        return false;
    }

    public static void sendJsonError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(String.format("{\"success\":false,\"message\":\"%s\"}", message));
    }

    public static Map<String, String> readFormParameters(HttpServletRequest req) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        StringBuilder body = new StringBuilder();
        try (var reader = new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8)) {
            char[] buffer = new char[1024];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                body.append(buffer, 0, length);
            }
        }

        if (body.length() == 0) {
            return values;
        }

        String[] pairs = body.toString().split("&");
        for (String pair : pairs) {
            if (pair.isBlank()) {
                continue;
            }
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }
}
