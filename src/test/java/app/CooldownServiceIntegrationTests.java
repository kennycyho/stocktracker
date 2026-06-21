package app;

import app.cooldown.CooldownService;
import app.cooldown.model.Cooldown;
import app.cooldown.repository.CooldownRepository;
import app.dto.Product;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
public class CooldownServiceIntegrationTests {

    @Autowired
    CooldownRepository cooldownRepository;

    @Autowired
    CooldownService cooldownService;

    private static final Product PRODUCT = new Product("Test Product", "http://www.example.com");

    @BeforeEach
    void setUp() {
        cooldownRepository.deleteAll();
    }

    @Test
    void isOffCooldown_returnsTrue_whenNoRecordExists() {
        Assertions.assertTrue(cooldownService.isOffCooldown(PRODUCT));
    }

    @Test
    void isOffCooldown_returnsTrue_whenRecordIsExpired() {
        Cooldown c = new Cooldown();
        c.setUrl(PRODUCT.url());
        c.setLastSeen(LocalDateTime.now().minusDays(10));
        cooldownRepository.save(c);

        Assertions.assertTrue(cooldownService.isOffCooldown(PRODUCT));
    }

}
