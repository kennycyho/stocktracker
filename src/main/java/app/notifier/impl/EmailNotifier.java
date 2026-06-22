package app.notifier.impl;

import app.cooldown.CooldownService;
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
    private final CooldownService cooldownService;

    @Value("${app.notifier.recipient}")
    private String recipientEmail;

    public EmailNotifier(JavaMailSender mailSender,
                         CooldownService cooldownService) {
        this.mailSender = mailSender;
        this.cooldownService = cooldownService;
    }

    public void send(String subject, List<Product> products) {
        List<Product> productsToNotify = getProductsToNotify(products);

        if (productsToNotify.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(buildBody(productsToNotify));
        mailSender.send(message);

        setCooldowns(productsToNotify);
    }

    private List<Product> getProductsToNotify(List<Product> products) {
        return products.stream()
                .filter(cooldownService::isOffCooldownAndEnabled)
                .toList();
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

    private void setCooldowns(List<Product> productsToNotify) {
        for (Product product : productsToNotify) {
            cooldownService.setOrRefreshCooldown(product);
        }
    }
}
