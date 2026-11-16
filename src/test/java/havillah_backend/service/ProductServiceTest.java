package havillah_backend.service;

import havillah_backend.dto.ProductRequest;
import havillah_backend.dto.ProductResponse;
import havillah_backend.entity.Category;
import havillah_backend.entity.Product;
import havillah_backend.entity.Vendor;
import java.util.List;
import havillah_backend.exception.ResourceNotFoundException;
import havillah_backend.repository.CategoryRepository;
import havillah_backend.repository.ProductRepository;
import havillah_backend.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private VendorRepository vendorRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void vendorCreateUsesAuthenticatedVendorInsteadOfSubmittedVendorId() {
        Category category = new Category("Electronics", "Electronic devices");
        category.setId(1L);

        Vendor authenticatedVendor = new Vendor(
                "Vendor One", "vendor.one@example.com", "08000000001", "Lagos"
        );
        authenticatedVendor.setId(10L);

        ProductRequest request = validProductRequest();
        request.setVendorId(99L); // A vendor must not be able to choose another vendor.

        var authentication = new UsernamePasswordAuthenticationToken(
                "vendor.one@example.com",
                "not-used",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_VENDOR"))
        );

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(vendorRepository.findByUserEmail("vendor.one@example.com"))
                .thenReturn(Optional.of(authenticatedVendor));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(100L);
            return product;
        });

        ProductResponse response = productService.createProduct(request, authentication);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        assertEquals(10L, productCaptor.getValue().getVendor().getId());
        assertEquals(100L, response.getId());
        verify(vendorRepository, never()).findById(99L);
    }

    @Test
    void vendorCannotDeleteAnotherVendorsProduct() {
        Vendor productOwner = new Vendor(
                "Vendor Two", "vendor.two@example.com", "08000000002", "Abuja"
        );
        productOwner.setId(20L);

        Vendor authenticatedVendor = new Vendor(
                "Vendor One", "vendor.one@example.com", "08000000001", "Lagos"
        );
        authenticatedVendor.setId(10L);

        Product product = new Product();
        product.setId(200L);
        product.setVendor(productOwner);

        var authentication = new UsernamePasswordAuthenticationToken(
                "vendor.one@example.com",
                "not-used",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_VENDOR"))
        );

        when(productRepository.findById(200L)).thenReturn(Optional.of(product));
        when(vendorRepository.findByUserEmail("vendor.one@example.com"))
                .thenReturn(Optional.of(authenticatedVendor));

        assertThrows(
                AccessDeniedException.class,
                () -> productService.deleteProduct(200L, authentication)
        );

        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void adminMustProvideVendorIdWhenCreatingProduct() {
        Category category = new Category("Electronics", "Electronic devices");
        category.setId(1L);

        ProductRequest request = validProductRequest();
        request.setVendorId(null);

        var authentication = new UsernamePasswordAuthenticationToken(
                "admin@havillah.com",
                "not-used",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.createProduct(request, authentication)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(productRepository, never()).save(any(Product.class));
    }


    @Test
    void getAllProductsReturnsOnlyActiveProducts() {
        Product activeProduct = createTestProduct(1L);
        activeProduct.setId(1L);
        activeProduct.setActive(true);

        when(productRepository.findByActiveTrue())
                .thenReturn(List.of(activeProduct));

        List<ProductResponse> result = productService.getAllProducts();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());

        verify(productRepository).findByActiveTrue();
    }

    @Test
    void searchProductsReturnsOnlyActiveProducts() {
        Product activeProduct = createTestProduct(1L);
        activeProduct.setId(1L);
        activeProduct.setActive(true);

        when(productRepository.findByNameContainingIgnoreCaseAndActiveTrue("phone"))
                .thenReturn(List.of(activeProduct));

        List<ProductResponse> result = productService.searchProducts("phone");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());

        verify(productRepository)
                .findByNameContainingIgnoreCaseAndActiveTrue("phone");
    }

    @Test
    void getProductByIdThrowsExceptionWhenProductIsInactiveOrNotFound() {
        when(productRepository.findByIdAndActiveTrue(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProductById(99L)
        );

        verify(productRepository).findByIdAndActiveTrue(99L);
    }

    private Product createTestProduct(Long id) {
        Category category = new Category("Electronics", "Electronic devices");
        category.setId(1L);

        Vendor vendor = new Vendor(
                "Test Vendor",
                "vendor@example.com",
                "08000000001",
                "Lagos"
        );
        vendor.setId(10L);

        Product product = new Product();
        product.setId(id);
        product.setName("Test smartphone");
        product.setDescription("Test product");
        product.setPrice(new BigDecimal("150000.00"));
        product.setStockQuantity(5);
        product.setActive(true);
        product.setCategory(category);
        product.setVendor(vendor);

        return product;
    }

    private ProductRequest validProductRequest() {
        ProductRequest request = new ProductRequest();
        request.setName("Test smartphone");
        request.setDescription("A product used in a unit test");
        request.setPrice(new BigDecimal("150000.00"));
        request.setQuantity(5);
        request.setCategoryId(1L);
        return request;
    }
}
