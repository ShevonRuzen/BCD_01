package com.techmart.api;

import com.techmart.service.NotificationRegistry;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/notifications")
public class NotificationEndpoint extends HttpServlet {

    @EJB
    private NotificationRegistry registry;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (SessionAuthHelper.sendUnauthorizedIfNotLoggedIn(req, resp)) {
            return;
        }

        String email = SessionAuthHelper.getSessionEmail(req);
        String role = SessionAuthHelper.getSessionRole(req);

        List<NotificationRegistry.NotificationItem> items = registry.getNotificationsForUser(email, role);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            NotificationRegistry.NotificationItem item = items.get(i);
            if (i > 0) json.append(",");
            json.append(String.format(
                "{\"id\":\"%s\",\"title\":\"%s\",\"message\":\"%s\",\"timestamp\":%d}",
                item.getId(),
                item.getTitle().replace("\"", "\\\""),
                item.getMessage().replace("\"", "\\\""),
                item.getTimestamp()
            ));
        }
        json.append("]");

        resp.getWriter().write(json.toString());
    }
}
