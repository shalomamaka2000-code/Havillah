
package havillah_backend.service;

import havillah_backend.dto.OrderRequest;
import havillah_backend.dto.OrderResponse;
import havillah_backend.entity.*;
import havillah_backend.exception.EmptyCartException;
import havillah_backend.exception.InsufficientStockException;
import havillah_backend.exception.InvalidOrderStateException;
import havillah_backend.repository.CartRepository;
import havillah_backend.repository.OrderRepository;
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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

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
    }

    @Test
    void checkoutRejectsEmptyCart() {
        when(cartRepository.findByUser(user))
                .thenReturn(Optional.of(cart));

        assertThrows(
                EmptyCartException.class,
                () -> orderService.checkout(
                        "customer@example.com",
                        new OrderRequest.CheckoutRequest("12 Example Street")
                )
        );

        verify(orderRepository, never()).save(any(Order.class));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void checkoutCreatesOrderDeductsStockAndClearsCart() {
        when(cartRepository.findByUser(user))
                .thenReturn(Optional.of(cart));

        addProductToCart(2);

        when(productRepository.findByIdForUpdate(30L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order saved = invocation.getArgument(0);
                    saved.setId(100L);
                    return saved;
                });

        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.checkout(
                "customer@example.com",
                new OrderRequest.CheckoutRequest("12 Example Street")
        );

        assertEquals(100L, response.id());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(new BigDecimal("30000.00"), response.totalAmount());
        assertEquals("12 Example Street", response.shippingAddress());
        assertEquals(1, response.items().size());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(8, product.getStockQuantity());
        assertTrue(cart.getItems().isEmpty());

        verify(orderRepository).save(any(Order.class));
        verify(cartRepository).save(cart);
    }

    @Test
    void checkoutRejectsQuantityAboveAvailableStock() {
        when(cartRepository.findByUser(user))
                .thenReturn(Optional.of(cart));

        product.setStockQuantity(1);
        addProductToCart(2);

        when(productRepository.findByIdForUpdate(30L))
                .thenReturn(Optional.of(product));

        assertThrows(
                InsufficientStockException.class,
                () -> orderService.checkout(
                        "customer@example.com",
                        new OrderRequest.CheckoutRequest("12 Example Street")
                )
        );

        verify(orderRepository, never()).save(any(Order.class));
        verify(cartRepository, never()).save(any(Cart.class));
        assertEquals(1, product.getStockQuantity());
        assertEquals(1, cart.getItems().size());
    }

    @Test
    void checkoutRejectsInactiveProduct() {
        when(cartRepository.findByUser(user))
                .thenReturn(Optional.of(cart));

        product.setActive(false);
        addProductToCart(1);

        when(productRepository.findByIdForUpdate(30L))
                .thenReturn(Optional.of(product));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.checkout(
                        "customer@example.com",
                        new OrderRequest.CheckoutRequest("12 Example Street")
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void cancellingPendingOrderRestoresProductStock() {
        Order order = createOrderWithItem(OrderStatus.PENDING, 2);
        product.setStockQuantity(8);

        when(orderRepository.findByIdAndUser(100L, user))
                .thenReturn(Optional.of(order));

        when(productRepository.findByIdForUpdate(30L))
                .thenReturn(Optional.of(product));

        OrderResponse response = orderService.cancelMyOrder(
                "customer@example.com",
                100L
        );

        assertEquals(OrderStatus.CANCELLED, response.status());
        assertEquals(10, product.getStockQuantity());
    }

    @Test
    void cancellingShippedOrderIsRejected() {
        Order order = createOrderWithItem(OrderStatus.SHIPPED, 2);
        product.setStockQuantity(8);

        when(orderRepository.findByIdAndUser(100L, user))
                .thenReturn(Optional.of(order));

        assertThrows(
                InvalidOrderStateException.class,
                () -> orderService.cancelMyOrder(
                        "customer@example.com",
                        100L
                )
        );

        assertEquals(8, product.getStockQuantity());
        verify(productRepository, never()).findByIdForUpdate(any());
    }

    private void addProductToCart(int quantity) {
        CartItem item = new CartItem();
        item.setId(40L);
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(quantity);

        cart.getItems().add(item);
    }

    private Order createOrderWithItem(OrderStatus status, int quantity) {
        Order order = new Order();
        order.setId(100L);
        order.setUser(user);
        order.setStatus(status);
        order.setShippingAddress("12 Example Street");
        order.setTotalAmount(
                product.getPrice().multiply(BigDecimal.valueOf(quantity))
        );

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductName(product.getName());
        item.setUnitPrice(product.getPrice());
        item.setQuantity(quantity);

        order.getItems().add(item);

        return order;
    }
}
