package com.clothapp.gateway.security;

import com.clothapp.gateway.security.SecurityDtos.UserActiveUpdate;
import com.clothapp.gateway.security.SecurityDtos.UserDto;
import com.clothapp.gateway.security.SecurityDtos.UserRoleUpdate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserAdminController {

    private final UserRepository users;

    public UserAdminController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public List<UserDto> list() {
        return users.findAllByOrderByCreatedAtDesc().stream().map(SecurityDtos.UserDto::from).toList();
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    public UserDto active(@PathVariable String id, @RequestBody UserActiveUpdate body) {
        AppUser user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setActive(Boolean.TRUE.equals(body.active()));
        return UserDto.from(users.save(user));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public UserDto role(@PathVariable String id, @RequestBody UserRoleUpdate body) {
        AppUser user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(Role.valueOf(body.role()));
        return UserDto.from(users.save(user));
    }
}
