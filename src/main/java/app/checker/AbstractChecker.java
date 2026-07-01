package app.checker;

import app.cooldown.CooldownService;
import app.dto.CheckerConfig;
import app.dto.Product;
import app.fetcher.HttpFetcher;
import app.notifier.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Abstract base class for checking stock from search results pages.
 * <p>
 * This class provides the core functionality for checking product availability:
 * <ul>
 *   <li>Fetching HTTP responses from configured URLs</li>
 *   <li>Parsing product information from response bodies</li>
 *   <li>Filtering products by regex patterns</li>
 *   <li>Managing cooldown periods to avoid duplicate notifications</li>
 *   <li>Sending notifications when products are in stock</li>
 * </ul>
 * <p>
 * Subclasses must implement {@link #getUnfilteredItemList(String)} to parse
 * product information from the specific HTML structure of their target websites.
 */
public abstract class AbstractChecker implements Checker {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final HttpFetcher httpFetcher;
    private final Notifier notifier;
    private final CooldownService cooldownService;
    private final CheckerConfig checkerConfig;

    /**
     * Constructs an instance of AbstractChecker with the provided dependencies.
     *
     * @param httpFetcher     The HTTP fetcher to use for fetching web pages.
     * @param notifier        The notifier to use for sending notifications.
     * @param cooldownService The cooldown service to use for filtering products.
     * @param checkerConfig   Configuration settings for the checker.
     */
    protected AbstractChecker(HttpFetcher httpFetcher,
                              Notifier notifier,
                              CooldownService cooldownService,
                              CheckerConfig checkerConfig) {
        this.httpFetcher = httpFetcher;
        this.notifier = notifier;
        this.cooldownService = cooldownService;
        this.checkerConfig = checkerConfig;
    }

    /**
     * Retrieves an unfiltered list of products from the search results page.
     *
     * @param responseBody The HTTP response body containing the search results page.
     * @return An unfiltered list of products.
     */
    public abstract List<Product> getUnfilteredItemList(String responseBody);

    /**
     * Checks for available stock and sends notifications if any items are in stock.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Fetches the HTTP response body from the configured URL</li>
     *   <li>Retrieves an unfiltered list of products from the response</li>
     *   <li>Filters products by the configured regex filter (if any)</li>
     *   <li>Filters products by cooldown status</li>
     *   <li>Sends a notification for products that are off cooldown</li>
     *   <li>Refreshes cooldown for notified products</li>
     * </ol>
     */
    @Override
    public void check() {
        String responseBody = fetchResponseBody();
        if (responseBody == null) return;

        List<Product> unfilteredProductList = getUnfilteredItemList(responseBody);
        if (unfilteredProductList.isEmpty()) return;
        logger.debug("Found {} items for product {}", unfilteredProductList.size(), checkerConfig.name());

        List<Product> filteredProductList = filterByRegex(unfilteredProductList);
        logger.debug("Filtered list contains {} items for product {}", filteredProductList.size(), checkerConfig.name());
        if (filteredProductList.isEmpty()) return;

        List<Product> offCooldownProducts = cooldownService.filter(filteredProductList);
        if (offCooldownProducts.isEmpty()) return;

        notifyAndRefreshCooldown(offCooldownProducts);
    }

    /**
     * Retrieves the name of the checker.
     *
     * @return The name of the checker.
     */
    public String getName() {
        return checkerConfig.name();
    }

    /**
     * Retrieves the checker configuration.
     *
     * @return The checker configuration.
     */
    protected CheckerConfig getCheckerConfig() {
        return checkerConfig;
    }

    /**
     * Fetches the HTTP response body from the configured URL.
     * Handles different HTTP status codes and logs appropriate messages.
     *
     * @return The response body if status is 200, null otherwise.
     */
    private String fetchResponseBody() {
        String responseBody = null;
        ResponseEntity<String> response = httpFetcher.fetch(checkerConfig.url());

        if (response == null) {
            logger.warn("Fetch returned null for {}, skipping", checkerConfig.name());
        }
        else if (response.getStatusCode().value() == 200) {
            responseBody = response.getBody();
        }
        else if (response.getStatusCode().value() >= 500) {
            logger.info("Server error while fetching {}: {}", checkerConfig.name(), response.getStatusCode().value());
        }
        else {
            logger.error("Error while fetching {}: {}", checkerConfig.name(), response.getStatusCode().value());
        }
        return responseBody;
    }

    /**
     * Filters the list of products based on checker config regex.
     *
     * @param unfilteredProductList The unfiltered list of products.
     * @return A list of filtered products.
     */
    private List<Product> filterByRegex(List<Product> unfilteredProductList) {
        if (checkerConfig.regexFilter() != null && !checkerConfig.regexFilter().isBlank()) {
            return unfilteredProductList.stream()
                    .filter(item -> item.name().matches(checkerConfig.regexFilter()))
                    .toList();
        }
        return unfilteredProductList;
    }

    /**
     * Sends a notification for the given products and refreshes their cooldowns.
     *
     * @param products The products to notify about and refresh cooldowns for.
     */
    private void notifyAndRefreshCooldown(List<Product> products) {
        logger.info("Sending notification for {} items", products.size());
        notifier.send(checkerConfig.name() + " is in stock with " + products.size() + " items", products);
        products.forEach(cooldownService::setOrRefreshCooldown);
    }
}
