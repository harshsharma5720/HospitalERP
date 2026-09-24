package ITmonteur.example.hospitalERP.configuration;

import ITmonteur.example.hospitalERP.dto.RegisterRequestDTO;
import ITmonteur.example.hospitalERP.entities.Role;
import ITmonteur.example.hospitalERP.repositories.UserRepository;
import ITmonteur.example.hospitalERP.services.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN account at startup when ADMIN_USERNAME / ADMIN_PASSWORD are set
 * and no user with that username exists yet. Public registration only creates patients,
 * so this is how a fresh database gets its first administrator.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AuthService authService;
    private final UserRepository userRepository;

    @Value("${app.bootstrap-admin.username:}")
    private String username;
    @Value("${app.bootstrap-admin.password:}")
    private String password;
    @Value("${app.bootstrap-admin.email:}")
    private String email;
    @Value("${app.bootstrap-admin.phone:0000000000}")
    private String phone;

    public AdminBootstrap(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) {
            return;
        }
        if (userRepository.existsByUsername(username)) {
            return;
        }
        if (password.length() < 8) {
            logger.warn("ADMIN_PASSWORD must be at least 8 characters - bootstrap admin was not created");
            return;
        }
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email.isBlank() ? username + "@localhost" : email);
        request.setPhoneNumber(phone);
        authService.createUser(request, Role.ADMIN);
        logger.info("Bootstrap admin account '{}' created", username);
    }
}
