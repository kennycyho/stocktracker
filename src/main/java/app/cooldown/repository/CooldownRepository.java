package app.cooldown.repository;

import app.cooldown.model.Cooldown;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CooldownRepository extends JpaRepository<Cooldown, Long> {

    Optional<Cooldown> findByUrl(String url);
}
