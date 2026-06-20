package app.notifier.cooldown.repository;

import app.notifier.cooldown.model.Cooldown;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CooldownRepository extends JpaRepository<Cooldown, Long> {

    Optional<Cooldown> findByUrl(String url);

    Optional<LocalDateTime> findCooldownSinceByUrl(String url);

    Optional<Boolean> findDisabledByUrl(String url);
}
