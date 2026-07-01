package app.fetcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpFetcherTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private HttpFetcher httpFetcher;

    private static final String TEST_URL = "https://example.com";
    private static final String TEST_BODY = "Hello World";

    @BeforeEach
    void setUp() {
        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void fetch_returnsResponse_whenStatusIs200() {
        ResponseEntity<String> response = new ResponseEntity<>(TEST_BODY, HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(response);

        ResponseEntity<String> result = httpFetcher.fetch(TEST_URL);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(TEST_BODY, result.getBody());
        verify(restClient).get();
        verify(requestHeadersUriSpec).uri(TEST_URL);
    }

    @Test
    void fetch_returnsResponse_whenStatusIs4xx() {
        ResponseEntity<String> response = new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
        when(responseSpec.toEntity(String.class)).thenReturn(response);

        ResponseEntity<String> result = httpFetcher.fetch(TEST_URL);

        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void fetch_returnsResponse_whenStatusIs5xx() {
        ResponseEntity<String> response = new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
        when(responseSpec.toEntity(String.class)).thenReturn(response);

        ResponseEntity<String> result = httpFetcher.fetch(TEST_URL);

        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void fetch_returnsNull_whenExceptionThrown() {
        when(responseSpec.toEntity(String.class)).thenThrow(new RuntimeException("Network error"));

        ResponseEntity<String> result = httpFetcher.fetch(TEST_URL);

        assertNull(result);
    }
}
