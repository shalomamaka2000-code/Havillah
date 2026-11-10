package havillah_backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    // Legacy single-image field; still supported for existing clients.
    private String imageUrl;

    // Optional gallery. The first URL is also used as imageUrl.
    @Size(max = 10, message = "A product can have at most 10 images")
    private List<@NotBlank(message = "Image URLs cannot be blank") String> imageUrls;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    // Admins may specify a vendor. Vendor accounts always use their linked profile.
    private Long vendorId;

    public ProductRequest() {
    }
}
