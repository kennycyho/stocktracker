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
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
                    headers.set(HttpHeaders.ACCEPT,
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
                    headers.set(HttpHeaders.ACCEPT_LANGUAGE,
                            "en-US,en;q=0.9");
                    headers.set("Sec-CH-UA",
                            "\"Not-A.Brand\";v=\"99\", \"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\"");
                    headers.set("Sec-CH-UA-Mobile", "?0");
                    headers.set("Sec-CH-UA-Platform", "\"Windows\"");
                    headers.set("Sec-Fetch-Dest", "document");
                    headers.set("Sec-Fetch-Mode", "navigate");
                    headers.set("Sec-Fetch-Site", "none");
                    headers.set("Sec-Fetch-User", "?1");
                    headers.set("Upgrade-Insecure-Requests", "1");
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
