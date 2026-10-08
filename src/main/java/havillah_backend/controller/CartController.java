package havillah_backend.controller;

import havillah_backend.dto.CartRequest;
import havillah_backend.dto.CartResponse;
import havillah_backend.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
    @RequestMapping("/api/cart")
    public class CartController {

        private final CartService cartService;

        public CartController(CartService cartService) {
            this.cartService = cartService;
        }

        // authentication.getName() is the logged-in user's email (the JWT subject)
        @GetMapping
        public CartResponse getCart(Authentication authentication) {
            return cartService.getCart(authentication.getName());
        }

        @PostMapping("/items")
        public ResponseEntity<CartResponse> addItem(Authentication authentication,
                                                    @Valid @RequestBody CartRequest.AddToCartRequest request) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(cartService.addItem(authentication.getName(), request));
        }

        @PutMapping("/items/{itemId}")
        public CartResponse updateQuantity(Authentication authentication,
                                           @PathVariable Long itemId,
                                           @Valid @RequestBody CartRequest.UpdateCartItemRequest request) {
            return cartService.updateQuantity(authentication.getName(), itemId, request);
        }

        @DeleteMapping("/items/{itemId}")
        public CartResponse removeItem(Authentication authentication, @PathVariable Long itemId) {
            return cartService.removeItem(authentication.getName(), itemId);
        }

        @DeleteMapping
        public ResponseEntity<Void> clearCart(Authentication authentication) {
            cartService.clearCart(authentication.getName());
            return ResponseEntity.noContent().build();
        }
    }

