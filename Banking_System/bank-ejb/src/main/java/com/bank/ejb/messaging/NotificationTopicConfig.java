package com.bank.ejb.messaging;

import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.JMSDestinationDefinitions;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@JMSDestinationDefinitions({
    @JMSDestinationDefinition(
        name = "java:global/jms/NotificationTopic",
        interfaceName = "jakarta.jms.Topic",
        destinationName = "NotificationTopic"
    )
})
@Singleton
@Startup
public class NotificationTopicConfig {
    // EJB startup configuration class to initialize JMS Topic
}
