package app.dto;

/**
 * Configuration record for a stock checker.
 * <p>
 * Contains the settings needed to configure and run a specific checker instance.
 *
 * @param name        the display name of the checker
 * @param checker     the type identifier of the checker implementation
 * @param url         the URL to check for stock availability
 * @param regexFilter optional regex pattern to filter products by name
 */
public record CheckerConfig(String name, String checker, String url, String regexFilter) {

}