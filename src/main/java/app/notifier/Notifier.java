package app.notifier;

import app.dto.Product;

import java.util.List;

/**
 * Interface for a notifier that sends notifications.
 */
public interface Notifier {

    /**
     * Sends a notification with the specified title and list of products.
     *
     * @param title       the title of the notification
     * @param productList the list of products to include in the notification
     */
    void send(String title, List<Product> productList);
}
