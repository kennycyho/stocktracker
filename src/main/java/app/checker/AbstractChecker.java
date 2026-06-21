package app.checker;

import app.dto.CheckerConfig;
import app.dto.Product;
import app.fetcher.HttpFetcher;
import app.notifier.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks stock from search results page.
 */

public abstract class AbstractChecker implements Checker {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final HttpFetcher httpFetcher;
    protected final Notifier notifier;
    protected final CheckerConfig checkerConfig;

    @Value("${app.notifier.recipient}")
    private String recipientEmail;

    protected AbstractChecker(HttpFetcher httpFetcher, Notifier notifier, CheckerConfig checkerConfig) {
        this.httpFetcher = httpFetcher;
        this.notifier = notifier;
        this.checkerConfig = checkerConfig;
    }

    public abstract List<Product> getUnfilteredItemList(HttpResponse<String> response);

    @Override
    public void check() {
        List<Product> productList = getFilteredItemList();
        if (!productList.isEmpty()) {
            logger.info("Found items for product {}", checkerConfig.name());
            notifier.send(recipientEmail,
                    checkerConfig.name() + " is in stock with " + productList.size() + " items",
                    productList);
        }
    }

    private List<Product> getFilteredItemList() {
        List<Product> filteredProductList = new ArrayList<>();
        HttpResponse<String> response = httpFetcher.fetch(checkerConfig.url());

        if (response.statusCode() == 200) {
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

    private List<Product> getAndFilterItemsList(HttpResponse<String> response) {
        List<Product> unfilteredProductList = getUnfilteredItemList(response);
        if (checkerConfig.regexFilter() != null && !checkerConfig.regexFilter().isBlank()) {
            return unfilteredProductList.stream()
                    .filter(item -> item.name().matches(checkerConfig.regexFilter()))
                    .toList();
        }
        return unfilteredProductList;
    }
}
