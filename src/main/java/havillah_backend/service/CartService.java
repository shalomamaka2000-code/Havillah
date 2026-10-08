package havillah_backend.service;


import havillah_backend.dto.CartItemResponse;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository,
                       UserRepository userRepository,
                       ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    // Finds the user's cart, creating an empty one on first use
    private Cart getOrCreateCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(String email) {
        return toResponse(getOrCreateCart(email));
    }

    @Transactional
    public CartResponse addItem(String email, CartRequest.AddToCartRequest request) {
        Cart cart = getOrCreateCart(email);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.productId()));

        // If the product is already in the cart, increase its quantity instead
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        int newQuantity;
        newQuantity = request.quantity() + (item != null ? item.getQuantity() : 0);
        checkStock(product, newQuantity);

        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            cart.getItems().add(item);
        }
        item.setQuantity(newQuantity);

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateQuantity(String email, Long itemId, CartRequest.UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(email);
        CartItem item = findItem(cart, itemId);
        checkStock(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(String email, Long itemId) {
        Cart cart = getOrCreateCart(email);
        CartItem item = findItem(cart, itemId);
        cart.getItems().remove(item); // orphanRemoval deletes the row
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public void clearCart(String email) {
        Cart cart = getOrCreateCart(email);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    // Looks up the item inside THIS user's cart, so users can't touch each other's items
    private CartItem findItem(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + itemId));
    }

    private void checkStock(Product product, int quantity) {
        if (quantity > product.getStockQuantity()) {
            throw new InsufficientStockException(
                    "Only " + product.getStockQuantity() + " unit(s) of '"
                            + product.getName() + "' available");
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(i -> new CartItemResponse(
                        i.getId(),
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getProduct().getPrice(),
                        i.getQuantity(),
                        i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items, total);
    }
}