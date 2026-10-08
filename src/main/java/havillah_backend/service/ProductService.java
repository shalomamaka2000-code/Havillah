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
import org.springframework.stereotype.Service;

import java.util.List;

@Service

public class ProductService {

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

    public ProductResponse createProduct(ProductRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found")
                );

        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Vendor not found")
                );

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setVendor(vendor);

        Product savedProduct = productRepository.save(product);

        return mapToResponse(savedProduct);
    }
/*
This is the search parameter from ProductRepository now add the
endpoint to ProductController
*/
    public List<ProductResponse> searchProducts(String name) {
        return productRepository
                .findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ProductResponse> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProductResponse getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found")
                );

        return mapToResponse(product);
    }

    public ProductResponse updateProduct(
            Long id,
            ProductRequest request
    ) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found")
                );

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found")
                );

        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Vendor not found")
                );

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setVendor(vendor);

        Product updatedProduct = productRepository.save(product);

        return mapToResponse(updatedProduct);
    }

    public void deleteProduct(Long id) {

        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found");
        }

        productRepository.deleteById(id);
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

