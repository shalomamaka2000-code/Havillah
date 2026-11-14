package havillah_backend.controller;

import havillah_backend.dto.ProductRequest;
import havillah_backend.dto.ProductResponse;
import havillah_backend.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private ProductResponse sampleProduct() {
        return new ProductResponse(
                1L,
                "Wireless Headphones",
                "Bluetooth headphones",
                new BigDecimal("25000.00"),
                15,
                "https://example.com/headphones.jpg",
                true,
                LocalDateTime.of(2026, 11, 14, 9, 0),
                2L,
                "Electronics",
                3L,
                "Tech Store"
        );
    }

    @Test
    void getAllProductsReturnsProductList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(sampleProduct()));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Wireless Headphones"))
                .andExpect(jsonPath("$[0].categoryName").value("Electronics"));

        verify(productService).getAllProducts();
    }

    @Test
    void getProductByIdReturnsProduct() throws Exception {
        when(productService.getProductById(1L)).thenReturn(sampleProduct());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Wireless Headphones"))
                .andExpect(jsonPath("$.price").value(25000.00));
    }

    @Test
    void searchProductsReturnsMatchingProducts() throws Exception {
        when(productService.searchProducts("headphones")).thenReturn(List.of(sampleProduct()));

        mockMvc.perform(get("/api/products/search").param("name", "headphones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Wireless Headphones"));

        verify(productService).searchProducts("headphones");
    }

    @Test
    void getProductsPageReturnsPagedResults() throws Exception {
        when(productService.getProductsPage(
                eq("headphones"), eq(2L), eq(3L), any(), any(), eq(true),
                eq(0), eq(12), eq("newest"), eq("desc")
        )).thenReturn(new PageImpl<>(List.of(sampleProduct()), PageRequest.of(0, 12), 1));

        mockMvc.perform(get("/api/products/page")
                        .param("name", "headphones")
                        .param("categoryId", "2")
                        .param("vendorId", "3")
                        .param("inStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Wireless Headphones"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void createProductReturnsCreatedForVendor() throws Exception {
        when(productService.createProduct(any(ProductRequest.class), any()))
                .thenReturn(sampleProduct());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Wireless Headphones",
                                  "description": "Bluetooth headphones",
                                  "price": 25000.00,
                                  "quantity": 15,
                                  "imageUrl": "https://example.com/headphones.jpg",
                                  "categoryId": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Wireless Headphones"));

        verify(productService).createProduct(any(ProductRequest.class), any());
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void createProductReturnsBadRequestWhenRequiredFieldsAreInvalid() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "price": 0,
                                  "quantity": -1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any(ProductRequest.class), any());
    }
}
