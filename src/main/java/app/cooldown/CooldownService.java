package app.cooldown;

import app.cooldown.model.Cooldown;
import app.cooldown.repository.CooldownRepository;
import app.dto.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service class responsible for managing cooldowns for products.
 */
@Service
public class CooldownService {

    private final CooldownRepository cooldownRepository;
    private final CooldownCacheService cacheService;
    private final Long interval;

    /**
     * Constructs a new instance of CooldownService with the given repository and cooldown interval.
     *
     * @param cooldownRepository the repository to interact with the cooldown data
     * @param interval           the cooldown interval in milliseconds
     */
    public CooldownService(
            CooldownRepository cooldownRepository,
            CooldownCacheService cacheService,
            @Value("${cooldown.interval-ms}") Long interval
    ) {
        this.cooldownRepository = cooldownRepository;
        this.cacheService = cacheService;
        this.interval = interval;
    }

    /**
     * Checks if a product is valid based on its last seen time, whether it's disabled, or if there is no record in the
     * cooldown database.
     *
     * @param product the product to check
     * @return true if the product is valid, false otherwise
     */
    public boolean isValid(Product product) {
        Optional<Cooldown> cooldown = findCooldown(product.url());
        return cooldown.map(this::isValidCooldown).orElse(true);
    }

    /**
     * Sets or refreshes the cooldown for a given product.
     *
     * @param product the product to set or refresh the cooldown for
     */
    public void setOrRefreshCooldown(Product product) {
        Cooldown cooldown = fetchOrInstantiate(product.url());
        cooldown.setLastSeen(LocalDateTime.now());
        cooldownRepository.save(cooldown);
        cacheService.put(cooldown);
    }

    /**
     * Disables the notifications for a given product.
     *
     * @param product the product to disable the notifications for
     */
    public void disable(Product product) {
        Cooldown cooldown = fetchOrInstantiate(product.url());
        cooldown.setDisabled(true);
        cooldownRepository.save(cooldown);
        cacheService.put(cooldown);
    }

    /**
     * Filters a list of products, returning only those that are valid based on their last seen time and whether they are disabled.
     *
     * @param products the list of products to filter
     * @return a filtered list of valid products
     */
    public List<Product> filter(List<Product> products) {
        return products.stream()
                .filter(this::isValid)
                .toList();
    }

    private Optional<Cooldown> findCooldown(String url) {
        Optional<Cooldown> cached = cacheService.get(url);
        if (cached.isPresent()) return cached;

        Optional<Cooldown> stored = cooldownRepository.findByUrl(url);
        stored.ifPresent(cacheService::put);
        return stored;
    }

    /**
     * Fetches from database or creates a cooldown for a given URL.
     *
     * @param url the URL of the product
     * @return the fetched or created cooldown
     */
    private Cooldown fetchOrInstantiate(String url) {
        return cooldownRepository.findByUrl(url)
                .orElseGet(() -> {
                    Cooldown c = new Cooldown();
                    c.setUrl(url);
                    return c;
                });
    }

    private boolean isValidCooldown(Cooldown cooldown) {
        if (cooldown.isDisabled()) return false;
        return cooldown.getLastSeen().isBefore(LocalDateTime.now().minus(interval, ChronoUnit.MILLIS));
    }

}
