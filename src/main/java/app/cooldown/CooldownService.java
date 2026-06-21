package app.cooldown;

import app.cooldown.model.Cooldown;
import app.cooldown.repository.CooldownRepository;
import app.dto.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class CooldownService {

    private final CooldownRepository cooldownRepository;
    private final Long interval;

    public CooldownService(CooldownRepository cooldownRepository,
                           @Value("${cooldown.interval-ms}") Long interval) {
        this.cooldownRepository = cooldownRepository;
        this.interval = interval;
    }

    public boolean isOffCooldown(Product product) {
        Optional<LocalDateTime> lastSeen = cooldownRepository.findLastSeenByUrl(product.url());
        return lastSeen.isEmpty()
                || lastSeen.get().isBefore(LocalDateTime.now().minus(interval, ChronoUnit.MILLIS));
    }

    public boolean isDisabled(Product product) {
        Optional<Boolean> disabledOptional = cooldownRepository.findDisabledByUrl(product.url());
        return disabledOptional.isPresent() && disabledOptional.get();
    }

    public void setOrRefreshCooldown(Product product) {
        Cooldown cooldown = cooldownRepository.findByUrl(product.url())
                .orElseGet(() -> {
                    Cooldown c = new Cooldown();
                    c.setUrl(product.url());
                    return c;
                });
        cooldown.setLastSeen(LocalDateTime.now());
        cooldownRepository.save(cooldown);
    }

    public void disable(Product product) {
        Cooldown cooldown = cooldownRepository.findByUrl(product.url())
                .orElseGet(() -> {
                    Cooldown c = new Cooldown();
                    c.setUrl(product.url());
                    return c;
                });
        cooldown.setDisabled(true);
        cooldownRepository.save(cooldown);
    }

}
