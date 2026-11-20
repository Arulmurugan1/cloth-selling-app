package com.clothapp.gateway.proxy;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/orders")
public class OrderGatewayController {

    private final RestClient orderRest;
    private final ProxySupport support;
    private final CircuitBreaker cb;
    private final Retry retry;

    public OrderGatewayController(@Qualifier("orderRest") RestClient orderRest,
                                  ProxySupport support,
                                  @Qualifier("orderCircuitBreaker") CircuitBreaker cb,
                                  @Qualifier("orderRetry") Retry retry) {
        this.orderRest = orderRest;
        this.support = support;
        this.cb = cb;
        this.retry = retry;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body,
                                    @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return call(() -> ResponseEntity.status(201).body(support.headers(orderRest.post(), "/internal/orders", jwt, body)
                .retrieve().body(Object.class)), "Order service unavailable");
    }

    @GetMapping
    public ResponseEntity<?> list(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return call(() -> ResponseEntity.ok(support.headers(orderRest.get(), "/internal/orders", jwt)
                .retrieve().body(Object.class)), "Order service unavailable");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> one(@PathVariable String id,
                                 @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return call(() -> ResponseEntity.ok(support.headers(orderRest.get(), "/internal/orders/" + id, jwt)
                .retrieve().body(Object.class)), "Order service unavailable");
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<?> dashboard(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return call(() -> ResponseEntity.ok(support.headers(orderRest.get(), "/internal/orders/dashboard/summary", jwt)
                .retrieve().body(Object.class)), "Order service unavailable");
    }

    private ResponseEntity<?> call(Supplier<ResponseEntity<?>> supplier, String message) {
        try {
            return retry.executeSupplier(() -> cb.executeSupplier(supplier));
        } catch (Exception ex) {
            return ResponseEntity.status(503).body(Map.of("message", message));
        }
    }
}
