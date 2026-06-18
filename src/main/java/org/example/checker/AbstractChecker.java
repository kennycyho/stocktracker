package org.example.checker;

import org.example.dto.CheckerConfig;
import org.example.dto.Product;
import org.example.fetcher.HttpFetcher;
import org.example.notifier.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks stock from search results page.
 */

public abstract class AbstractChecker implements Checker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractChecker.class);

    protected final HttpFetcher httpFetcher;
    protected final Notifier notifier;
    protected final CheckerConfig checkerConfig;

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
            LOGGER.info("Found items for product {}", checkerConfig.name());
            notifier.send(
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
            LOGGER.info("Server error while fetching {}: {}", checkerConfig.name(), response.statusCode());
        }
        else {
            LOGGER.error("Error while fetching {}: {}", checkerConfig.name(), response.statusCode());
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
