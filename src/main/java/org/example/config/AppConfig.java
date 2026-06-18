package org.example.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.checker.Checker;
import org.example.checker.impl.CooksEdgeChecker;
import org.example.checker.impl.SharpKnifeShopChecker;
import org.example.checker.impl.StaySharpChecker;
import org.example.dto.CheckerConfig;
import org.example.fetcher.HttpFetcher;
import org.example.notifier.Notifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@EnableScheduling
@Configuration
public class AppConfig {

    @Bean
    public List<Checker> checkers(ObjectMapper mapper, HttpFetcher fetcher, Notifier notifier)
            throws IOException {
        List<CheckerConfig> configs = mapper.readValue(
                new ClassPathResource("checkers.json").getInputStream(),
                new TypeReference<>() {
                }
        );

        Map<String, Function<CheckerConfig, Checker>> factories = Map.of(
                "cooksEdgeChecker", c -> new CooksEdgeChecker(fetcher, notifier, c),
                "sharpKnifeShopChecker", c -> new SharpKnifeShopChecker(fetcher, notifier, c),
                "staySharpChecker", c -> new StaySharpChecker(fetcher, notifier, c)
        );
        return configs.stream()
                .map(c -> Optional.ofNullable(factories.get(c.checker()))
                        .orElseThrow(() -> new IllegalArgumentException("Unknown checker: " + c.checker()))
                        .apply(c))
                .toList();
    }
}
