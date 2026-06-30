package app.cooldown;

import app.cooldown.model.Cooldown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Service for managing cooldowns in Redis.
 */
@Service
public class CooldownCacheService {

    private final Logger logger = LoggerFactory.getLogger(CooldownCacheService.class);
    private static final String KEY_PREFIX = "cooldown:";

    private final RedisTemplate<String, Cooldown> redisTemplate;
    private final Duration ttl;

    /**
     * Constructs a new CooldownCacheService.
     *
     * @param redisTemplate the Redis template for interacting with Redis
     * @param interval      the cooldown interval in milliseconds
     */
    public CooldownCacheService(RedisTemplate<String, Cooldown> redisTemplate,
                                @Value("${cooldown.interval-ms}") long interval) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMillis(interval);
    }

    /**
     * Retrieves a cooldown from Redis.
     *
     * @param url the URL of the cooldown
     * @return an Optional containing the Cooldown if found, or an empty Optional otherwise
     */
    public Optional<Cooldown> get(String url) {
        String key = KEY_PREFIX + url;
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key));
        }
        catch (Exception e) {
            logger.warn("Redis get failed for url={}, falling back to DB", url, e);
            return Optional.empty();
        }
    }

    /**
     * Puts a cooldown into Redis.
     *
     * @param cooldown the Cooldown to put
     */
    public void put(Cooldown cooldown) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + cooldown.getUrl(), cooldown, ttl);
        }
        catch (Exception e) {
            logger.warn("Redis put failed for url={}", cooldown.getUrl(), e);
        }
    }

    /**
     * Evicts a cooldown from Redis.
     *
     * @param url the URL of the cooldown to evict
     */
    public void evict(String url) {
        try {
            redisTemplate.delete(KEY_PREFIX + url);
        }
        catch (Exception e) {
            logger.warn("Redis evict failed for url={}", url, e);
        }
    }

}
