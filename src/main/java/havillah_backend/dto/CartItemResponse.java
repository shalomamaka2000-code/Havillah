package havillah_backend.dto;

import java.math.BigDecimal;

public record CartItemResponse(
            Long itemId,
            Long productId,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal
    ) {}
