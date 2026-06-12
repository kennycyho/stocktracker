package org.example.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.checker.CooksEdgeChecker;
import org.example.checker.SharpKnifeShopChecker;
import org.example.checker.StaySharpChecker;
import org.example.checker.StockChecker;
import org.example.fetcher.HttpFetcher;
import org.example.model.CheckerConfig;
import org.example.notifier.StockNotifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@EnableScheduling
@Configuration
public class AppConfig {

    @Bean
    public List<StockChecker> checkers(ObjectMapper mapper, HttpFetcher fetcher, StockNotifier notifier)
            throws IOException {
        List<CheckerConfig> configs = mapper.readValue(
                new ClassPathResource("checkers.json").getInputStream(),
                new TypeReference<>() {
                }
        );

        Map<String, Function<CheckerConfig, StockChecker>> factories = Map.of(
                "cooksEdgeChecker", c -> new CooksEdgeChecker(fetcher, notifier, c),
                "sharpKnifeShopChecker", c -> new SharpKnifeShopChecker(fetcher, notifier, c),
                "staySharpChecker", c -> new StaySharpChecker(fetcher, notifier, c)
        );
        return configs.stream()
                .map(c -> factories.get(c.checker()).apply(c))
                .toList();
    }
}
