package com.clothapp.gateway.proxy;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/batch/products")
public class BatchGatewayController {

    private final RestClient inventoryRest;
    private final ProxySupport support;
    private final CircuitBreaker cb;
    private final Retry retry;

    public BatchGatewayController(@Qualifier("inventoryRest") RestClient inventoryRest,
                                  ProxySupport support,
                                  @Qualifier("inventoryCircuitBreaker") CircuitBreaker cb,
                                  @Qualifier("inventoryRetry") Retry retry) {
        this.inventoryRest = inventoryRest;
        this.support = support;
        this.cb = cb;
        this.retry = retry;
    }

    @GetMapping("/template")
    public ResponseEntity<?> template(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return call(() -> {
            byte[] bytes = support.headers(inventoryRest.get(), "/api/batch/products/template", jwt)
                    .retrieve().body(byte[].class);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=product-upload-template.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(bytes);
        }, "Batch template unavailable");
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file,
                                    @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return call(() -> {
            var body = new org.springframework.util.LinkedMultiValueMap<String, Object>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(toBytes(file)) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() == null ? "products.csv" : file.getOriginalFilename();
                }
            });
            Object resp = inventoryRest.post()
                    .uri("/api/batch/products/upload")
                    .header("X-Internal-Key", support.authHeaders(jwt).getFirst("X-Internal-Key"))
                    .header("X-User-Id", support.authHeaders(jwt).getFirst("X-User-Id"))
                    .header("X-User-Name", support.authHeaders(jwt).getFirst("X-User-Name"))
                    .header("X-User-Email", support.authHeaders(jwt).getFirst("X-User-Email"))
                    .header("X-User-Role", support.authHeaders(jwt).getFirst("X-User-Role"))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Object.class);
            return ResponseEntity.ok(resp);
        }, "Batch upload failed");
    }

    private byte[] toBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot read uploaded file", ex);
        }
    }

    private ResponseEntity<?> call(Supplier<ResponseEntity<?>> supplier, String message) {
        try {
            return retry.executeSupplier(() -> cb.executeSupplier(supplier));
        } catch (Exception ex) {
            return ResponseEntity.status(503).body(Map.of("message", message));
        }
    }
}
