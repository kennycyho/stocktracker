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
    private final Long interval;

    /**
     * Constructs a new instance of CooldownService with the given repository and cooldown interval.
     *
     * @param cooldownRepository the repository to interact with the cooldown data
     * @param interval           the cooldown interval in milliseconds
     */
    public CooldownService(CooldownRepository cooldownRepository,
                           @Value("${cooldown.interval-ms}") Long interval) {
        this.cooldownRepository = cooldownRepository;
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
        Optional<Cooldown> cooldownOptional = cooldownRepository.findByUrl(product.url());
        return cooldownOptional.isEmpty()
                || !cooldownOptional.get().isDisabled() && isOffCooldown(cooldownOptional.get());
    }

    /**
     * Sets or refreshes the cooldown for a given product.
     *
     * @param product the product to set or refresh the cooldown for
     */
    public void setOrRefreshCooldown(Product product) {
        Cooldown cooldown = findOrInstantiate(product.url());
        cooldown.setLastSeen(LocalDateTime.now());
        cooldownRepository.save(cooldown);
    }

    /**
     * Disables the notifications for a given product.
     *
     * @param product the product to disable the notifications for
     */
    public void disable(Product product) {
        Cooldown cooldown = findOrInstantiate(product.url());
        cooldown.setDisabled(true);
        cooldownRepository.save(cooldown);
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

    /**
     * Checks if a cooldown is expired based on the last seen time and the configured interval.
     *
     * @param cooldown the cooldown to check
     * @return true if the cooldown is expired, false otherwise
     */
    private boolean isOffCooldown(Cooldown cooldown) {
        return cooldown.getLastSeen().isBefore(LocalDateTime.now().minus(interval, ChronoUnit.MILLIS));
    }

    /**
     * Finds or creates a cooldown for a given URL.
     *
     * @param url the URL of the product
     * @return the found or created cooldown
     */
    private Cooldown findOrInstantiate(String url) {
        return cooldownRepository.findByUrl(url).orElseGet(() -> {
            Cooldown c = new Cooldown();
            c.setUrl(url);
            return c;
        });
    }

}
