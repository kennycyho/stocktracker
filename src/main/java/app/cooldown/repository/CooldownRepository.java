package app.cooldown.repository;

import app.cooldown.model.Cooldown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CooldownRepository extends JpaRepository<Cooldown, Long> {

    Optional<Cooldown> findByUrl(String url);

    @Query("SELECT c.lastSeen FROM Cooldown c WHERE c.url = :url")
    Optional<LocalDateTime> findLastSeenByUrl(String url);

    @Query("SELECT c.disabled FROM Cooldown c WHERE c.url = :url")
    Optional<Boolean> findDisabledByUrl(String url);
}
