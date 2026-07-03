package app.fetcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpFetcherTest {

    private static final String TEST_URL = "https://example.com/api";
    private static final String RESPONSE_BODY = "response body";

    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private HttpFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new HttpFetcher(restClient);
    }

    @Test
    void fetch_returnsResponseEntity_whenUrlIsValid() {
        ResponseEntity<String> expectedResponse = ResponseEntity.ok(RESPONSE_BODY);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(TEST_URL)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(expectedResponse);

        ResponseEntity<String> actualResponse = fetcher.fetch(TEST_URL);

        assertNotNull(actualResponse);
        assertSame(expectedResponse, actualResponse);
        verify(restClient).get();
        verify(requestHeadersUriSpec).uri(TEST_URL);
        verify(requestHeadersUriSpec).retrieve();
        verify(responseSpec).toEntity(String.class);
    }

    @Test
    void fetch_returnsNull_whenRestClientThrowsException() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(TEST_URL)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenThrow(new RuntimeException("Connection failed"));

        ResponseEntity<String> actualResponse = fetcher.fetch(TEST_URL);

        assertNull(actualResponse);
        verify(restClient).get();
        verify(requestHeadersUriSpec).uri(TEST_URL);
        verify(requestHeadersUriSpec).retrieve();
        verify(responseSpec).toEntity(String.class);
    }
}
