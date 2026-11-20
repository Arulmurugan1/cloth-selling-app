package com.clothapp.gateway.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final byte[] secret;
    private final long expirySeconds;
    private final JwtDecoder jwtDecoder;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiry-seconds:3600}") long expirySeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirySeconds = expirySeconds;
        SecretKeySpec key = new SecretKeySpec(this.secret, "HmacSHA256");
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(key).build();
    }

    public String create(AppUser user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirySeconds);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getId())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .claim("name", user.getFullName())
                .claim("email", user.getEmail())
                .claim("mobile", user.getMobile())
                .claim("role", user.getRole().name())
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to create token", e);
        }
    }

    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }
}
