package app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    
    private static final Logger RESTCLIENT_LOGGER = LoggerFactory.getLogger("app.fetcher.RestClient");

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
                .defaultHeaders(headers -> {
                    headers.set(HttpHeaders.USER_AGENT,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0");
                    headers.set(HttpHeaders.ACCEPT,
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
                    headers.set(HttpHeaders.ACCEPT_LANGUAGE,
                            "en-US,en;q=0.5");
                    headers.set(HttpHeaders.ACCEPT_ENCODING, "gzip");
                    headers.set("DNT", "1");
                    headers.set(HttpHeaders.CONNECTION, "keep-alive");
                    headers.set("Upgrade-Insecure-Requests", "1");
                    headers.set("Sec-Fetch-Dest", "document");
                    headers.set("Sec-Fetch-Mode", "navigate");
                    headers.set("Sec-Fetch-Site", "none");
                    headers.set("Sec-Fetch-User", "?1");
                    headers.set("Priority", "u=1");
                })
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
