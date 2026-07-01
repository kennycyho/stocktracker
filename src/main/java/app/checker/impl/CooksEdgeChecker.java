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
import java.util.ArrayList;
import java.util.List;

/**
 * Stock checker for cooksedge.com. Configure with a search result page with filters.
 * Client-side regex filter can be applied to further filter the ItemsList.
 */

public class CooksEdgeChecker extends AbstractChecker {

    /**
     * Constructs a new CooksEdgeChecker.
     *
     * @param httpFetcher     the HTTP fetcher for making requests
     * @param notifier        the notifier for sending alerts
     * @param cooldownService the cooldown service for rate limiting
     * @param checkerConfig   the configuration for this checker
     */
    public CooksEdgeChecker(HttpFetcher httpFetcher,
                            Notifier notifier,
                            CooldownService cooldownService,
                            CheckerConfig checkerConfig) {
        super(httpFetcher, notifier, cooldownService, checkerConfig);
    }

    /**
     * Parses the HTTP response and extracts all products from the page.
     *
     * @param responseBody the HTTP response body containing the page HTML
     * @return a list of products found on the page
     */
    public List<Product> getUnfilteredItemList(String responseBody) {
        List<Product> unfilteredProductList = new ArrayList<>();
        Document doc = Jsoup.parse(responseBody);
        doc.setBaseUri(URI.create(getCheckerConfig().url()).resolve("/").toString());

        Element searchWindow = doc.selectFirst("div.search__window");

        if (searchWindow == null) {
            logger.error("Search window element could not be found");
            return unfilteredProductList;
        }

        Elements products = searchWindow.select("div.product-item__meta");
        for (Element product : products) {
            Element itemTag = product.selectFirst("a");
            if (itemTag != null) {
                String href = itemTag.absUrl("href").split("\\?")[0];
                String text = itemTag.text();
                if (text.isBlank()) {
                    logger.error("Could not get title for product: {}", product.outerHtml());
                }
                else unfilteredProductList.add(new Product(text, href));
            }
            else logger.error("Missing anchor in product: {}", product.outerHtml());
        }

        return unfilteredProductList;
    }

}
