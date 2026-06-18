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

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Stock checker for sharpknifeshop.com. Configure with a search result page with filters.
 * Client-side regex filter can be applied to further filter the ItemsList.
 */

public class SharpKnifeShopChecker extends AbstractChecker {

    private static final System.Logger LOGGER = System.getLogger(SharpKnifeShopChecker.class.getName());

    public SharpKnifeShopChecker(HttpFetcher httpFetcher, Notifier notifier, CheckerConfig checkerConfig) {
        super(httpFetcher, notifier, checkerConfig);
    }

    public List<Item> getUnfilteredItemList(HttpResponse<String> response) {
        List<Item> unfilteredItemList = new ArrayList<>();
        Document doc = Jsoup.parse(response.body());
        doc.setBaseUri(URI.create(checkerConfig.url()).resolve("/").toString());

        Element title = doc.selectXpath("head/title").first();
        if (title != null) {
            String text = title.text();
            if (text.contains("0 result")) {
                return unfilteredItemList;
            }
        }

        try {
            Element collection = doc.selectFirst("div[class=#collection-grid]");
            Elements products = collection.select("a.stretched-link");
            for (Element product : products) {
                try {
                    String href = product.absUrl("href");
                    String text = product.text();
                    unfilteredItemList.add(new Item(text, href));
                }
                catch (NullPointerException e) {
                    LOGGER.log(System.Logger.Level.ERROR, "Could not get href or title for product", e);
                }
            }
        }
        catch (NullPointerException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Element could not be found, source changed", e);
        }
        return unfilteredItemList;
    }

}