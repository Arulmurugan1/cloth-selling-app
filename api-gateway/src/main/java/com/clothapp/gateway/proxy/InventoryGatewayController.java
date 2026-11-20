package com.clothapp.gateway.proxy;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/products")
public class InventoryGatewayController {

    private final RestClient inventoryRest;
    private final ProxySupport support;
    private final CircuitBreaker cb;
    private final Retry retry;

    public InventoryGatewayController(@Qualifier("inventoryRest") RestClient inventoryRest,
                                      ProxySupport support,
                                      @Qualifier("inventoryCircuitBreaker") CircuitBreaker cb,
                                      @Qualifier("inventoryRetry") Retry retry) {
        this.inventoryRest = inventoryRest;
        this.support = support;
        this.cb = cb;
        this.retry = retry;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String search,
                                  @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        String uri = (search == null || search.isBlank()) ? "/api/products" : "/api/products?search=" + search;
        return call(() -> ResponseEntity.ok(support.headers(inventoryRest.get(), uri, jwt).retrieve().body(Object.class)),
                "Inventory service unavailable");
    }

    @GetMapping("/{sku}")
    public ResponseEntity<?> get(@PathVariable String sku,
                                 @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return call(() -> ResponseEntity.ok(support.headers(inventoryRest.get(), "/api/products/" + sku, jwt)
                .retrieve().body(Object.class)), "Inventory service unavailable");
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body,
                                    @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return call(() -> ResponseEntity.status(201).body(support.headers(inventoryRest.post(), "/api/products", jwt, body)
                .retrieve().body(Object.class)), "Inventory write failed");
    }

    @PutMapping("/{sku}")
    public ResponseEntity<?> update(@PathVariable String sku,
                                    @RequestBody Map<String, Object> body,
                                    @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return call(() -> ResponseEntity.ok(support.headers(inventoryRest.put(), "/api/products/" + sku, jwt, body)
                .retrieve().body(Object.class)), "Inventory write failed");
    }

    @PatchMapping("/{sku}/inventory")
    public ResponseEntity<?> inventory(@PathVariable String sku,
                                       @RequestBody Map<String, Object> body,
                                       @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return call(() -> ResponseEntity.ok(support.headers(inventoryRest.patch(), "/api/products/" + sku + "/inventory", jwt, body)
                .retrieve().body(Object.class)), "Inventory write failed");
    }

    @PatchMapping("/{sku}/active")
    public ResponseEntity<?> active(@PathVariable String sku,
                                    @RequestBody Map<String, Object> body,
                                    @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return call(() -> ResponseEntity.ok(support.headers(inventoryRest.patch(), "/api/products/" + sku + "/active", jwt, body)
                .retrieve().body(Object.class)), "Inventory write failed");
    }

    @PatchMapping("/{sku}/defective")
    public ResponseEntity<?> defective(@PathVariable String sku,
                                       @RequestBody Map<String, Object> body,
                                       @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return call(() -> ResponseEntity.ok(support.headers(inventoryRest.patch(), "/api/products/" + sku + "/defective", jwt, body)
                .retrieve().body(Object.class)), "Inventory write failed");
    }

    private ResponseEntity<?> call(Supplier<ResponseEntity<?>> supplier, String message) {
        try {
            return retry.executeSupplier(() -> cb.executeSupplier(supplier));
        } catch (Exception ex) {
            return ResponseEntity.status(503).body(support.error(message));
        }
    }
}
