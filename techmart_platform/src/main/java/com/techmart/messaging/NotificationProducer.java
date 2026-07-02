package com.techmart.messaging;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import java.util.logging.Logger;

@Stateless
public class NotificationProducer {

    private static final Logger LOGGER = Logger.getLogger(NotificationProducer.class.getName());

    @Resource(lookup = "java:app/jms/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(lookup = "java:app/jms/NotificationQueue")
    private Queue notificationQueue;

    public void sendNotification(String title, String message, String recipient) {
        try (JMSContext context = connectionFactory.createContext()) {
            String payload = String.format(
                "{\"title\":\"%s\",\"message\":\"%s\",\"recipient\":\"%s\",\"timestamp\":%d}",
                title.replace("\"", "\\\"").replace("\n", " "),
                message.replace("\"", "\\\"").replace("\n", " "),
                recipient.replace("\"", "\\\""),
                System.currentTimeMillis()
            );
            context.createProducer().send(notificationQueue, payload);
            LOGGER.info("Published notification to JMS Queue: " + payload);
        } catch (Exception e) {
            LOGGER.severe("Failed to send JMS notification: " + e.getMessage());
        }
    }
}
