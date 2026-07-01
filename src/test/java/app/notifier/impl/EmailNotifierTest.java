package app.notifier.impl;

import app.dto.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotifierTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void send_sendsEmail_whenProductsListIsNotEmpty() {
        EmailNotifier emailNotifier = new EmailNotifier(mailSender, "test@example.com");
        List<Product> products = List.of(new Product("productName", "url"));

        emailNotifier.send("Stock Alert", products);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertTrue(message.getText().contains("productName"));
        assertEquals("test@example.com", message.getTo()[0]);
    }
}
