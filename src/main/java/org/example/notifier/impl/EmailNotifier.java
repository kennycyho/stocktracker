package org.example.notifier.impl;

import org.example.model.Item;
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

    public void send(String subject, List<Item> items) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(buildBody(items));

        mailSender.send(message);
    }

    private String buildBody(List<Item> items) {
        StringBuilder sb = new StringBuilder();
        for (Item item : items) {
            sb.append(item.name())
                    .append(": ").append(item.url())
                    .append("\n");
        }
        return sb.toString();
    }
}
