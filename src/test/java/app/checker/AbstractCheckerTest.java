package app.checker;

import app.cooldown.CooldownService;
import app.dto.CheckerConfig;
import app.dto.Product;
import app.fetcher.HttpFetcher;
import app.notifier.Notifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AbstractChecker}.
 * <p>
 * Since AbstractChecker is abstract, a minimal concrete subclass
 * ({@link TestableChecker}) is used to control exactly what
 * {@link AbstractChecker#getUnfilteredItemList(String)} returns for each test.
 */
@ExtendWith(MockitoExtension.class)
class AbstractCheckerTest {

    private static final String CHECKER_NAME = "Test Checker";
    private static final String CHECKER_URL = "https://example.com/search";

    @Mock
    private HttpFetcher httpFetcher;

    @Mock
    private Notifier notifier;

    @Mock
    private CooldownService cooldownService;

    private ResponseEntity<String> okResponse;

    @BeforeEach
    void setUp() {
        okResponse = ResponseEntity.ok("<html>irrelevant, subclass supplies products</html>");
    }

    // ---------------------------------------------------------------
    // Early-exit guards
    // ---------------------------------------------------------------

    @Test
    void check_doesNothing_whenResponseIsNull() {
        when(httpFetcher.fetch(CHECKER_URL)).thenReturn(null);
        AbstractChecker checker = newChecker(null, List.of(product("A")));

        checker.check();

        verifyNoInteractions(notifier, cooldownService);
    }

    @Test
    void check_doesNothing_whenStatusIsNot200() {
        ResponseEntity<String> notFound = ResponseEntity.status(HttpStatus.NOT_FOUND).body("nope");
        when(httpFetcher.fetch(CHECKER_URL)).thenReturn(notFound);
        AbstractChecker checker = newChecker(null, List.of(product("A")));

        checker.check();

        verifyNoInteractions(notifier, cooldownService);
    }

    @Test
    void check_doesNothing_whenUnfilteredListIsEmpty() {
        when(httpFetcher.fetch(CHECKER_URL)).thenReturn(okResponse);
        AbstractChecker checker = newChecker(null, List.of());

        checker.check();

        verifyNoInteractions(notifier, cooldownService);
    }

    @Test
    void check_doesNothing_whenRegexFilterExcludesAllProducts() {
        when(httpFetcher.fetch(CHECKER_URL)).thenReturn(okResponse);
        AbstractChecker checker = newChecker("^NoMatch$", List.of(product("A"), product("B")));

        checker.check();

        verifyNoInteractions(notifier, cooldownService);
    }

    @Test
    void check_doesNothing_whenAllProductsAreOnCooldown() {
        when(httpFetcher.fetch(CHECKER_URL)).thenReturn(okResponse);
        AbstractChecker checker = newChecker(null, List.of(product("A"), product("B")));
        when(cooldownService.filter(anyList())).thenReturn(List.of());

        checker.check();

        verifyNoInteractions(notifier);
        verify(cooldownService, never()).setOrRefreshCooldown(any());
    }

    // ---------------------------------------------------------------
    // Happy path
    // ---------------------------------------------------------------

    @Test
    void check_sendsNotificationAndRefreshesCooldown_forOffCooldownProducts() {
        Product a = product("A");
        Product b = product("B");
        when(httpFetcher.fetch(CHECKER_URL)).thenReturn(okResponse);
        AbstractChecker checker = newChecker(null, List.of(a, b));
        when(cooldownService.filter(List.of(a, b))).thenReturn(List.of(a, b));

        checker.check();

        verify(notifier).send(CHECKER_NAME + " is in stock with 2 items", List.of(a, b));
        verify(cooldownService).setOrRefreshCooldown(a);
        verify(cooldownService).setOrRefreshCooldown(b);
    }

    @Test
    void check_refreshesCooldown_onlyForNotifiedProducts_notAllFilteredProducts() {
        Product a = product("A");
        Product b = product("B");
        when(httpFetcher.fetch(CHECKER_URL)).thenReturn(okResponse);
        AbstractChecker checker = newChecker(null, List.of(a, b));
        // Only "a" survives the cooldown filter, even though both survive regex.
        when(cooldownService.filter(List.of(a, b))).thenReturn(List.of(a));

        checker.check();

        verify(notifier).send(anyString(), eq(List.of(a)));
        verify(cooldownService).setOrRefreshCooldown(a);
        verify(cooldownService, never()).setOrRefreshCooldown(b);
    }

    @Test
    void check_notificationMessage_includesCheckerNameAndCount() {
        Product a = product("A");
        when(httpFetcher.fetch(CHECKER_URL)).thenReturn(okResponse);
        AbstractChecker checker = newChecker(null, List.of(a));
        when(cooldownService.filter(List.of(a))).thenReturn(List.of(a));

        checker.check();

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notifier).send(messageCaptor.capture(), eq(List.of(a)));
        assertThat(messageCaptor.getValue()).isEqualTo(CHECKER_NAME + " is in stock with 1 items");
    }

    // ---------------------------------------------------------------
    // Regex filtering
    // ---------------------------------------------------------------

    @Test
    void check_passesAllProductsThrough_whenRegexFilterIsNull() {
        Product a = product("Widget");
        Product b = product("Gadget");
        when(httpFetcher.fetch(CHECKER_URL)).thenReturn(okResponse);
        AbstractChecker checker = newChecker(null, List.of(a, b));
        when(cooldownService.filter(List.of(a, b))).thenReturn(List.of(a, b));

        checker.check();

        verify(cooldownService).filter(List.of(a, b));
    }

    @Test
    void check_passesAllProductsThrough_whenRegexFilterIsBlank() {
        Product a = product("Widget");
        Product b = product("Gadget");
        when(httpFetcher.fetch(CHECKER_URL)).thenReturn(okResponse);
        AbstractChecker checker = newChecker("   ", List.of(a, b));
        when(cooldownService.filter(List.of(a, b))).thenReturn(List.of(a, b));

        checker.check();

        verify(cooldownService).filter(List.of(a, b));
    }

    @Test
    void check_onlyPassesMatchingProducts_whenRegexFilterIsSet() {
        Product matches = product("Special Edition Widget");
        Product doesNotMatch = product("Regular Gadget");
        when(httpFetcher.fetch(CHECKER_URL)).thenReturn(okResponse);
        AbstractChecker checker = newChecker("^Special.*", List.of(matches, doesNotMatch));
        when(cooldownService.filter(List.of(matches))).thenReturn(List.of(matches));

        checker.check();

        // cooldownService.filter must receive only the regex-matched product,
        // proving filtering order is: unfiltered -> regex -> cooldown.
        verify(cooldownService).filter(List.of(matches));
        verify(cooldownService, never()).filter(argThat(list -> list.contains(doesNotMatch)));
    }

    // ---------------------------------------------------------------
    // Simple accessors / delegation
    // ---------------------------------------------------------------

    @Test
    void getName_returnsCheckerConfigName() {
        AbstractChecker checker = newChecker(null, List.of());

        assertThat(checker.getName()).isEqualTo(CHECKER_NAME);
    }

    @Test
    void check_fetchesConfiguredUrl() {
        when(httpFetcher.fetch(CHECKER_URL)).thenReturn(okResponse);
        AbstractChecker checker = newChecker(null, List.of());

        checker.check();

        verify(httpFetcher).fetch(CHECKER_URL);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private AbstractChecker newChecker(String regexFilter, List<Product> itemsToReturn) {
        CheckerConfig config = new CheckerConfig(CHECKER_NAME, "test", CHECKER_URL, regexFilter);
        return new TestableChecker(httpFetcher, notifier, cooldownService, config, itemsToReturn);
    }

    private Product product(String name) {
        return new Product(name, "https://example.com/" + name);
    }

    /**
     * Minimal concrete subclass allowing tests to control exactly what
     * getUnfilteredItemList returns, independent of any real HTML parsing.
     */
    private static class TestableChecker extends AbstractChecker {

        private final List<Product> itemsToReturn;

        TestableChecker(HttpFetcher httpFetcher,
                        Notifier notifier,
                        CooldownService cooldownService,
                        CheckerConfig checkerConfig,
                        List<Product> itemsToReturn) {
            super(httpFetcher, notifier, cooldownService, checkerConfig);
            this.itemsToReturn = itemsToReturn;
        }

        @Override
        public List<Product> getUnfilteredItemList(String responseBody) {
            return itemsToReturn;
        }
    }
}