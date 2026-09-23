package havillah_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter

public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String imageUrl;
    private Boolean active;
    private LocalDateTime createdAt;

    private Long categoryId;
    private String categoryName;

    private Long vendorId;
    private String vendorBusinessName;

    public ProductResponse(
            Long id,
            String name,
            String description,
            BigDecimal price,
            Integer quantity,
            String imageUrl,
            Boolean active,
            LocalDateTime createdAt,
            Long categoryId,
            String categoryName,
            Long vendorId,
            String vendorBusinessName
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.active = active;
        this.createdAt = createdAt;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.vendorId = vendorId;
        this.vendorBusinessName = vendorBusinessName;
    }

}