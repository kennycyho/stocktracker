package app;

import app.cooldown.CooldownService;
import app.cooldown.model.Cooldown;
import app.cooldown.repository.CooldownRepository;
import app.dto.Product;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@SpringBootTest
public class CooldownServiceIntegrationTests {

    @Autowired
    CooldownRepository cooldownRepository;

    @Autowired
    CooldownService cooldownService;

    @Value("${cooldown.interval-ms}")
    Long interval;

    private static final Product PRODUCT = new Product("Test Product", "http://www.example.com");

    @BeforeEach
    void setUp() {
        cooldownRepository.deleteAll();
    }

    @Test
    void isValid_returnsTrue_whenNoRecordExists() {
        Assertions.assertTrue(cooldownService.isValid(PRODUCT));
    }

    @Test
    void isValid_returnsTrue_whenRecordIsOffCooldown() {
        Cooldown c = new Cooldown();
        c.setUrl(PRODUCT.url());
        c.setLastSeen(LocalDateTime.now().minusDays(10));
        cooldownRepository.save(c);

        Assertions.assertTrue(cooldownService.isValid(PRODUCT));
    }

    @Test
    void isValid_returnsFalse_whenRecordIsOnCooldown() {
        Cooldown c = new Cooldown();
        c.setUrl(PRODUCT.url());
        c.setLastSeen(LocalDateTime.now());
        cooldownRepository.save(c);

        Assertions.assertFalse(cooldownService.isValid(PRODUCT));
    }

    @Test
    void isValid_returnsFalse_whenRecordIsDisabled() {
        Cooldown c = new Cooldown();
        c.setUrl(PRODUCT.url());
        c.setDisabled(true);
        c.setLastSeen(LocalDateTime.now());
        cooldownRepository.save(c);

        Assertions.assertFalse(cooldownService.isValid(PRODUCT));
    }

    @Test
    void setOrRefreshCooldown_savesNewRecord_whenNoRecordExists() {
        cooldownService.setOrRefreshCooldown(PRODUCT);

        Optional<Cooldown> c = cooldownRepository.findByUrl(PRODUCT.url());
        Assertions.assertTrue(c.isPresent());
        Assertions.assertTrue(c.get().getLastSeen().isAfter(LocalDateTime.now().minus(interval, ChronoUnit.MILLIS)));
    }

    @Test
    void setOrRefreshCooldown_refreshesRecord_whenRecordExists() {
        Cooldown c = new Cooldown();
        c.setUrl(PRODUCT.url());
        c.setLastSeen(LocalDateTime.now().minusDays(10));
        cooldownRepository.save(c);

        cooldownService.setOrRefreshCooldown(PRODUCT);

        Optional<Cooldown> existingCooldown = cooldownRepository.findByUrl(PRODUCT.url());
        Assertions.assertTrue(existingCooldown.isPresent());
        Assertions.assertTrue(existingCooldown.get()
                .getLastSeen().isAfter(LocalDateTime.now().minus(interval, ChronoUnit.MILLIS)));
    }

    @Test
    void disable_createsDisabledRecord_whenNoRecordExists() {
        cooldownService.disable(PRODUCT);

        Optional<Cooldown> cd = cooldownRepository.findByUrl(PRODUCT.url());
        Assertions.assertTrue(cd.isPresent());
        Assertions.assertTrue(cd.get().isDisabled());
    }

    @Test
    void disable_setsRecordToDisabled_whenRecordExists() {
        
    }
}
