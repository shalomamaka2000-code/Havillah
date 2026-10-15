package havillah_backend.repository;

import havillah_backend.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

/*This line tells spring "find products with String-name regardless of the case"
* now add the search to ProductService*/
    List<Product> findByNameContainingIgnoreCase(String name);

    // Locks the product row until the transaction ends, so two buyers
// can't both take the last unit
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}