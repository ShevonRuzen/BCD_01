package com.bank.web.messaging;

import com.bank.web.websocket.NotificationWebSocket;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import java.util.logging.Logger;

@MessageDriven(
    activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:global/jms/NotificationTopic"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic")
    }
)
public class NotificationMDB implements MessageListener {

    private static final Logger logger = Logger.getLogger(NotificationMDB.class.getName());

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof MapMessage) {
                MapMessage mapMsg = (MapMessage) message;
                Long userId = mapMsg.getLong("userId");
                String text = mapMsg.getString("text");
                
                logger.info("MDB received notification for user " + userId + ": " + text);
                
                // Route to WebSocket session
                NotificationWebSocket.sendNotification(userId, text);
            }
        } catch (JMSException e) {
            logger.severe("MDB message processing failed: " + e.getMessage());
        }
    }
}
