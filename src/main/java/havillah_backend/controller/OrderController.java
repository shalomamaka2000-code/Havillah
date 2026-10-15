package havillah_backend.controller;

import havillah_backend.dto.OrderRequest;
import havillah_backend.dto.OrderResponse;
import havillah_backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> checkout(
            Authentication authentication,
            @Valid @RequestBody OrderRequest.CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.checkout(authentication.getName(), request));
    }

    @GetMapping
    public List<OrderResponse> myOrders(Authentication authentication) {
        return orderService.getMyOrders(authentication.getName());
    }

    @GetMapping("/{id}")
    public OrderResponse myOrder(Authentication authentication, @PathVariable Long id) {
        return orderService.getMyOrder(authentication.getName(), id);
    }

    @PutMapping("/{id}/cancel")
    public OrderResponse cancel(Authentication authentication, @PathVariable Long id) {
        return orderService.cancelMyOrder(authentication.getName(), id);
    }
}