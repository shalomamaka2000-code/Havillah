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

        if (roleRepository.count() == 0) {

            Role admin = new Role();
            admin.setName("ADMIN");

            Role customer = new Role();
            customer.setName("CUSTOMER");

            Role vendor = new Role();
            vendor.setName("VENDOR");

            roleRepository.save(admin);
            roleRepository.save(customer);
            roleRepository.save(vendor);
        }
    }
}