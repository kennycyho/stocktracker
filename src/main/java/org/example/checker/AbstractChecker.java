package org.example.checker;

import org.example.fetcher.HttpFetcher;
import org.example.model.CheckerConfig;
import org.example.model.Item;
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

    public abstract List<Item> getUnfilteredItemList(HttpResponse<String> response);

    @Override
    public void check() {
        List<Item> itemList = getFilteredItemList();
        if (!itemList.isEmpty()) {
            LOGGER.info("Found items for product {}", checkerConfig.name());
            notifier.send(
                    checkerConfig.name() + " is in stock with " + itemList.size() + " items",
                    itemList);
        }
    }

    private List<Item> getFilteredItemList() {
        List<Item> filteredItemList = new ArrayList<>();
        HttpResponse<String> response = httpFetcher.fetch(checkerConfig.url());

        if (response.statusCode() == 200) {
            filteredItemList.addAll(getAndFilterItemsList(response));
        }
        else if (response.statusCode() >= 500) {
            LOGGER.info("Server error while fetching {}: {}", checkerConfig.name(), response.statusCode());
        }
        else {
            LOGGER.error("Error while fetching {}: {}", checkerConfig.name(), response.statusCode());
        }
        return filteredItemList;
    }

    private List<Item> getAndFilterItemsList(HttpResponse<String> response) {
        List<Item> unfilteredItemList = getUnfilteredItemList(response);
        if (checkerConfig.regexFilter() != null && !checkerConfig.regexFilter().isBlank()) {
            return unfilteredItemList.stream()
                    .filter(item -> item.name().matches(checkerConfig.regexFilter()))
                    .toList();
        }
        return unfilteredItemList;
    }
}
