package com.clothapp.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class SuperAdminBootstrap implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Value("${app.bootstrap.superadmin.email:superadmin@clothapp.local}")
    private String email;

    @Value("${app.bootstrap.superadmin.mobile:9999999999}")
    private String mobile;

    @Value("${app.bootstrap.superadmin.name:Super Admin}")
    private String name;

    @Value("${app.bootstrap.superadmin.password}")
    private String password;

    public SuperAdminBootstrap(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        AppUser user = new AppUser(email.toLowerCase(), mobile, name, encoder.encode(password),
                LocalDate.of(1990, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        user.setRole(Role.SUPER_ADMIN);
        users.save(user);
    }
}
