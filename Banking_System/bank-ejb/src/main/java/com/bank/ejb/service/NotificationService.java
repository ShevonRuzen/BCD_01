package com.bank.ejb.service;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.MapMessage;
import jakarta.jms.Topic;
import java.util.logging.Logger;

@Stateless
public class NotificationService {

    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = "java:global/jms/NotificationTopic")
    private Topic topic;

    public void notifyUser(Long userId, String text) {
        try {
            MapMessage message = jmsContext.createMapMessage();
            message.setLong("userId", userId);
            message.setString("text", text);
            jmsContext.createProducer().send(topic, message);
            logger.info("Published JMS notification message for user: " + userId);
        } catch (Exception e) {
            logger.severe("Failed to publish JMS notification: " + e.getMessage());
        }
    }
}
