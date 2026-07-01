package app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration class for setting up the RestClient bean.
 */
@Configuration
public class RestClientConfig {

    /**
     * Creates and configures a RestClient bean for making HTTP requests.
     *
     * @param builder the RestClient.Builder to configure
     * @return a configured RestClient instance
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
