package app.cooldown;

import app.cooldown.model.Cooldown;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CooldownCacheServiceIntegrationTests {

    public static final String TEST_URL = "http://example.com";
    public static final String TEST_KEY = "cooldown:" + TEST_URL;

    @Autowired
    private RedisTemplate<String, Cooldown> redisTemplate;

    @Autowired
    private CooldownCacheService cooldownCacheService;

    @BeforeEach
    public void setUp() {
        // Clear the cache before each test
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    public void testGetWhenCooldownExists() {
        Cooldown cooldown = new Cooldown();
        cooldown.setUrl(TEST_URL);
        redisTemplate.opsForValue().set(TEST_KEY, cooldown);

        Optional<Cooldown> cachedCooldown = cooldownCacheService.get(TEST_URL);
        assertTrue(cachedCooldown.isPresent());
        assertEquals(cooldown, cachedCooldown.get());
    }

    @Test
    public void testGetWhenCooldownDoesNotExist() {
        Optional<Cooldown> cachedCooldown = cooldownCacheService.get(TEST_URL);
        assertFalse(cachedCooldown.isPresent());
    }

    @Test
    public void testPut() {
        Cooldown cooldown = new Cooldown();
        cooldown.setUrl(TEST_URL);

        cooldownCacheService.put(cooldown);

        Optional<Cooldown> cachedCooldown = Optional.ofNullable(redisTemplate.opsForValue().get(TEST_KEY));
        assertTrue(cachedCooldown.isPresent());
        assertEquals(cooldown, cachedCooldown.get());
    }

    @Test
    public void testEvict() {
        Cooldown cooldown = new Cooldown();
        cooldown.setUrl(TEST_URL);
        redisTemplate.opsForValue().set(TEST_KEY, cooldown);

        Optional<Cooldown> cachedCooldown = Optional.ofNullable(redisTemplate.opsForValue().get(TEST_KEY));
        assertTrue(cachedCooldown.isPresent());

        cooldownCacheService.evict(TEST_URL);

        cachedCooldown = Optional.ofNullable(redisTemplate.opsForValue().get(TEST_KEY));
        assertFalse(cachedCooldown.isPresent());
    }

}
