package havillah_backend.service;

import havillah_backend.dto.CartRequest;
import havillah_backend.dto.CartResponse;
import havillah_backend.entity.Cart;
import havillah_backend.entity.CartItem;
import havillah_backend.entity.Product;
import havillah_backend.entity.User;
import havillah_backend.exception.InsufficientStockException;
import havillah_backend.exception.ResourceNotFoundException;
import havillah_backend.repository.CartRepository;
import havillah_backend.repository.ProductRepository;
import havillah_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(10L);
        user.setEmail("customer@example.com");

        cart = new Cart();
        cart.setId(20L);
        cart.setUser(user);
        cart.setItems(new ArrayList<>());

        product = new Product();
        product.setId(30L);
        product.setName("Wireless Headphones");
        product.setPrice(new BigDecimal("15000.00"));
        product.setStockQuantity(10);
        product.setActive(true);

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
    }

    @Test
    void addItemAddsProductAndCalculatesCartTotal() {
        when(productRepository.findById(30L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addItem(
                "customer@example.com",
                new CartRequest.AddToCartRequest(30L, 2)
        );

        assertEquals(1, response.items().size());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(new BigDecimal("30000.00"), response.items().get(0).subtotal());
        assertEquals(new BigDecimal("30000.00"), response.totalAmount());
        assertSame(product, cart.getItems().get(0).getProduct());
    }

    @Test
    void addingExistingProductIncreasesItsQuantity() {
        CartItem existingItem = new CartItem();
        existingItem.setId(40L);
        existingItem.setCart(cart);
        existingItem.setProduct(product);
        existingItem.setQuantity(2);
        cart.getItems().add(existingItem);

        when(productRepository.findById(30L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addItem(
                "customer@example.com",
                new CartRequest.AddToCartRequest(30L, 3)
        );

        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).getQuantity());
        assertEquals(5, response.items().get(0).quantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void addItemRejectsQuantityAboveAvailableStock() {
        product.setStockQuantity(3);
        when(productRepository.findById(30L)).thenReturn(Optional.of(product));

        assertThrows(
                InsufficientStockException.class,
                () -> cartService.addItem(
                        "customer@example.com",
                        new CartRequest.AddToCartRequest(30L, 4)
                )
        );

        verify(cartRepository, never()).save(any(Cart.class));
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void addItemRejectsInactiveProduct() {
        product.setActive(false);
        when(productRepository.findById(30L)).thenReturn(Optional.of(product));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cartService.addItem(
                        "customer@example.com",
                        new CartRequest.AddToCartRequest(30L, 1)
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addItemRejectsProductThatDoesNotExist() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.addItem(
                        "customer@example.com",
                        new CartRequest.AddToCartRequest(999L, 1)
                )
        );

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void updateQuantityRejectsQuantityAboveAvailableStock() {
        CartItem existingItem = new CartItem();
        existingItem.setId(40L);
        existingItem.setCart(cart);
        existingItem.setProduct(product);
        existingItem.setQuantity(2);
        cart.getItems().add(existingItem);
        product.setStockQuantity(3);

        assertThrows(
                InsufficientStockException.class,
                () -> cartService.updateQuantity(
                        "customer@example.com",
                        40L,
                        new CartRequest.UpdateCartItemRequest(4)
                )
        );

        assertEquals(2, existingItem.getQuantity());
        verify(cartRepository, never()).save(any(Cart.class));
    }
}
