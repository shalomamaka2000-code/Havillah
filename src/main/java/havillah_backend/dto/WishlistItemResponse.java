package havillah_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter

public class WishlistItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private LocalDateTime addedAt;

    public WishlistItemResponse(
            Long id,
            Long productId,
            String productName,
            String description,
            BigDecimal price,
            String imageUrl,
            LocalDateTime addedAt
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.addedAt = addedAt;
    }
}
