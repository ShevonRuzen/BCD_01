package com.techmart.order;

import com.techmart.messaging.NotificationProducer;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Asynchronous engine for outbound notifications.
 */
@Stateless
public class AsynchronousNotificationEngine {

    private static final Logger LOGGER = Logger.getLogger(AsynchronousNotificationEngine.class.getName());

    /** JMS Notification Queue Producer. */
    @EJB
    private NotificationProducer notificationProducer;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sends email and publishes JMS toast asynchronously.
     *
     * @param email   Customer email
     * @param orderId Order UUID
     * @return Future<Boolean> true if any channel works
     */
    @Asynchronous
    public Future<Boolean> sendEmailConfirmation(String email, String orderId) {
        boolean emailSent  = dispatchEmail(email, orderId);
        boolean toastSent  = dispatchToastNotification(email, orderId);

        boolean overallSuccess = emailSent || toastSent; // succeed if at least one channel works
        LOGGER.info(String.format(
            "Async notification complete for order %s – email=%b, toast=%b",
            orderId, emailSent, toastSent
        ));
        return new AsyncResult<>(overallSuccess);
    }

    /**
     * Publishes a standalone JMS toast.
     *
     * @param title     Toast title
     * @param message   Toast body
     * @param recipient Target email or "ADMIN"
     * @return Future<Boolean> true if sent
     */
    @Asynchronous
    public Future<Boolean> sendToastNotification(String title, String message, String recipient) {
        boolean sent = dispatchToastNotification(recipient, title, message);
        return new AsyncResult<>(sent);
    }
    /** Helper for standalone toast alerts. */
    private boolean dispatchToastNotification(String recipient, String title, String message) {
        if (notificationProducer == null) {
            LOGGER.warning("NotificationProducer not injected – standalone toast skipped.");
            return false;
        }
        try {
            notificationProducer.sendNotification(title, message, recipient);
            LOGGER.info("Standalone JMS toast published – title: " + title + ", recipient: " + recipient);
            return true;
        } catch (Exception e) {
            LOGGER.severe("Standalone JMS toast error: " + e.getMessage());
            return false;
        }
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers (synchronous, called from within async methods)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dispatches order-confirmation email via SMTP.
     */
    private boolean dispatchEmail(String email, String orderId) {
        try {
            String subject = "TechMart Order Confirmation";
            String body = String.format(
                "Hello,%n%nYour order %s has been successfully processed.%n%nThank you for shopping with TechMart.%n",
                orderId
            );
            boolean sent = sendMail(email, subject, body);
            if (sent) {
                LOGGER.info("Order receipt dispatched via email to: " + email);
            } else {
                LOGGER.warning("Email skipped (SMTP not configured) for order: " + orderId);
            }
            return sent;
        } catch (Exception e) {
            LOGGER.severe("Email dispatch error for order " + orderId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Publishes customer and admin JMS toast notifications.
     */
    private boolean dispatchToastNotification(String email, String orderId) {
        if (notificationProducer == null) {
            LOGGER.warning("NotificationProducer not injected – toast skipped for order: " + orderId);
            return false;
        }
        try {
            // Customer toast
            notificationProducer.sendNotification(
                "Order Confirmed",
                "Your order " + orderId + " has been successfully processed. Thank you for shopping with TechMart!",
                email
            );

            // Admin toast
            notificationProducer.sendNotification(
                "New Order Received",
                "Order " + orderId + " was placed by " + email + ". Check the dashboard for details.",
                "ADMIN"
            );

            LOGGER.info("JMS toast notifications published for order: " + orderId);
            return true;
        } catch (Exception e) {
            LOGGER.severe("JMS toast dispatch error for order " + orderId + ": " + e.getMessage());
            return false;
        }
    }



    // ─────────────────────────────────────────────────────────────────────────
    // SMTP mail sender
    // ─────────────────────────────────────────────────────────────────────────

    private boolean sendMail(String toEmail, String subject, String body) throws Exception {
        String smtpHost = resolveConfigValue(
            System.getProperty("smtp.host"),
            System.getenv("SMTP_HOST"),
            "smtp.gmail.com"
        );
        String smtpPort = resolveConfigValue(
            System.getProperty("smtp.port"),
            System.getenv("SMTP_PORT"),
            "587"
        );
        String smtpUsername = resolveConfigValue(
            System.getProperty("smtp.username"),
            System.getenv("SMTP_USERNAME"),
            System.getenv("SMTP_USER")
        );
        String smtpPassword = resolveConfigValue(
            System.getProperty("smtp.password"),
            System.getenv("SMTP_PASSWORD"),
            System.getenv("SMTP_PASS")
        );
        String fromEmail = resolveConfigValue(
            System.getProperty("smtp.from"),
            System.getenv("SMTP_FROM"),
            smtpUsername
        );

        if (smtpUsername == null || smtpUsername.isBlank()
                || smtpPassword == null || smtpPassword.isBlank()
                || fromEmail == null || fromEmail.isBlank()) {
            LOGGER.warning(
                "SMTP credentials not configured. Set smtp.username / smtp.password / smtp.from " +
                "system properties or SMTP_USERNAME / SMTP_PASSWORD / SMTP_FROM environment variables."
            );
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.ssl.trust", smtpHost);
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        final String user = smtpUsername;
        final String pass = smtpPassword;
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });

        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setFrom(new InternetAddress(fromEmail));
        mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        mimeMessage.setSubject(subject);
        mimeMessage.setText(body, "UTF-8");

        Transport.send(mimeMessage);
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────────

    /** Resolves first non-null config value. */
    static String resolveConfigValue(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}