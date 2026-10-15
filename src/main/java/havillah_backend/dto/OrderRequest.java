package havillah_backend.dto;

import havillah_backend.entity.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OrderRequest {

    public record CheckoutRequest(
            @NotBlank(message = "Shipping address is required") String shippingAddress
    ) {}

    public record UpdateStatusRequest(
            @NotNull(message = "Status is required") OrderStatus status
    ) {}
}