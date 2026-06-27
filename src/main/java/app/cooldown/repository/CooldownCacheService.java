package app.cooldown.repository;

import app.cooldown.model.Cooldown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

public class CooldownCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CooldownCacheService.class);
    private static final String KEY_PREFIX = "cooldown:";

    private final RedisTemplate<String, Cooldown> redisTemplate;
    private final Duration ttl;

    public CooldownCacheService(RedisTemplate<String, Cooldown> redisTemplate,
                                @Value("${cooldown.interval-ms}") long interval) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMillis(interval);
    }

    public Optional<Cooldown> get(String url) {
        String key = KEY_PREFIX + url;
        try {
            Cooldown cached = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(cached);
        }
        catch (Exception e) {
            LOGGER.warn("Redis get failed for url={}, falling back to DB", url, e);
            return Optional.empty();
        }
    }

    public void put(Cooldown cooldown) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + cooldown.getUrl(), cooldown, ttl);
        }
        catch (Exception e) {
            LOGGER.warn("Redis put failed for url={}", cooldown.getUrl(), e);
        }
    }

    public void evict(String url) {
        try {
            redisTemplate.delete(KEY_PREFIX + url);
        }
        catch (Exception e) {
            LOGGER.warn("Redis evict failed for url={}", url, e);
        }
    }

}
