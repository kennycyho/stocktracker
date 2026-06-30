package app.fetcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service for fetching HTTP responses.
 */
@Service
public class HttpFetcher {

    private static final Logger logger = LoggerFactory.getLogger(HttpFetcher.class);

    private final RestClient restClient;

    /**
     * Constructs a new HttpFetcher with the provided RestClient.
     *
     * @param restClient the RestClient to use for fetching
     */
    public HttpFetcher(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Fetches an HTTP response from the specified URL.
     *
     * @param url the URL to fetch
     * @return the ResponseEntity, or null if an error occurs
     */
    public ResponseEntity<String> fetch(String url) {
        ResponseEntity<String> response = null;
        try {
            response = restClient.get().uri(url).retrieve().toEntity(String.class);
            if (response.getStatusCode().value() >= 400) {
                logger.debug("Page returned status: {}. Url: {}", response.getStatusCode(), url);
            }
        }
        catch (Exception e) {
            logger.error("Url could not be fetched: {}", url, e);
        }
        return response;
    }
}
