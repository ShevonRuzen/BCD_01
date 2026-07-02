package com.techmart.api;

import com.techmart.user.User;
import com.techmart.user.UserAccountService;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet({
    "/api/auth/register",
    "/api/auth/login",
    "/api/auth/logout",
    "/api/auth/me"
})
public class AuthEndpoint extends HttpServlet {

    @EJB
    private UserAccountService userAccountService;

    @EJB
    private com.techmart.messaging.NotificationProducer notificationProducer;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String path = req.getServletPath();

        if ("/api/auth/register".equals(path)) {
            handleRegister(req, resp);
        } else if ("/api/auth/login".equals(path)) {
            handleLogin(req, resp);
        } else if ("/api/auth/logout".equals(path)) {
            handleLogout(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String path = req.getServletPath();
        if ("/api/auth/me".equals(path)) {
            handleMe(req, resp);
        } else if ("/api/auth/logout".equals(path)) {
            handleLogout(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("loggedInUser") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Not logged in\"}");
            return;
        }

        String email = (String) session.getAttribute("loggedInUser");
        User user = userAccountService.findByEmail(email);
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"User not found\"}");
            return;
        }

        java.util.Map<String, String> params = SessionAuthHelper.readFormParameters(req);
        String username = params.get("username");
        String password = params.get("password");
        String address = params.get("address");
        String telephone = params.get("telephone");

        if (username != null && !username.isBlank()) {
            user.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            user.setPasswordHash(password);
        }
        user.setAddress(address);
        user.setTelephone(telephone);

        userAccountService.updateProfile(user);

        // Update session details if changed
        session.setAttribute("loggedInUsername", user.getUsername());

        resp.getWriter().write(String.format(
            "{\"success\":true,\"message\":\"Profile updated successfully\",\"user\":{\"id\":\"%s\",\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"address\":\"%s\",\"telephone\":\"%s\",\"password\":\"%s\"}}",
            user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
            user.getAddress() != null ? user.getAddress().replace("\"", "\\\"") : "",
            user.getTelephone() != null ? user.getTelephone().replace("\"", "\\\"") : "",
            user.getPasswordHash() != null ? user.getPasswordHash().replace("\"", "\\\"") : ""
        ));
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (username == null || email == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"username, email and password are required\"}");
            return;
        }

        boolean created = userAccountService.registerUser(username, email, password);
        if (!created) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("{\"success\":false,\"message\":\"username or email already exists\"}");
            return;
        }

        notificationProducer.sendNotification(
            "New User Registered",
            "User " + username + " (" + email + ") registered a new customer account.",
            "ADMIN"
        );

        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write("{\"success\":true,\"message\":\"User registered successfully\"}");
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (email == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"email and password are required\"}");
            return;
        }

        User user = userAccountService.authenticate(email, password);
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Invalid email or password\"}");
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("loggedInUser", user.getEmail());
        session.setAttribute("loggedInUserId", user.getId());
        session.setAttribute("loggedInUsername", user.getUsername());
        session.setAttribute("loggedInUserRole", user.getRole());

        resp.getWriter().write(String.format(
            "{\"success\":true,\"message\":\"Login successful\",\"user\":{\"id\":\"%s\",\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\"}}",
            user.getId(), user.getUsername(), user.getEmail(), user.getRole()
        ));
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        resp.getWriter().write("{\"success\":true,\"message\":\"Logged out successfully\"}");
    }

    private void handleMe(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("loggedInUser") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Not logged in\"}");
            return;
        }

        String email = (String) session.getAttribute("loggedInUser");
        User user = userAccountService.findByEmail(email);
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"User not found\"}");
            return;
        }

        resp.getWriter().write(String.format(
            "{\"success\":true,\"user\":{\"id\":\"%s\",\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"address\":\"%s\",\"telephone\":\"%s\",\"password\":\"%s\"}}",
            user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
            user.getAddress() != null ? user.getAddress().replace("\"", "\\\"") : "",
            user.getTelephone() != null ? user.getTelephone().replace("\"", "\\\"") : "",
            user.getPasswordHash() != null ? user.getPasswordHash().replace("\"", "\\\"") : ""
        ));
    }
}