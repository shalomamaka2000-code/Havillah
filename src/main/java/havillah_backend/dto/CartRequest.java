package havillah_backend.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartRequest {
    public record AddToCartRequest(
            @NotNull(message = "Product id is required") Long productId,
            @Min(value = 1, message = "Quantity must be at least 1") int quantity
    ) {}

    public record UpdateCartItemRequest(
            @Min(value = 1, message = "Quantity must be at least 1") int quantity
    ) {}
}
