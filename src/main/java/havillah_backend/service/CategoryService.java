package havillah_backend.service;

import havillah_backend.dto.CategoryRequest;
import havillah_backend.dto.CategoryResponse;
import havillah_backend.entity.Category;
import havillah_backend.exception.DuplicateResourceException;
import havillah_backend.exception.ResourceNotFoundException;
import havillah_backend.repository.CategoryRepository;
import havillah_backend.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category already exists");
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return toResponse(categoryRepository.save(category));
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return toResponse(category);
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (categoryRepository.existsByName(request.getName())
                && !category.getName().equalsIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Category name already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found");
        }

        if (productRepository.existsByCategory_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete this category because products are still assigned to it. Reassign those products first."
            );
        }

        categoryRepository.deleteById(id);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}