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

    private final String testUrl = "https://example.com";
    private final String testBody = "Hello World";

    @BeforeEach
    void setUp() {
        // Setup the fluent API chain for RestClient
        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void fetch_returnsResponseEntity_whenRequestIsSuccessful() {
        // Arrange
        ResponseEntity<String> expectedResponse = new ResponseEntity<>(testBody, HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<String> actualResponse = httpFetcher.fetch(testUrl);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
        assertEquals(testBody, actualResponse.getBody());
        verify(restClient).get();
        verify(requestHeadersUriSpec).uri(testUrl);
    }

    @Test
    void fetch_returnsResponseEntity_whenRequestReturnsErrorStatus() {
        // Arrange
        ResponseEntity<String> expectedResponse = new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
        when(responseSpec.toEntity(String.class)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<String> actualResponse = httpFetcher.fetch(testUrl);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(HttpStatus.NOT_FOUND, actualResponse.getStatusCode());
        assertEquals("Not Found", actualResponse.getBody());
    }

    @Test
    void fetch_returnsNull_whenExceptionOccurs() {
        // Arrange
        when(responseSpec.toEntity(String.class)).thenThrow(new RuntimeException("Network error"));

        // Act
        ResponseEntity<String> actualResponse = httpFetcher.fetch(testUrl);

        // Assert
        assertNull(actualResponse);
    }
}
