package app.dto;

/**
 * Record representing a product found during stock checking.
 * <p>
 * Contains the product name and its URL for notification purposes.
 *
 * @param name the name/title of the product
 * @param url  the URL where the product can be accessed
 */
public record Product(String name, String url) {

}
