package havillah_backend.service;

import havillah_backend.dto.OrderItemResponse;
import havillah_backend.dto.OrderRequest;
import havillah_backend.dto.OrderResponse;
import havillah_backend.entity.*;
import havillah_backend.exception.EmptyCartException;
import havillah_backend.exception.InsufficientStockException;
import havillah_backend.exception.InvalidOrderStateException;
import havillah_backend.exception.ResourceNotFoundException;
import havillah_backend.repository.CartRepository;
import havillah_backend.repository.OrderRepository;
import havillah_backend.repository.ProductRepository;
import havillah_backend.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        UserRepository userRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /*
    Converts the cart into an order. Everything below runs in ONE transaction:
    if any step fails (e.g. out of stock), stock changes and the order are
    rolled back together and the cart stays untouched.
    */
    @Transactional
    public OrderResponse checkout(String email, OrderRequest.CheckoutRequest request) {
        User user = getUser(email);
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(EmptyCartException::new);
        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException();
        }

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.shippingAddress());

        // Lock products in a fixed (id) order so two simultaneous checkouts
        // can't deadlock each other
        List<CartItem> cartItems = cart.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId()))
                .toList();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + cartItem.getProduct().getId()));

            // Re-check stock now that the row is locked; the cart check may be stale
            if (cartItem.getQuantity() > product.getStockQuantity()) {
                throw new InsufficientStockException(
                        "Only " + product.getStockQuantity() + " unit(s) of '"
                                + product.getName() + "' available");
            }
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            order.getItems().add(orderItem);

            total = total.add(product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        // Empty the cart now that the order exists
        cart.getItems().clear();
        cartRepository.save(cart);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String email) {
        return orderRepository.findByUserOrderByCreatedAtDesc(getUser(email))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(String email, Long orderId) {
        return toResponse(findUserOrder(email, orderId));
    }

    // Customer cancel: only allowed while the order hasn't shipped
    @Transactional
    public OrderResponse cancelMyOrder(String email, Long orderId) {
        Order order = findUserOrder(email, orderId);
        changeStatus(order, OrderStatus.CANCELLED);
        return toResponse(order);
    }

    // ---------- Admin ----------

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderRequest.UpdateStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));
        changeStatus(order, request.status());
        return toResponse(order);
    }

    // ---------- Helpers ----------

    // findByIdAndUser means users can only ever load their own orders (404 otherwise)
    private Order findUserOrder(String email, Long orderId) {
        return orderRepository.findByIdAndUser(orderId, getUser(email))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));
    }

    private void changeStatus(Order order, OrderStatus next) {
        if (!order.getStatus().canTransitionTo(next)) {
            throw new InvalidOrderStateException(
                    "Cannot change order from " + order.getStatus() + " to " + next);
        }
        if (next == OrderStatus.CANCELLED) {
            restock(order);
        }
        order.setStatus(next);
    }

    // Puts the quantities of a cancelled order back into stock
    private void restock(Order order) {
        order.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId()))
                .forEach(item -> {
                    Product product = productRepository
                            .findByIdForUpdate(item.getProduct().getId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Product not found with id: " + item.getProduct().getId()));
                    product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                });
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getProduct().getId(),
                        i.getProductName(),
                        i.getUnitPrice(),
                        i.getQuantity(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalAmount(),
                order.getShippingAddress(), order.getCreatedAt(), items);
    }
}