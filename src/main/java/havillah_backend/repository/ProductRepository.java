package havillah_backend.repository;

import havillah_backend.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /*
     * Finds products whose names contain the supplied text,
     * without case sensitivity.
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    // Check whether a category is still referenced by any product.
    boolean existsByCategory_Id(Long categoryId);

    // Search and filter products while returning only the requested page.
    @Query(
            value = """
                    SELECT p FROM Product p
                    WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
                      AND (:categoryId IS NULL OR p.category.id = :categoryId)
                      AND (:vendorId IS NULL OR p.vendor.id = :vendorId)
                      AND (:minPrice IS NULL OR p.price >= :minPrice)
                      AND (:maxPrice IS NULL OR p.price <= :maxPrice)
                      AND (:inStock IS NULL
                           OR (:inStock = TRUE AND p.stockQuantity > 0)
                           OR (:inStock = FALSE AND p.stockQuantity = 0))
                      AND p.active = TRUE
                    """,
            countQuery = """
                    SELECT COUNT(p) FROM Product p
                    WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
                      AND (:categoryId IS NULL OR p.category.id = :categoryId)
                      AND (:vendorId IS NULL OR p.vendor.id = :vendorId)
                      AND (:minPrice IS NULL OR p.price >= :minPrice)
                      AND (:maxPrice IS NULL OR p.price <= :maxPrice)
                      AND (:inStock IS NULL
                           OR (:inStock = TRUE AND p.stockQuantity > 0)
                           OR (:inStock = FALSE AND p.stockQuantity = 0))
                      AND p.active = TRUE
                    """
    )
    Page<Product> findProductsWithFilters(
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            @Param("vendorId") Long vendorId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("inStock") Boolean inStock,
            Pageable pageable
    );

    // Locks the product row until the transaction ends, so two buyers
    // cannot both take the last unit.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}