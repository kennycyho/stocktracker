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

    public boolean isValid(Product product) {
        Optional<Cooldown> cooldownOptional = cooldownRepository.findByUrl(product.url());
        return cooldownOptional.isEmpty() // never seen before
                || isOffCooldown(cooldownOptional.get()) && !cooldownOptional.get().isDisabled();
    }

    public void setOrRefreshCooldown(Product product) {
        Cooldown cooldown = findOrCreate(product.url());
        cooldown.setLastSeen(LocalDateTime.now());
        cooldownRepository.save(cooldown);
    }

    public void disable(Product product) {
        Cooldown cooldown = findOrCreate(product.url());
        cooldown.setDisabled(true);
        cooldownRepository.save(cooldown);
    }

    private boolean isOffCooldown(Cooldown cooldown) {
        return cooldown.getLastSeen().isBefore(LocalDateTime.now().minus(interval, ChronoUnit.MILLIS));
    }

    private Cooldown findOrCreate(String url) {
        return cooldownRepository.findByUrl(url).orElseGet(() -> {
            Cooldown c = new Cooldown();
            c.setUrl(url);
            return c;
        });
    }

}
