package app.notifier.impl;

import app.dto.Product;
import app.notifier.Notifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailNotifier implements Notifier {

    private final JavaMailSender mailSender;
    private final String recipientEmail;

    public EmailNotifier(JavaMailSender mailSender,
                         @Value("${app.notifier.recipient}") String recipientEmail) {
        this.mailSender = mailSender;
        this.recipientEmail = recipientEmail;
    }

    public void send(String subject, List<Product> products) {
        if (products.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(buildBody(products));
        mailSender.send(message);
    }

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
