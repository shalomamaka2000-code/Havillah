package havillah_backend.controller;

import havillah_backend.dto.WishlistItemResponse;
import havillah_backend.entity.User;
import havillah_backend.repository.UserRepository;
import havillah_backend.security.CustomUserDetailsService;
import havillah_backend.security.JwtService;
import havillah_backend.service.WishlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WishlistController.class)
@AutoConfigureMockMvc(addFilters = false)
class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WishlistService wishlistService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UsernamePasswordAuthenticationToken customerAuthentication() {
        UserDetails customer = org.springframework.security.core.userdetails.User
                .withUsername("customer@example.com")
                .password("password")
                .authorities("ROLE_CUSTOMER")
                .build();

        return new UsernamePasswordAuthenticationToken(
                customer, null, customer.getAuthorities()
        );
    }

    private User sampleUser() {
        User user = new User();
        user.setId(7L);
        user.setEmail("customer@example.com");
        return user;
    }

    private WishlistItemResponse sampleWishlistItem() {
        return new WishlistItemResponse(
                20L,
                5L,
                "Wireless Headphones",
                "Noise-cancelling wireless headphones",
                new BigDecimal("25000.00"),
                "https://example.com/headphones.jpg",
                LocalDateTime.of(2026, 11, 14, 10, 0)
        );
    }

    @Test
    void addProductReturnsCreatedWishlistItem() throws Exception {
        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(sampleUser()));
        when(wishlistService.addProduct(7L, 5L)).thenReturn(sampleWishlistItem());

        mockMvc.perform(post("/api/wishlist/5")
                        .principal(customerAuthentication()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.productId").value(5))
                .andExpect(jsonPath("$.productName").value("Wireless Headphones"))
                .andExpect(jsonPath("$.price").value(25000.00));

        verify(wishlistService).addProduct(7L, 5L);
    }

    @Test
    void getWishlistReturnsCurrentUsersWishlist() throws Exception {
        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(sampleUser()));
        when(wishlistService.getWishlist(7L))
                .thenReturn(List.of(sampleWishlistItem()));

        mockMvc.perform(get("/api/wishlist")
                        .principal(customerAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(5))
                .andExpect(jsonPath("$[0].productName").value("Wireless Headphones"));

        verify(wishlistService).getWishlist(7L);
    }

    @Test
    void removeProductReturnsNoContent() throws Exception {
        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(sampleUser()));
        doNothing().when(wishlistService).removeProduct(7L, 5L);

        mockMvc.perform(delete("/api/wishlist/5")
                        .principal(customerAuthentication()))
                .andExpect(status().isNoContent());

        verify(wishlistService).removeProduct(7L, 5L);
    }

    @Test
    void clearWishlistReturnsNoContent() throws Exception {
        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(sampleUser()));
        doNothing().when(wishlistService).clearWishlist(7L);

        mockMvc.perform(delete("/api/wishlist")
                        .principal(customerAuthentication()))
                .andExpect(status().isNoContent());

        verify(wishlistService).clearWishlist(7L);
    }
}
