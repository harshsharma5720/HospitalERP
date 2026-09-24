package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.entities.Role;
import ITmonteur.example.hospitalERP.entities.User;
import ITmonteur.example.hospitalERP.exception.ForbiddenException;
import ITmonteur.example.hospitalERP.repositories.UserRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;

/**
 * Resolves the logged-in user from the security context.
 * Services use this instead of trusting user/patient IDs sent by the client.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new AuthenticationCredentialsNotFoundException("Not authenticated");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("User no longer exists"));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public boolean hasRole(Role role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String authority = "ROLE_" + role.name();
        return authentication.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    public boolean hasAnyRole(Role... roles) {
        return Arrays.stream(roles).anyMatch(this::hasRole);
    }

    // ADMIN and RECEPTIONIST manage appointments and patients on behalf of the hospital
    public boolean isStaff() {
        return hasAnyRole(Role.ADMIN, Role.RECEPTIONIST);
    }

    /** Allows the call when userId belongs to the caller, or the caller has one of the given roles. */
    public void requireSelfOrRole(Long userId, Role... allowedRoles) {
        if (hasAnyRole(allowedRoles)) {
            return;
        }
        if (!Objects.equals(getCurrentUserId(), userId)) {
            throw new ForbiddenException("You can only access your own records");
        }
    }
}
