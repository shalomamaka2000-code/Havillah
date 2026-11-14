package havillah_backend.controller;

import havillah_backend.dto.CartItemResponse;
import havillah_backend.dto.CartRequest;
import havillah_backend.dto.CartResponse;
import havillah_backend.security.CustomUserDetailsService;
import havillah_backend.security.JwtService;
import havillah_backend.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "customer@example.com")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CartResponse sampleCart() {
        return new CartResponse(
                List.of(new CartItemResponse(
                        10L,
                        5L,
                        "Wireless Headphones",
                        new BigDecimal("25000.00"),
                        2,
                        new BigDecimal("50000.00")
                )),
                new BigDecimal("50000.00")
        );
    }

    @Test
    void getCartReturnsCurrentUsersCart() throws Exception {
        when(cartService.getCart("customer@example.com")).thenReturn(sampleCart());

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].itemId").value(10))
                .andExpect(jsonPath("$.items[0].productName").value("Wireless Headphones"))
                .andExpect(jsonPath("$.totalAmount").value(50000.00));

        verify(cartService).getCart("customer@example.com");
    }

    @Test
    void addItemReturnsCreatedCart() throws Exception {
        when(cartService.addItem(eq("customer@example.com"), any(CartRequest.AddToCartRequest.class)))
                .thenReturn(sampleCart());

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 5,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalAmount").value(50000.00));

        verify(cartService).addItem(eq("customer@example.com"), any(CartRequest.AddToCartRequest.class));
    }

    @Test
    void addItemReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": null,
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).addItem(any(), any(CartRequest.AddToCartRequest.class));
    }

    @Test
    void updateQuantityReturnsUpdatedCart() throws Exception {
        when(cartService.updateQuantity(
                eq("customer@example.com"),
                eq(10L),
                any(CartRequest.UpdateCartItemRequest.class)
        )).thenReturn(sampleCart());

        mockMvc.perform(put("/api/cart/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        verify(cartService).updateQuantity(
                eq("customer@example.com"),
                eq(10L),
                any(CartRequest.UpdateCartItemRequest.class)
        );
    }

    @Test
    void removeItemReturnsUpdatedCart() throws Exception {
        when(cartService.removeItem("customer@example.com", 10L)).thenReturn(
                new CartResponse(List.of(), BigDecimal.ZERO)
        );

        mockMvc.perform(delete("/api/cart/items/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalAmount").value(0));

        verify(cartService).removeItem("customer@example.com", 10L);
    }

    @Test
    void clearCartReturnsNoContent() throws Exception {
        doNothing().when(cartService).clearCart("customer@example.com");

        mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isNoContent());

        verify(cartService).clearCart("customer@example.com");
    }
}
