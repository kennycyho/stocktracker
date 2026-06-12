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
 * Stock checker for cooksedge.com. Configure with a search result page with filters.
 * Client-side regex filter can be applied to further filter the ItemsList.
 */

public class CooksEdgeChecker extends SearchChecker {

    private static final System.Logger LOGGER = System.getLogger(CooksEdgeChecker.class.getName());

    public CooksEdgeChecker(HttpFetcher httpFetcher, StockNotifier stockNotifier, CheckerConfig checkerConfig) {
        super(httpFetcher, stockNotifier, checkerConfig);
    }

    public List<Item> getUnfilteredItemList() {
        List<Item> unfilteredItemList = new ArrayList<>();
        HttpResponse<String> response = httpFetcher.fetch(checkerConfig.url());
        Document doc = Jsoup.parse(response.body());
        doc.setBaseUri(URI.create(checkerConfig.url()).resolve("/").toString());

        try {
            Element searchWindow = doc.selectFirst("div.search__window");
            Elements products = searchWindow.select("div.product-item__meta");
            for (Element product : products) {
                Element itemTag = product.selectFirst("a");
                try {
                    String href = itemTag.absUrl("href");
                    String text = itemTag.text();
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