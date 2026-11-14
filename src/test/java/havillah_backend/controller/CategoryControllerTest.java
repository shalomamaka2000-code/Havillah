package havillah_backend.controller;

import havillah_backend.dto.CategoryRequest;
import havillah_backend.dto.CategoryResponse;
import havillah_backend.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    private CategoryResponse sampleCategory() {
        return new CategoryResponse(1L, "Electronics", "Electronic devices and accessories");
    }

    @Test
    void getAllCategoriesReturnsCategoryList() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(sampleCategory()));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Electronics"))
                .andExpect(jsonPath("$[0].description").value("Electronic devices and accessories"));

        verify(categoryService).getAllCategories();
    }

    @Test
    void getCategoryByIdReturnsCategory() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(sampleCategory());

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCategoryReturnsCreatedForAdmin() throws Exception {
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(sampleCategory());

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Electronics",
                                  "description": "Electronic devices and accessories"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"));

        verify(categoryService).createCategory(any(CategoryRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCategoryReturnsBadRequestWhenRequiredFieldsAreBlank() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "description": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(any(CategoryRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCategoryReturnsUpdatedCategory() throws Exception {
        when(categoryService.updateCategory(eq(1L), any(CategoryRequest.class)))
                .thenReturn(new CategoryResponse(1L, "Computers", "Computers and laptops"));

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Computers",
                                  "description": "Computers and laptops"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Computers"));

        verify(categoryService).updateCategory(eq(1L), any(CategoryRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCategoryReturnsNoContent() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(1L);
    }
}
