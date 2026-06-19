package org.example.notifier.cooldown;

import org.example.dto.Product;
import org.example.notifier.cooldown.model.Cooldown;
import org.example.notifier.cooldown.repository.CooldownRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Optional<LocalDateTime> last_cooldown = cooldownRepository.findCooldownSinceByUrl(product.url());
        return last_cooldown.isEmpty() // not yet tracked
                || last_cooldown.get().isBefore(LocalDateTime.now().minus(interval, ChronoUnit.MILLIS));
    }

    public boolean isDisabled(Product product) {
        Optional<Boolean> disabledOptional = cooldownRepository.findDisabledByUrl(product.url());
        return disabledOptional.isPresent() && disabledOptional.get();
    }

    @Transactional
    public void setOrRefreshCooldown(Product product) {
        Cooldown newCooldown = cooldownRepository.findByUrl(product.url()).orElse(new Cooldown(product.url()));
        newCooldown.setCooldownSince(LocalDateTime.now());
        cooldownRepository.save(newCooldown);
    }

    public void disable(Product product) {
        Cooldown newCooldown = cooldownRepository.findByUrl(product.url()).orElse(new Cooldown(product.url()));
        newCooldown.setDisabled(true);
        cooldownRepository.save(newCooldown);
    }

}
