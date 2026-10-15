package havillah_backend.config;

import havillah_backend.entity.Role;
import havillah_backend.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        createRoleIfMissing("ADMIN");
        createRoleIfMissing("CUSTOMER");
        createRoleIfMissing("VENDOR");
    }

    private void createRoleIfMissing(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = new Role();
            role.setName(name);
            roleRepository.save(role);
        }
    }
}