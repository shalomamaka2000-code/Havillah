package havillah_backend.service;

import havillah_backend.dto.CategoryRequest;
import havillah_backend.dto.CategoryResponse;
import havillah_backend.entity.Category;
import havillah_backend.exception.DuplicateResourceException;
import havillah_backend.exception.ResourceNotFoundException;
import havillah_backend.repository.CategoryRepository;
import havillah_backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category("Electronics", "Electronic devices");
        category.setId(10L);
    }

    @Test
    void createCategorySuccessfully() {
        CategoryRequest request = request("Electronics", "Electronic devices");

        when(categoryRepository.existsByName("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> {
                    Category saved = invocation.getArgument(0);
                    saved.setId(10L);
                    return saved;
                });

        CategoryResponse response = categoryService.createCategory(request);

        assertEquals(10L, response.getId());
        assertEquals("Electronics", response.getName());
        assertEquals("Electronic devices", response.getDescription());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createDuplicateCategoryIsRejected() {
        CategoryRequest request = request("Electronics", "Electronic devices");

        when(categoryRepository.existsByName("Electronics")).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> categoryService.createCategory(request)
        );

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void getAllCategoriesReturnsCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<CategoryResponse> response = categoryService.getAllCategories();

        assertEquals(1, response.size());
        assertEquals("Electronics", response.get(0).getName());
        assertEquals("Electronic devices", response.get(0).getDescription());
    }

    @Test
    void getCategoryByIdReturnsCategory() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.getCategoryById(10L);

        assertEquals(10L, response.getId());
        assertEquals("Electronics", response.getName());
    }

    @Test
    void getMissingCategoryIsRejected() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.getCategoryById(999L)
        );
    }

    @Test
    void updateCategorySuccessfully() {
        CategoryRequest request = request("Home Appliances", "Appliances for the home");

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName("Home Appliances")).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponse response = categoryService.updateCategory(10L, request);

        assertEquals("Home Appliances", response.getName());
        assertEquals("Appliances for the home", response.getDescription());
        verify(categoryRepository).save(category);
    }

    @Test
    void deletingCategoryWithAssignedProductsIsRejected() {
        when(categoryRepository.existsById(10L)).thenReturn(true);
        when(productRepository.existsByCategory_Id(10L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.deleteCategory(10L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void deletingCategoryWithoutProductsSucceeds() {
        when(categoryRepository.existsById(10L)).thenReturn(true);
        when(productRepository.existsByCategory_Id(10L)).thenReturn(false);

        assertDoesNotThrow(() -> categoryService.deleteCategory(10L));

        verify(categoryRepository).deleteById(10L);
    }

    @Test
    void deletingMissingCategoryIsRejected() {
        when(categoryRepository.existsById(999L)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.deleteCategory(999L)
        );

        verify(productRepository, never()).existsByCategory_Id(anyLong());
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    private CategoryRequest request(String name, String description) {
        CategoryRequest request = new CategoryRequest();
        request.setName(name);
        request.setDescription(description);
        return request;
    }
}
