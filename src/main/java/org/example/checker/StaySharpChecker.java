package org.example.checker;

import org.example.fetcher.HttpFetcher;
import org.example.model.CheckerConfig;
import org.example.model.Item;
import org.example.notifier.StockNotifier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Stock checker for staysharpmtl.com. Configure with a search result page with filters.
 * Client-side regex filter can be applied to further filter the ItemsList.
 */

public class StaySharpChecker extends SearchChecker {

    private static final System.Logger LOGGER = System.getLogger(StaySharpChecker.class.getName());

    public StaySharpChecker(HttpFetcher httpFetcher, StockNotifier stockNotifier, CheckerConfig checkerConfig) {
        super(httpFetcher, stockNotifier, checkerConfig);
    }

    public List<Item> getUnfilteredItemList() {
        List<Item> unfilteredItemList = new ArrayList<>();

        HttpResponse<String> response = httpFetcher.fetch(checkerConfig.url());
        Document doc = Jsoup.parse(response.body());
        doc.setBaseUri(URI.create(checkerConfig.url()).resolve("/").toString());

        Elements products = doc.select("div[data-product-grid]");
        if (!products.isEmpty()) {
            for (Element product : products.asList()) {
                Element titleText = product.selectFirst("a[class=yv-product-title text]");
                try {
                    String href = titleText.absUrl("href");
                    String title = titleText.attr("title");
                    unfilteredItemList.add(new Item(title, href));
                }
                catch (NullPointerException e) {
                    LOGGER.log(System.Logger.Level.ERROR, "Could not get href or title for product", e);
                }
            }
        }
        return unfilteredItemList;
    }

}
