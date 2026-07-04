package app.config;

import app.checker.Checker;
import app.checker.impl.CooksEdgeChecker;
import app.checker.impl.SharpKnifeShopChecker;
import app.checker.impl.StaySharpChecker;
import app.cooldown.CooldownService;
import app.dto.CheckerConfig;
import app.fetcher.HttpFetcher;
import app.notifier.Notifier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Configuration class for setting up the application.
 */
@EnableScheduling
@Configuration
public class AppConfig {

    private static final Logger RESTCLIENT_LOGGER = LoggerFactory.getLogger("app.fetcher.RestClient");

    /**
     * Creates a list of checkers based on configuration and dependencies.
     *
     * @param mapper   the object mapper for JSON processing
     * @param fetcher  the HTTP fetcher for fetching data
     * @param notifier the notifier for sending notifications
     * @return a list of configured checkers
     * @throws IOException if an I/O error occurs while reading the configuration file
     */
    @Bean
    public List<Checker> checkers(ObjectMapper mapper,
                                  HttpFetcher fetcher,
                                  Notifier notifier,
                                  CooldownService cooldownService,
                                  @Value("${app.checkers-file}") String checkersFilePath) throws IOException {
        List<CheckerConfig> configList = mapper.readValue(
                new FileSystemResource(checkersFilePath).getInputStream(),
                new TypeReference<>() {
                });

        Map<String, Function<CheckerConfig, Checker>> checkFactory = Map.of(
                "cooksEdgeChecker", c ->
                        new CooksEdgeChecker(fetcher, notifier, cooldownService, c),
                "sharpKnifeShopChecker", c ->
                        new SharpKnifeShopChecker(fetcher, notifier, cooldownService, c),
                "staySharpChecker", c ->
                        new StaySharpChecker(fetcher, notifier, cooldownService, c)
        );
        return configList.stream()
                .map(c -> Optional.ofNullable(checkFactory.get(c.checker()))
                        .orElseThrow(() -> new IllegalArgumentException("Unknown checker: " + c.checker()))
                        .apply(c))
                .toList();
    }

    /**
     * Configures a RestClient with error handling for HTTP status codes.
     * <p>
     * Logs client errors (4xx) and server errors (5xx) with appropriate severity levels.
     *
     * @param builder the RestClient builder to configure
     * @return a configured RestClient instance
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .defaultStatusHandler(HttpStatusCode::isError,
                        (request, response) -> {
                            if (response.getStatusCode().is4xxClientError()) {
                                RESTCLIENT_LOGGER.error("Client error when fetching {}: {}",
                                        request.getURI(), response.getStatusCode());
                            }
                            else {
                                RESTCLIENT_LOGGER.info("Server error when fetching {}: {}",
                                        request.getURI(), response.getStatusCode());
                            }
                        })
                .build();
    }
}
