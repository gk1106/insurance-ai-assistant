package com.insuranceai.backend.security;

import com.insuranceai.backend.auth.entity.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CurrentUserProvider {

    public Optional<UserPrincipal> getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    /**
     * Allows ADMIN/AGENT unconditionally. For a CUSTOMER, allows access only when the
     * resource's owning customer id matches their own linked customer id.
     */
    public void assertOwnerOrElevated(UUID resourceCustomerId) {
        UserPrincipal principal = getCurrentPrincipal()
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));

        if (principal.getRole() == Role.ADMIN || principal.getRole() == Role.AGENT) {
            return;
        }
        if (principal.getCustomerId() == null || !principal.getCustomerId().equals(resourceCustomerId)) {
            throw new AccessDeniedException("Not authorized to access this resource");
        }
    }
}
