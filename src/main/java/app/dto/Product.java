package app.dto;

/**
 * Record representing a product found during stock checking.
 * <p>
 * Contains the product name and its URL for notification purposes.
 */
public record Product(String name, String url) {

}
