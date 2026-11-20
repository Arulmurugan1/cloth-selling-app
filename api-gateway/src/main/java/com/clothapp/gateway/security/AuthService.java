package com.clothapp.gateway.security;

import com.clothapp.gateway.security.SecurityDtos.AuthResponse;
import com.clothapp.gateway.security.SecurityDtos.LoginRequest;
import com.clothapp.gateway.security.SecurityDtos.RegisterRequest;
import com.clothapp.gateway.security.SecurityDtos.UserDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionLimiterService sessionLimiter;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SessionLimiterService sessionLimiter,
                       LoginAttemptService loginAttemptService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionLimiter = sessionLimiter;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public UserDto register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (users.existsByMobile(req.mobile())) {
            throw new IllegalArgumentException("Mobile already exists");
        }
        AppUser created = users.save(new AppUser(req.email().toLowerCase(), req.mobile(), req.fullName(),
                passwordEncoder.encode(req.password()), req.dob().atStartOfDay().toInstant(java.time.ZoneOffset.UTC)));
        return UserDto.from(created);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        loginAttemptService.checkAllowed(req.login());
        AppUser user = users.findByEmailIgnoreCaseOrMobile(req.login().toLowerCase(), req.login())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!user.isActive()) {
            throw new SecurityException("User is inactive");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            loginAttemptService.fail(req.login());
            throw new IllegalArgumentException("Invalid credentials");
        }
        if (!sessionLimiter.registerSession(user.getId(), jwtService.getExpirySeconds())) {
            throw new SecurityException("Maximum active users reached. Please try later.");
        }
        loginAttemptService.success(req.login());
        return new AuthResponse(jwtService.create(user), jwtService.getExpirySeconds(), UserDto.from(user));
    }

    public void logout(String userId) {
        sessionLimiter.removeSession(userId);
    }
}
