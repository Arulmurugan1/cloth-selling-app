package com.clothapp.gateway.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class DownstreamClients {

    @Bean("inventoryRest")
    RestClient inventoryRest(RestClient.Builder builder,
                             @Value("${app.inventory-base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean("orderRest")
    RestClient orderRest(RestClient.Builder builder,
                         @Value("${app.order-base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
