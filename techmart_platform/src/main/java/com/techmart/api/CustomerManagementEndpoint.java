package com.techmart.api;

import com.techmart.user.User;
import com.techmart.user.UserAccountService;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet({
    "/api/customers",
    "/api/customers/*"
})
public class CustomerManagementEndpoint extends HttpServlet {

    @EJB
    private UserAccountService userAccountService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (SessionAuthHelper.sendUnauthorizedIfNotLoggedIn(req, resp)) {
            return;
        }
        if (SessionAuthHelper.sendForbiddenIfNotAdmin(req, resp)) {
            return;
        }
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            List<User> users = userAccountService.getAllUsers();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                if (i > 0) json.append(",");
                json.append(String.format(
                    "{\"id\":\"%s\",\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"active\":%s,\"address\":\"%s\",\"telephone\":\"%s\"}",
                    user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.isActive() ? "true" : "false",
                    user.getAddress() != null ? user.getAddress().replace("\"", "\\\"") : "",
                    user.getTelephone() != null ? user.getTelephone().replace("\"", "\\\"") : ""
                ));
            }
            json.append("]");
            resp.getWriter().write(json.toString());
            return;
        }

        String id = path.substring(1);
        User user = userAccountService.findById(id);
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Customer not found\"}");
            return;
        }

        resp.getWriter().write(String.format(
            "{\"success\":true,\"user\":{\"id\":\"%s\",\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"active\":%s,\"address\":\"%s\",\"telephone\":\"%s\"}}",
            user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.isActive() ? "true" : "false",
            user.getAddress() != null ? user.getAddress().replace("\"", "\\\"") : "",
            user.getTelephone() != null ? user.getTelephone().replace("\"", "\\\"") : ""
        ));
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
            resp.getWriter().write("{\"success\":false,\"message\":\"Customer id is required\"}");
            return;
        }

        Map<String, String> params = SessionAuthHelper.readFormParameters(req);
        String username = params.get("username");
        String email = params.get("email");
        String role = params.get("role");
        String activeParam = params.get("active");
        String address = params.get("address");
        String telephone = params.get("telephone");
        
        Boolean active = null;
        if (activeParam != null) {
            active = Boolean.valueOf(activeParam);
        }

        User updated = userAccountService.updateUser(id, username, email, role, active, address, telephone);
        if (updated == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Customer not found\"}");
            return;
        }

        resp.getWriter().write(String.format(
            "{\"success\":true,\"user\":{\"id\":\"%s\",\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"address\":\"%s\",\"telephone\":\"%s\"}}",
            updated.getId(), updated.getUsername(), updated.getEmail(), updated.getRole(),
            updated.getAddress() != null ? updated.getAddress().replace("\"", "\\\"") : "",
            updated.getTelephone() != null ? updated.getTelephone().replace("\"", "\\\"") : ""
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
            resp.getWriter().write("{\"success\":false,\"message\":\"Customer id is required\"}");
            return;
        }

        boolean deleted = userAccountService.deleteUser(id);
        if (!deleted) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"success\":false,\"message\":\"Customer not found\"}");
            return;
        }

        resp.getWriter().write("{\"success\":true,\"message\":\"Customer deleted successfully\"}");
    }
}