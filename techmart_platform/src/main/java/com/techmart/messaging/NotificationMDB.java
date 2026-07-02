package com.techmart.messaging;

import com.techmart.service.NotificationRegistry;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.StringReader;
import java.util.logging.Logger;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:app/jms/NotificationQueue"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
})
public class NotificationMDB implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(NotificationMDB.class.getName());

    @EJB
    private NotificationRegistry registry;

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                String payload = ((TextMessage) message).getText();
                LOGGER.info("NotificationMDB received message: " + payload);

                JsonObject json = Json.createReader(new StringReader(payload)).readObject();
                String title = json.getString("title");
                String msg = json.getString("message");
                String recipient = json.getString("recipient");

                registry.addNotification(title, msg, recipient);
            }
        } catch (Exception e) {
            LOGGER.severe("Error processing notification message: " + e.getMessage());
        }
    }
}
