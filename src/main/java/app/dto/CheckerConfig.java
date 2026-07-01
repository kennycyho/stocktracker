package app.dto;

/**
 * Configuration record for a stock checker.
 * <p>
 * Contains the settings needed to configure and run a specific checker instance.
 */
public record CheckerConfig(String name, String checker, String url, String regexFilter) {

}