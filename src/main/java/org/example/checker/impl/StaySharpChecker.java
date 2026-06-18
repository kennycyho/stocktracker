package org.example.checker.impl;

import org.example.checker.AbstractChecker;
import org.example.fetcher.HttpFetcher;
import org.example.model.CheckerConfig;
import org.example.model.Item;
import org.example.notifier.Notifier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Stock checker for staysharpmtl.com. Configure with a search result page with filters.
 * Client-side regex filter can be applied to further filter the ItemsList.
 */

public class StaySharpChecker extends AbstractChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaySharpChecker.class.getName());

    public StaySharpChecker(HttpFetcher httpFetcher, Notifier notifier, CheckerConfig checkerConfig) {
        super(httpFetcher, notifier, checkerConfig);
    }

    public List<Item> getUnfilteredItemList(HttpResponse<String> response) {
        List<Item> unfilteredItemList = new ArrayList<>();
        Document doc = Jsoup.parse(response.body());
        doc.setBaseUri(URI.create(checkerConfig.url()).resolve("/").toString());

        Elements products = doc.select("div[data-product-grid]");
        if (!products.isEmpty()) {
            for (Element product : products) {
                Element titleText = product.selectFirst("a[class=yv-product-title text]");
                if (titleText != null) {
                    String href = titleText.absUrl("href");
                    String title = titleText.attr("title");
                    unfilteredItemList.add(new Item(title, href));
                }
                else {
                    LOGGER.error("Could not get details for product: {}", product.outerHtml());
                }
            }
        }
        return unfilteredItemList;
    }
    
}
