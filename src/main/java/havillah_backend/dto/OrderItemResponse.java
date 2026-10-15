package havillah_backend.dto;
import havillah_backend.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderItemResponse(
        Long productId, String productName,
        BigDecimal unitPrice, int quantity, BigDecimal subtotal
) {}