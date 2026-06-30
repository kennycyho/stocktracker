package app.cooldown;

import app.cooldown.model.Cooldown;
import app.cooldown.repository.CooldownRepository;
import app.dto.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class CooldownServiceIntegrationTests {

    @Autowired
    CooldownRepository cooldownRepository;

    @Autowired
    CooldownService cooldownService;

    @Autowired
    CooldownCacheService cacheService;

    @Autowired
    RedisTemplate<String, Cooldown> redisTemplate;

    @Value("${cooldown.interval-ms}")
    Long interval;

    private static final Product PRODUCT = new Product("Test Product", "http://www.example.com");

    @BeforeEach
    void setUp() {
        cooldownRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
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

    @Test
    void filter_returnsAllProducts_whenAllAreValid() {
        Product p1 = new Product("P1", "http://p1.com");
        Product p2 = new Product("P2", "http://p2.com");
        List<Product> products = Arrays.asList(p1, p2);

        List<Product> result = cooldownService.filter(products);

        assertEquals(2, result.size());
        assertTrue(result.contains(p1));
        assertTrue(result.contains(p2));
    }

    @Test
    void filter_returnsOnlyValidProducts_whenSomeAreInvalid() {
        Product pValid1 = new Product("Valid1", "http://valid1.com");
        Product pOnCooldown = new Product("OnCooldown", "http://oncooldown.com");
        Product pDisabled = new Product("Disabled", "http://disabled.com");
        Product pValid2 = new Product("Valid2", "http://valid2.com");

        Cooldown c1 = new Cooldown();
        c1.setUrl(pOnCooldown.url());
        c1.setLastSeen(LocalDateTime.now());
        cooldownRepository.save(c1);

        Cooldown c2 = new Cooldown();
        c2.setUrl(pDisabled.url());
        c2.setDisabled(true);
        cooldownRepository.save(c2);

        List<Product> products = Arrays.asList(pValid1, pOnCooldown, pDisabled, pValid2);
        List<Product> result = cooldownService.filter(products);

        assertEquals(2, result.size());
        assertTrue(result.contains(pValid1));
        assertTrue(result.contains(pValid2));
        assertFalse(result.contains(pOnCooldown));
        assertFalse(result.contains(pDisabled));
    }

    @Test
    void filter_returnsEmptyList_whenNoneAreValid() {
        Product p1 = new Product("P1", "http://p1.com");
        Product p2 = new Product("P2", "http://p2.com");

        Cooldown c1 = new Cooldown();
        c1.setUrl(p1.url());
        c1.setLastSeen(LocalDateTime.now());
        cooldownRepository.save(c1);

        Cooldown c2 = new Cooldown();
        c2.setUrl(p2.url());
        c2.setDisabled(true);
        cooldownRepository.save(c2);

        List<Product> products = Arrays.asList(p1, p2);
        List<Product> result = cooldownService.filter(products);

        assertTrue(result.isEmpty());
    }

    @Test
    void filter_returnsEmptyList_whenInputIsEmpty() {
        List<Product> result = cooldownService.filter(List.of());
        assertTrue(result.isEmpty());
    }

    // begin cache tests

    @Test
    void isValid_returnsFalse_whenOnCooldownInCacheOnly() {
        // Seed Redis directly — nothing in Postgres
        Cooldown c = new Cooldown();
        c.setUrl(PRODUCT.url());
        c.setLastSeen(LocalDateTime.now()); // on cooldown
        cacheService.put(c);

        assertFalse(cooldownService.isValid(PRODUCT));
    }

    @Test
    void isValid_returnsTrue_whenOffCooldownInCacheOnly() {
        Cooldown c = new Cooldown();
        c.setUrl(PRODUCT.url());
        c.setLastSeen(LocalDateTime.now().minusDays(10)); // off cooldown
        cacheService.put(c);

        assertTrue(cooldownService.isValid(PRODUCT));
    }

    @Test
    void isValid_populatesCache_whenFoundInDb() {
        Cooldown c = new Cooldown();
        c.setUrl(PRODUCT.url());
        c.setLastSeen(LocalDateTime.now().minusDays(10));
        cooldownRepository.save(c);

        cooldownService.isValid(PRODUCT); // triggers findCooldown → DB hit → cacheService.put

        assertTrue(cacheService.get(PRODUCT.url()).isPresent());
    }

    @Test
    void setOrRefreshCooldown_writesToCache() {
        cooldownService.setOrRefreshCooldown(PRODUCT);

        Optional<Cooldown> cached = cacheService.get(PRODUCT.url());
        assertTrue(cached.isPresent());
        assertFalse(cached.get().isDisabled());
    }
}
