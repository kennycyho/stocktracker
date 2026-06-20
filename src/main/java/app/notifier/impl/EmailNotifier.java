package app.notifier.impl;

import app.cooldown.CooldownService;
import app.dto.Product;
import app.notifier.Notifier;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmailNotifier implements Notifier {

    private final JavaMailSender mailSender;
    private final CooldownService cooldownService;

    public EmailNotifier(JavaMailSender mailSender,
                         CooldownService cooldownService) {
        this.mailSender = mailSender;
        this.cooldownService = cooldownService;
    }

    public void send(String recipientEmail, String subject, List<Product> products) {
        List<Product> productsToNotify = getProductsToNotify(products);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(buildBody(productsToNotify));
        mailSender.send(message);

        setCooldowns(productsToNotify);
    }

    private List<Product> getProductsToNotify(List<Product> products) {
        List<Product> productsToNotify = new ArrayList<>();
        for (Product product : products) {
            if (cooldownService.isOffCooldown(product) && !cooldownService.isDisabled(product)) {
                productsToNotify.add(product);
            }
        }
        return productsToNotify;
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
