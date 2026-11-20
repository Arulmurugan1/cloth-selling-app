package com.clothapp.gateway.security;

import com.clothapp.gateway.security.SecurityDtos.AuthResponse;
import com.clothapp.gateway.security.SecurityDtos.LoginRequest;
import com.clothapp.gateway.security.SecurityDtos.RegisterRequest;
import com.clothapp.gateway.security.SecurityDtos.UserDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public UserDto register(@Valid @RequestBody RegisterRequest body) {
        return service.register(body);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest body) {
        return service.login(body);
    }

    @PostMapping("/logout")
    public void logout(@AuthenticationPrincipal Jwt jwt) {
        service.logout(jwt.getSubject());
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return new UserDto(jwt.getSubject(), jwt.getClaimAsString("name"), jwt.getClaimAsString("email"),
                jwt.getClaimAsString("mobile"), jwt.getClaimAsString("role"), true);
    }
}
