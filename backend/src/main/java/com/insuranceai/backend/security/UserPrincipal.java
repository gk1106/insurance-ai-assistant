package com.insuranceai.backend.security;

import com.insuranceai.backend.auth.entity.Role;
import com.insuranceai.backend.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String password;
    private final Role role;
    private final UUID customerId;
    private final boolean enabled;

    public UserPrincipal(UUID id, String username, String password, Role role, UUID customerId, boolean enabled) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.customerId = customerId;
        this.enabled = enabled;
    }

    public static UserPrincipal fromUser(User user) {
        UUID customerId = user.getCustomer() != null ? user.getCustomer().getId() : null;
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRole(),
                customerId,
                user.isEnabled()
        );
    }

    public UUID getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
