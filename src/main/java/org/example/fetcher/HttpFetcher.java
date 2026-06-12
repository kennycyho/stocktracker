package org.example.fetcher;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class HttpFetcher {

    private static final System.Logger logger = System.getLogger(HttpFetcher.class.getName());

    private final HttpClient client;

    public HttpFetcher() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public HttpResponse<String> fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                logger.log(System.Logger.Level.ERROR, String.format(
                        "Page returned status: %d. Url: %s", response.statusCode(), url));
            }
        }
        catch (IOException | InterruptedException e) {
            logger.log(System.Logger.Level.ERROR, "Url could not be fetched: " + url, e);
        }
        return response;
    }
}

