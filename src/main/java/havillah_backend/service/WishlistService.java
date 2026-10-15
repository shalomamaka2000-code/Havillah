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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistService(
            WishlistItemRepository wishlistItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WishlistItemResponse addProduct(Long userId, Long productId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found"));

        if (wishlistItemRepository.existsByUserIdAndProductId(
                userId, productId)) {
            throw new DuplicateResourceException(
                    "Product is already in your wishlist");
        }

        WishlistItem item = new WishlistItem(user, product);

        WishlistItem savedItem = wishlistItemRepository.save(item);

        return mapToResponse(savedItem);
    }

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        return wishlistItemRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void removeProduct(Long userId, Long productId) {

        WishlistItem item = wishlistItemRepository
                .findByUserIdAndProductId(userId, productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found in your wishlist"));

        wishlistItemRepository.delete(item);
    }

    @Transactional
    public void clearWishlist(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        wishlistItemRepository.deleteByUserId(userId);
    }

    private WishlistItemResponse mapToResponse(WishlistItem item) {

        Product product = item.getProduct();

        return new WishlistItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                item.getAddedAt()
        );
    }
}
