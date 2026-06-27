package app.fetcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service for fetching HTTP responses.
 */
@Service
public class HttpFetcher {

    private static final Logger logger = LoggerFactory.getLogger(HttpFetcher.class);

    private final HttpClient client;

    /**
     * Constructs a new HttpFetcher with a default timeout of 10 seconds.
     */
    public HttpFetcher() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Fetches an HTTP response from the specified URL.
     *
     * @param url the URL to fetch
     * @return the HttpResponse, or null if an error occurs
     */
    public HttpResponse<String> fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                logger.debug("Page returned status: {}. Url: {}", response.statusCode(), url);
            }
        } catch (IOException e) {
            logger.error("Url could not be fetched: {}", url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Fetch interrupted for url: {}", url, e);
        }
        return response;
    }
}
