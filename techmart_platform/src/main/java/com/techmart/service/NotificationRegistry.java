package com.techmart.service;

import jakarta.ejb.Singleton;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class NotificationRegistry {

    public static class NotificationItem implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String id;
        private final String title;
        private final String message;
        private final String recipient;
        private final long timestamp;

        public NotificationItem(String title, String message, String recipient) {
            this.id = java.util.UUID.randomUUID().toString();
            this.title = title;
            this.message = message;
            this.recipient = recipient;
            this.timestamp = System.currentTimeMillis();
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getRecipient() { return recipient; }
        public long getTimestamp() { return timestamp; }
    }

    private final List<NotificationItem> notifications = new CopyOnWriteArrayList<>();

    public void addNotification(String title, String message, String recipient) {
        notifications.add(new NotificationItem(title, message, recipient));
        // Keep list size capped to prevent memory leaks
        if (notifications.size() > 100) {
            notifications.remove(0);
        }
    }

    public List<NotificationItem> getNotificationsForUser(String email, String role) {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem item : notifications) {
            if ("ALL".equalsIgnoreCase(item.getRecipient())) {
                result.add(item);
            } else if ("ADMIN".equalsIgnoreCase(item.getRecipient())) {
                if ("ADMIN".equalsIgnoreCase(role)) {
                    result.add(item);
                }
            } else if (email != null && email.equalsIgnoreCase(item.getRecipient())) {
                result.add(item);
            }
        }
        return result;
    }
}
