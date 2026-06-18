package org.example.notifier.impl;

import org.example.dto.Product;
import org.example.notifier.Notifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailNotifier implements Notifier {

    private final JavaMailSender mailSender;

    @Value("${app.notifier.recipient}")
    private String recipientEmail;

    public EmailNotifier(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String subject, List<Product> products) {
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
