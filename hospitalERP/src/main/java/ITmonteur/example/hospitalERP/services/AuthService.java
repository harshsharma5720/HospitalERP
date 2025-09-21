package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ITmonteur.example.hospitalERP.dto.AuthResponseDTO;
import ITmonteur.example.hospitalERP.dto.LoginRequestDTO;
import ITmonteur.example.hospitalERP.dto.RegisterRequestDTO;
import ITmonteur.example.hospitalERP.entities.Role;
import ITmonteur.example.hospitalERP.entities.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthResponseDTO register(RegisterRequestDTO request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.valueOf(request.getRole().toUpperCase())); // convert string to enum

        User savedUser= userRepository.save(user);
        // ye mne is liye kiya h taki register krne ke bad turant ek token generate ho jae bina login kre
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(savedUser.getUsername())   // username from DB
                .password(savedUser.getPassword())       // encoded password
                .authorities(new SimpleGrantedAuthority("ROLE_" + savedUser.getRole().name())) // map role
                .build();
        // 4. Generate JWT token for the saved user
        String token = jwtService.generateToken(userDetails);
        // 5. Return token in response object
        return new AuthResponseDTO(token);
//        return "User registered successfully!";
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        if (authentication.isAuthenticated()) {
            String token = jwtService.generateToken((org.springframework.security.core.userdetails.User) authentication.getPrincipal());
            return new AuthResponseDTO(token);
        } else {
            throw new RuntimeException("Invalid credentials");
        }
    }
}
