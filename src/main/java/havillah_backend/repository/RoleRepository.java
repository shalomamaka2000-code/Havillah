package havillah_backend.repository;

import havillah_backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

//Note this is an interface not a class
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}