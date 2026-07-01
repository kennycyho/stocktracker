package app.notifier.impl;

import app.dto.Product;
import app.notifier.Notifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of Notifier that sends emails.
 */
@Service
public class EmailNotifier implements Notifier {

    private final JavaMailSender mailSender;
    private final String recipientEmail;

    /**
     * Constructs a new EmailNotifier with the specified mail sender and recipient email.
     *
     * @param mailSender     the mail sender to use for sending emails
     * @param recipientEmail the email address of the recipient
     */
    public EmailNotifier(JavaMailSender mailSender,
                         @Value("${app.notifier.recipient}") String recipientEmail) {
        this.mailSender = mailSender;
        this.recipientEmail = recipientEmail;
    }

    /**
     * Sends a notification with the specified title and list of products via email.
     *
     * @param subject  the subject of the email
     * @param products the list of products to include in the email
     */
    public void send(String subject, List<Product> products) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(buildBody(products));
        mailSender.send(message);
    }

    /**
     * Builds the body of the email from the list of products.
     *
     * @param products the list of products to include in the email
     * @return the body of the email
     */
    private String buildBody(List<Product> products) {
        StringBuilder sb = new StringBuilder();
        for (Product product : products) {
            sb.append(product.name())
                    .append(": ").append(product.url())
                    .append("\n");
        }
        return sb.toString();
    }
}
