package havillah_backend.repository;

import havillah_backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

/*This line tells spring "find products with String-name regardless of the case"
* now add the search to ProductService*/
    List<Product> findByNameContainingIgnoreCase(String name);
}
