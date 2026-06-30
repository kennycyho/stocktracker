package app.config;

import app.cooldown.model.Cooldown;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration for Redis.
 */
@Configuration
public class RedisConfig {

    /**
     * Redis configuration.
     *
     * @return RedisTemplate instance for Redis operations.
     */
    @Bean
    public RedisTemplate<String, Cooldown> redisTemplate(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<Cooldown> serializer = new Jackson2JsonRedisSerializer<>(mapper, Cooldown.class);

        RedisTemplate<String, Cooldown> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        return template;
    }
}
