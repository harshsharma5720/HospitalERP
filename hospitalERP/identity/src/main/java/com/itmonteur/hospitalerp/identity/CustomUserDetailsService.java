package com.itmonteur.hospitalerp.identity;

import com.itmonteur.hospitalerp.identity.internal.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // The password is BCrypt encoded. A deactivated account is disabled: it can't log in (checked after the
        // password, see SecurityConfig) and JWTAuthenticationFilter rejects its tokens, like tokens of an older version
        return new AccountDetails(user);
    }
}
