package havillah_backend.config;

import havillah_backend.entity.Role;
import havillah_backend.entity.User;
import havillah_backend.repository.RoleRepository;
import havillah_backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        createRoleIfMissing("ADMIN");
        createRoleIfMissing("CUSTOMER");
        createRoleIfMissing("VENDOR");

        createAdminIfMissing();
    }

    private void createRoleIfMissing(String name) {

        if (roleRepository.findByName(name).isEmpty()) {
            Role role = new Role();
            role.setName(name);
            roleRepository.save(role);
        }
    }

    private void createAdminIfMissing() {

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() ->
                        new IllegalStateException("ADMIN role not found"));

        User admin = new User();

        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setEnabled(true);
        admin.setRole(adminRole);

        userRepository.save(admin);
    }
}