package app;

import app.cooldown.CooldownService;
import app.cooldown.repository.CooldownRepository;
import app.dto.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CooldownServiceUnitTests {

    @Mock
    private CooldownRepository cooldownRepository;

    private CooldownService cooldownService;

    Long INTERVAL_MS = 604800000L;
    Product PRODUCT = new Product("Test Product", "http://www.example.com");

    @BeforeEach
    void setUp() {
        cooldownService = new CooldownService(cooldownRepository, INTERVAL_MS);
    }

    @Test
    void isOffCooldown_returnsTrue_whenNoRecordExists() {
        when(cooldownRepository.findLastSeenByUrl(PRODUCT.url())).thenReturn(Optional.empty());
        assertTrue(cooldownService.isOffCooldown(PRODUCT));
    }

    @Test
    void isOffCooldown_returnsTrue_whenCooldownHasExpired() {
        when(cooldownRepository.findLastSeenByUrl(PRODUCT.url()))
                .thenReturn(Optional.of(LocalDateTime.now().minus(Duration.ofMillis(INTERVAL_MS * 2))));
        assertTrue(cooldownService.isOffCooldown(PRODUCT));
    }

    @Test
    void isOffCooldown_returnsFalse_whenCooldownIsActive() {
        when(cooldownRepository.findLastSeenByUrl(PRODUCT.url()))
                .thenReturn(Optional.of(LocalDateTime.now()));
        assertFalse(cooldownService.isOffCooldown(PRODUCT));
    }

    @Test
    void isDisabled_returnsFalse_whenNoRecordExists() {
        when(cooldownRepository.findDisabledByUrl(PRODUCT.url())).thenReturn(Optional.empty());
        assertFalse(cooldownService.isDisabled(PRODUCT));
    }

    @Test
    void isDisabled_returnsFalse_whenRecordExistsButNotDisabled() {
        when(cooldownRepository.findDisabledByUrl(PRODUCT.url())).thenReturn(Optional.of(false));
        assertFalse(cooldownService.isDisabled(PRODUCT));
    }

    @Test
    void isDisabled_returnsTrue_whenRecordExistsAndIsDisabled() {
        when(cooldownRepository.findDisabledByUrl(PRODUCT.url())).thenReturn(Optional.of(true));
        assertTrue(cooldownService.isDisabled(PRODUCT));
    }
}
