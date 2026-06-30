package app.cooldown.repository;

import app.cooldown.model.Cooldown;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for managing {@link Cooldown} entities.
 * Provides database access operations for cooldown tracking.
 */
public interface CooldownRepository extends JpaRepository<Cooldown, Long> {

    /**
     * Finds a cooldown entry by its associated URL.
     *
     * @param url the URL to search for
     * @return an Optional containing the cooldown if found, or empty otherwise
     */
    Optional<Cooldown> findByUrl(String url);
}
