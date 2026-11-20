package com.clothapp.gateway.proxy;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    RetryRegistry retryRegistry() {
        RetryConfig cfg = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(300))
                .build();
        return RetryRegistry.of(cfg);
    }

    @Bean("inventoryCircuitBreaker")
    CircuitBreaker inventoryCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("inventory");
    }

    @Bean("orderCircuitBreaker")
    CircuitBreaker orderCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("order");
    }

    @Bean("inventoryRetry")
    Retry inventoryRetry(RetryRegistry registry) {
        return registry.retry("inventory");
    }

    @Bean("orderRetry")
    Retry orderRetry(RetryRegistry registry) {
        return registry.retry("order");
    }
}
