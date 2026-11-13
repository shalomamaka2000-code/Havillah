package havillah_backend.service;

import havillah_backend.dto.WishlistItemResponse;
import havillah_backend.entity.Product;
import havillah_backend.entity.User;
import havillah_backend.entity.WishlistItem;
import havillah_backend.exception.DuplicateResourceException;
import havillah_backend.exception.ResourceNotFoundException;
import havillah_backend.repository.ProductRepository;
import havillah_backend.repository.UserRepository;
import havillah_backend.repository.WishlistItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(10L);
        user.setEmail("customer@example.com");

        product = new Product();
        product.setId(30L);
        product.setName("Wireless Headphones");
        product.setDescription("Bluetooth headphones");
        product.setPrice(new BigDecimal("15000.00"));
        product.setStockQuantity(10);
        product.setImageUrl("headphones.jpg");
        product.setActive(true);
    }

    @Test
    void addProductToWishlistSuccessfully() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(productRepository.findById(30L)).thenReturn(Optional.of(product));
        when(wishlistItemRepository.existsByUserIdAndProductId(10L, 30L))
                .thenReturn(false);
        when(wishlistItemRepository.save(any(WishlistItem.class)))
                .thenAnswer(invocation -> {
                    WishlistItem item = invocation.getArgument(0);
                    item.setId(100L);
                    return item;
                });

        WishlistItemResponse response = wishlistService.addProduct(10L, 30L);

        assertEquals(100L, response.getId());
        assertEquals(30L, response.getProductId());
        assertEquals("Wireless Headphones", response.getProductName());
        assertEquals(new BigDecimal("15000.00"), response.getPrice());
        assertEquals("headphones.jpg", response.getImageUrl());

        verify(wishlistItemRepository).save(any(WishlistItem.class));
    }

    @Test
    void addingDuplicateProductIsRejected() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(productRepository.findById(30L)).thenReturn(Optional.of(product));
        when(wishlistItemRepository.existsByUserIdAndProductId(10L, 30L))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> wishlistService.addProduct(10L, 30L)
        );

        verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
    }

    @Test
    void getWishlistReturnsCustomersItems() {
        WishlistItem item = new WishlistItem(user, product);
        item.setId(100L);

        when(userRepository.existsById(10L)).thenReturn(true);
        when(wishlistItemRepository.findByUserId(10L))
                .thenReturn(List.of(item));

        List<WishlistItemResponse> response = wishlistService.getWishlist(10L);

        assertEquals(1, response.size());
        assertEquals(30L, response.get(0).getProductId());
        assertEquals("Wireless Headphones", response.get(0).getProductName());
    }

    @Test
    void removeProductFromWishlistSuccessfully() {
        WishlistItem item = new WishlistItem(user, product);
        item.setId(100L);

        when(wishlistItemRepository.findByUserIdAndProductId(10L, 30L))
                .thenReturn(Optional.of(item));

        wishlistService.removeProduct(10L, 30L);

        verify(wishlistItemRepository).delete(item);
    }

    @Test
    void removingProductNotInWishlistIsRejected() {
        when(wishlistItemRepository.findByUserIdAndProductId(10L, 30L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> wishlistService.removeProduct(10L, 30L)
        );

        verify(wishlistItemRepository, never()).delete(any(WishlistItem.class));
    }
}
