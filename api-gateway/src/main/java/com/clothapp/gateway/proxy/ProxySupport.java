package com.clothapp.gateway.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class ProxySupport {

    private final String internalKey;

    public ProxySupport(@Value("${app.internal-api-key}") String internalKey) {
        this.internalKey = internalKey;
    }

    public RestClient.RequestHeadersSpec<?> headers(RestClient.RequestBodyUriSpec spec, String path, Jwt jwt, Object body) {
        var req = spec.uri(path)
                .header("X-Internal-Key", internalKey)
                .header("X-User-Id", jwt.getSubject())
                .header("X-User-Name", jwt.getClaimAsString("name"))
                .header("X-User-Email", jwt.getClaimAsString("email"))
                .header("X-User-Role", jwt.getClaimAsString("role"));
        if (body != null) {
            return req.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return req;
    }

    public RestClient.RequestHeadersSpec<?> headers(RestClient.RequestHeadersUriSpec<?> spec, String path, Jwt jwt) {
        return spec.uri(path)
                .header("X-Internal-Key", internalKey)
                .header("X-User-Id", jwt.getSubject())
                .header("X-User-Name", jwt.getClaimAsString("name"))
                .header("X-User-Email", jwt.getClaimAsString("email"))
                .header("X-User-Role", jwt.getClaimAsString("role"));
    }

    public HttpHeaders authHeaders(Jwt jwt) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.add("X-Internal-Key", internalKey);
        h.add("X-User-Id", jwt.getSubject());
        h.add("X-User-Name", jwt.getClaimAsString("name"));
        h.add("X-User-Email", jwt.getClaimAsString("email"));
        h.add("X-User-Role", jwt.getClaimAsString("role"));
        return h;
    }

    public Map<String, String> error(String message) {
        return Map.of("message", message);
    }
}
