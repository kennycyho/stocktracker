package app.cooldown;

import app.cooldown.model.Cooldown;
import app.cooldown.repository.CooldownRepository;
import app.dto.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
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
        assertTrue(cooldownService.isValid(PRODUCT));
    }

    @Test
    void isValid_returnsTrue_whenRecordIsOffCooldown() {
        Cooldown c = new Cooldown();
        c.setUrl(PRODUCT.url());
        c.setLastSeen(LocalDateTime.now().minusDays(10));
        cooldownRepository.save(c);

        assertTrue(cooldownService.isValid(PRODUCT));
    }

    @Test
    void isValid_returnsFalse_whenRecordIsOnCooldown() {
        Cooldown c = new Cooldown();
        c.setUrl(PRODUCT.url());
        cooldownRepository.save(c);

        assertFalse(cooldownService.isValid(PRODUCT));
    }

    @Test
    void isValid_returnsFalse_whenRecordIsDisabled() {
        Cooldown c = new Cooldown();
        c.setUrl(PRODUCT.url());
        c.setLastSeen(LocalDateTime.now().minusDays(10));
        c.setDisabled(true);
        cooldownRepository.save(c);

        assertFalse(cooldownService.isValid(PRODUCT));
    }

    @Test
    void setOrRefreshCooldown_savesNewRecord_whenNoRecordExists() {
        cooldownService.setOrRefreshCooldown(PRODUCT);

        Optional<Cooldown> c = cooldownRepository.findByUrl(PRODUCT.url());
        assertTrue(c.isPresent());
        assertTrue(c.get().getLastSeen().isAfter(LocalDateTime.now().minus(interval, ChronoUnit.MILLIS)));
    }

    @Test
    void setOrRefreshCooldown_refreshesRecord_whenRecordExists() {
        Cooldown c = new Cooldown();
        c.setUrl(PRODUCT.url());
        c.setLastSeen(LocalDateTime.now().minusDays(10));
        cooldownRepository.save(c);

        cooldownService.setOrRefreshCooldown(PRODUCT);

        Optional<Cooldown> existingCooldown = cooldownRepository.findByUrl(PRODUCT.url());
        assertTrue(existingCooldown.isPresent());
        assertTrue(existingCooldown.get()
                .getLastSeen().isAfter(LocalDateTime.now().minus(interval, ChronoUnit.MILLIS)));
    }

    @Test
    void disable_createsDisabledRecord_whenNoRecordExists() {
        cooldownService.disable(PRODUCT);

        Optional<Cooldown> cd = cooldownRepository.findByUrl(PRODUCT.url());
        assertTrue(cd.isPresent());
        assertTrue(cd.get().isDisabled());
    }

    @Test
    void disable_setsRecordToDisabled_whenRecordExists() {
        Cooldown c = new Cooldown();
        c.setUrl(PRODUCT.url());
        cooldownRepository.save(c);

        cooldownService.disable(PRODUCT);

        Optional<Cooldown> cd = cooldownRepository.findByUrl(PRODUCT.url());
        assertTrue(cd.isPresent());
        assertTrue(cd.get().isDisabled());
    }
}
