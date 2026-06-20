package app.checker.impl;

import app.checker.AbstractChecker;
import app.dto.CheckerConfig;
import app.dto.Product;
import app.fetcher.HttpFetcher;
import app.notifier.Notifier;
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
 * Stock checker for cooksedge.com. Configure with a search result page with filters.
 * Client-side regex filter can be applied to further filter the ItemsList.
 */

public class CooksEdgeChecker extends AbstractChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(CooksEdgeChecker.class);

    public CooksEdgeChecker(HttpFetcher httpFetcher, Notifier notifier, CheckerConfig checkerConfig) {
        super(httpFetcher, notifier, checkerConfig);
    }

    public List<Product> getUnfilteredItemList(HttpResponse<String> response) {
        List<Product> unfilteredProductList = new ArrayList<>();
        Document doc = Jsoup.parse(response.body());
        doc.setBaseUri(URI.create(checkerConfig.url()).resolve("/").toString());

        Element searchWindow = doc.selectFirst("div.search__window");

        if (searchWindow == null) {
            LOGGER.error("Search window element could not be found");
            return unfilteredProductList;
        }

        Elements products = searchWindow.select("div.product-item__meta");
        for (Element product : products) {
            Element itemTag = product.selectFirst("a");
            if (itemTag != null) {
                String href = itemTag.absUrl("href");
                String text = itemTag.text();
                if (text.isBlank()) {
                    LOGGER.error("Could not get title for product: {}", product.outerHtml());
                }
                else unfilteredProductList.add(new Product(text, href));
            }
            else LOGGER.error("Missing anchor in product: {}", product.outerHtml());
        }

        return unfilteredProductList;
    }

}