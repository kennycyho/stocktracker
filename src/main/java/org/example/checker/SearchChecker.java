package org.example.checker;

import org.example.fetcher.HttpFetcher;
import org.example.model.CheckerConfig;
import org.example.model.Item;
import org.example.notifier.StockNotifier;

import java.util.List;
import java.util.logging.Logger;

/**
 * Checks stock from search results page.
 */

public abstract class SearchChecker implements StockChecker {

    private static final Logger LOGGER = Logger.getLogger(SearchChecker.class.getName());

    protected final HttpFetcher httpFetcher;
    protected final StockNotifier stockNotifier;
    protected final CheckerConfig checkerConfig;

    protected SearchChecker(HttpFetcher httpFetcher, StockNotifier stockNotifier, CheckerConfig checkerConfig) {
        this.httpFetcher = httpFetcher;
        this.stockNotifier = stockNotifier;
        this.checkerConfig = checkerConfig;
    }

    public abstract List<Item> getUnfilteredItemList();

    @Override
    public void check() {
        List<Item> itemList = getFilteredItemList();
        if (!itemList.isEmpty()) {
            LOGGER.info("Found items for product " + checkerConfig.name());
            stockNotifier.send(
                    checkerConfig.name() + " is in stock with " + itemList.size() + " items",
                    itemList);
        }
    }

    private List<Item> getFilteredItemList() {
        List<Item> unfilteredItemList = getUnfilteredItemList();
        if (checkerConfig.regexFilter() != null && !checkerConfig.regexFilter().isBlank()) {
            return unfilteredItemList.stream()
                    .filter(item -> item.name().matches(checkerConfig.regexFilter()))
                    .toList();
        }
        else {
            return unfilteredItemList;
        }
    }
}
