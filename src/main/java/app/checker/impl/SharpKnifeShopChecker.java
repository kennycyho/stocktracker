package app.checker.impl;

import app.checker.AbstractChecker;
import app.cooldown.CooldownService;
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

    /**
     * Constructs a new SharpKnifeShopChecker.
     *
     * @param httpFetcher the HTTP fetcher for making requests
     * @param notifier the notifier for sending alerts
     * @param cooldownService the cooldown service for rate limiting
     * @param checkerConfig the configuration for this checker
     */
    public SharpKnifeShopChecker(HttpFetcher httpFetcher,
                                 Notifier notifier,
                                 CooldownService cooldownService,
                                 CheckerConfig checkerConfig) {
        super(httpFetcher, notifier, cooldownService, checkerConfig);
    }

    /**
     * Parses the HTTP response and extracts all products from the page.
     *
     * @param response the HTTP response containing the page HTML
     * @return a list of products found on the page
     */
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
            String href = product.absUrl("href").split("\\?")[0];
            String text = product.text();
            if (text.isBlank()) {
                logger.error("Could not get title for product: {}", product.outerHtml());
            }
            else unfilteredProductList.add(new Product(text, href));
        }

        return unfilteredProductList;
    }

    /**
     * Checks if the page contains search results by examining the page title.
     *
     * @param doc the parsed HTML document
     * @return true if the page has search results, false otherwise
     */
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
