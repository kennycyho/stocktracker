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

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Stock checker for sharpknifeshop.com. Configure with a search result page with filters.
 * Client-side regex filter can be applied to further filter the ItemsList.
 */

public class SharpKnifeShopChecker extends AbstractChecker {

    public SharpKnifeShopChecker(HttpFetcher httpFetcher, Notifier notifier, CheckerConfig checkerConfig) {
        super(httpFetcher, notifier, checkerConfig);
    }

    public List<Product> getUnfilteredItemList(HttpResponse<String> response) {
        List<Product> unfilteredProductList = new ArrayList<>();
        Document doc = Jsoup.parse(response.body());
        doc.setBaseUri(URI.create(checkerConfig.url()).resolve("/").toString());

        if (!hasSearchResults(doc)) return unfilteredProductList;

        Element collection = doc.selectFirst("div[class=#collection-grid]");
        if (collection == null) {
            logger.error("Could not get collection element");
            return unfilteredProductList;
        }

        Elements products = collection.select("a.stretched-link");
        for (Element product : products) {
            String href = product.absUrl("href");
            String text = product.text();
            if (text.isBlank()) {
                logger.error("Could not get title for product: {}", product.outerHtml());
            }
            else unfilteredProductList.add(new Product(text, href));
        }

        return unfilteredProductList;
    }

    private boolean hasSearchResults(Document doc) {
        Element title = doc.selectXpath("head/title").first();
        if (title != null) {
            String text = title.text();
            return !text.contains("0 result");
        }
        else logger.error("Could not get page title");
        return true;
    }
}