package havillah_backend.controller;

import havillah_backend.dto.WishlistItemResponse;
import havillah_backend.entity.User;
import havillah_backend.exception.ResourceNotFoundException;
import havillah_backend.repository.UserRepository;
import havillah_backend.service.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    public WishlistController(
            WishlistService wishlistService,
            UserRepository userRepository
    ) {
        this.wishlistService = wishlistService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{productId}")
    public ResponseEntity<WishlistItemResponse> addProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getAuthenticatedUserId(userDetails);

        WishlistItemResponse response =
                wishlistService.addProduct(userId, productId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> getWishlist(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getAuthenticatedUserId(userDetails);

        return ResponseEntity.ok(
                wishlistService.getWishlist(userId)
        );
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getAuthenticatedUserId(userDetails);

        wishlistService.removeProduct(userId, productId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearWishlist(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getAuthenticatedUserId(userDetails);

        wishlistService.clearWishlist(userId);

        return ResponseEntity.noContent().build();
    }

    private Long getAuthenticatedUserId(UserDetails userDetails) {

        User user = userRepository.findByEmail(
                userDetails.getUsername()
        ).orElseThrow(() ->
                new ResourceNotFoundException("User not found"));

        return user.getId();
    }
}