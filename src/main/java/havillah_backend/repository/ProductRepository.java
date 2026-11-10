package havillah_backend.repository;

import havillah_backend.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /*
     * Finds products whose names contain the supplied text,
     * without case sensitivity.
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    // Check whether a category is still referenced by any product.
    boolean existsByCategory_Id(Long categoryId);

    // Locks the product row until the transaction ends, so two buyers
    // cannot both take the last unit.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
