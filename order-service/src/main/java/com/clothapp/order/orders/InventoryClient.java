package com.clothapp.order.orders;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

@Component
public class InventoryClient {

    private final RestClient restClient;
    private final String internalApiKey;

    public InventoryClient(RestClient.Builder builder,
                           @Value("${app.inventory-base-url}") String inventoryBaseUrl,
                           @Value("${app.internal-api-key}") String internalApiKey) {
        this.restClient = builder.baseUrl(inventoryBaseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public ReserveResult reserve(List<StockLine> items) {
        return restClient.post()
                .uri("/internal/inventory/reserve")
                .header("X-Internal-Key", internalApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new StockRequest(items))
                .retrieve()
                .body(ReserveResult.class);
    }

    public void commit(List<StockLine> items) {
        restClient.post()
                .uri("/internal/inventory/commit")
                .header("X-Internal-Key", internalApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new StockRequest(items))
                .retrieve()
                .toBodilessEntity();
    }

    public void release(List<StockLine> items) {
        restClient.post()
                .uri("/internal/inventory/release")
                .header("X-Internal-Key", internalApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new StockRequest(items))
                .retrieve()
                .toBodilessEntity();
    }

    public record StockRequest(List<StockLine> items) {
    }

    public record StockLine(String sku, Integer qty) {
    }

    public record ReserveResult(boolean reserved, List<String> problems, List<ReservedLine> lines) {
    }

    public record ReservedLine(String sku, String name, BigDecimal unitPrice, Integer qty) {
    }
}
