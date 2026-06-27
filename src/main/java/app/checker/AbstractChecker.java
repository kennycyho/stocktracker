package app.checker;

import app.cooldown.CooldownService;
import app.dto.CheckerConfig;
import app.dto.Product;
import app.fetcher.HttpFetcher;
import app.notifier.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for checking stock from search results pages.
 */
public abstract class AbstractChecker implements Checker {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final HttpFetcher httpFetcher;
    protected final Notifier notifier;
    protected final CheckerConfig checkerConfig;
    private final CooldownService cooldownService;

    /**
     * Constructs an instance of AbstractChecker with the provided dependencies.
     *
     * @param httpFetcher   The HTTP fetcher to use for fetching web pages.
     * @param notifier      The notifier to use for sending notifications.
     * @param checkerConfig Configuration settings for the checker.
     */
    protected AbstractChecker(HttpFetcher httpFetcher, Notifier notifier, CheckerConfig checkerConfig, CooldownService cooldownService) {
        this.httpFetcher = httpFetcher;
        this.notifier = notifier;
        this.checkerConfig = checkerConfig;
        this.cooldownService = cooldownService;
    }

    /**
     * Retrieves an unfiltered list of products from the search results page.
     *
     * @param response The HTTP response containing the search results page.
     * @return An unfiltered list of products.
     */
    public abstract List<Product> getUnfilteredItemList(HttpResponse<String> response);

    /**
     * Checks for available stock and sends notifications if any items are in stock.
     */
    @Override
    public void check() {
        List<Product> productList = getFilteredItemList();
        if (!productList.isEmpty()) {
            logger.info("Found items for product {}", checkerConfig.name());

            List<Product> offCooldownProducts = cooldownService.filter(productList);

            notifier.send(checkerConfig.name() + " is in stock with " + offCooldownProducts.size() + " items",
                    offCooldownProducts);

            offCooldownProducts.forEach(cooldownService::setOrRefreshCooldown);
        }
    }

    /**
     * Handles http response status codes then retrieves and filters the list of products.
     *
     * @return A filtered list of products.
     */
    private List<Product> getFilteredItemList() {
        List<Product> filteredProductList = new ArrayList<>();
        HttpResponse<String> response = httpFetcher.fetch(checkerConfig.url());

        if (response == null) {
            logger.warn("Fetch returned null for {}, skipping", checkerConfig.name());
        }
        else if (response.statusCode() == 200) {
            filteredProductList.addAll(getAndFilterItemsList(response));
        }
        else if (response.statusCode() >= 500) {
            logger.info("Server error while fetching {}: {}", checkerConfig.name(), response.statusCode());
        }
        else {
            logger.error("Error while fetching {}: {}", checkerConfig.name(), response.statusCode());
        }
        return filteredProductList;
    }

    /**
     * Retrieves and filters the list of products from the HTTP response based on checker config regex.
     *
     * @param response The HTTP response containing the search results page.
     * @return A list of filtered products.
     */
    private List<Product> getAndFilterItemsList(HttpResponse<String> response) {
        List<Product> unfilteredProductList = getUnfilteredItemList(response);
        if (checkerConfig.regexFilter() != null && !checkerConfig.regexFilter().isBlank()) {
            return unfilteredProductList.stream()
                    .filter(item -> item.name().matches(checkerConfig.regexFilter()))
                    .toList();
        }
        return unfilteredProductList;
    }

    /**
     * Retrieves the name of the checker.
     *
     * @return The name of the checker.
     */
    public String getName() {
        return checkerConfig.name();
    }
}
