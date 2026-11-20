package com.clothapp.gateway.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, String> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    Optional<AppUser> findByMobile(String mobile);

    Optional<AppUser> findByEmailIgnoreCaseOrMobile(String email, String mobile);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByMobile(String mobile);

    List<AppUser> findAllByOrderByCreatedAtDesc();
}
