package havillah_backend.service;

import havillah_backend.dto.ProductRequest;
import havillah_backend.dto.ProductResponse;
import havillah_backend.entity.Category;
import havillah_backend.entity.Product;
import havillah_backend.entity.Vendor;
import havillah_backend.exception.ResourceNotFoundException;
import havillah_backend.repository.CategoryRepository;
import havillah_backend.repository.ProductRepository;
import havillah_backend.repository.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "name", "name",
            "price", "price",
            "newest", "createdAt",
            "stock", "stockQuantity"
    );

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final VendorRepository vendorRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            VendorRepository vendorRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.vendorRepository = vendorRepository;
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request, Authentication authentication) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Vendor vendor;
        if (isAdmin(authentication)) {
            if (request.getVendorId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vendor ID is required when an admin creates a product");
            }
            vendor = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        } else {
            vendor = getAuthenticatedVendor(authentication);
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setVendor(vendor);

        return mapToResponse(productRepository.save(product));
    }

    public List<ProductResponse> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCase(name)
                .stream().map(this::mapToResponse).toList();
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public Page<ProductResponse> getProductsPage(
            String name,
            Long categoryId,
            Long vendorId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page number cannot be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and " + MAX_PAGE_SIZE
            );
        }
        if (minPrice != null && minPrice.signum() < 0
                || maxPrice != null && maxPrice.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price filters cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Minimum price cannot be greater than maximum price"
            );
        }

        String sortProperty = SORT_FIELDS.get(sortBy == null ? "newest" : sortBy.toLowerCase());
        if (sortProperty == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported sort field. Use name, price, newest, or stock"
            );
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction == null ? "desc" : direction);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sort direction must be 'asc' or 'desc'"
            );
        }

        String normalizedName = name == null || name.isBlank() ? null : name.trim();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortProperty)
        );

        return productRepository.findProductsWithFilters(
                normalizedName,
                categoryId,
                vendorId,
                minPrice,
                maxPrice,
                inStock,
                pageable
        ).map(this::mapToResponse);
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapToResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(
            Long id,
            ProductRequest request,
            Authentication authentication
    ) {
        Product product = getProduct(id);
        boolean admin = isAdmin(authentication);

        if (!admin) {
            ensureVendorOwnsProduct(product, authentication);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);

        // Only an admin may transfer a product to another vendor.
        if (admin && request.getVendorId() != null) {
            Vendor vendor = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
            product.setVendor(vendor);
        }

        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id, Authentication authentication) {
        Product product = getProduct(id);

        if (!isAdmin(authentication)) {
            ensureVendorOwnsProduct(product, authentication);
        }

        productRepository.delete(product);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private Vendor getAuthenticatedVendor(Authentication authentication) {
        return vendorRepository.findByUserEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No vendor profile is linked to this account"
                ));
    }

    private void ensureVendorOwnsProduct(Product product, Authentication authentication) {
        Vendor vendor = getAuthenticatedVendor(authentication);
        if (!product.getVendor().getId().equals(vendor.getId())) {
            throw new AccessDeniedException("You can only manage your own products");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getActive(),
                product.getCreatedAt(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getVendor().getId(),
                product.getVendor().getBusinessName()
        );
    }
}