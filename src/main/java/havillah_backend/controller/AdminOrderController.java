package havillah_backend.controller;

import havillah_backend.dto.OrderRequest;
import havillah_backend.dto.OrderResponse;
import havillah_backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> all() {
        return orderService.getAllOrders();
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id,
                                      @Valid @RequestBody OrderRequest.UpdateStatusRequest request) {
        return orderService.updateStatus(id, request);
    }
}