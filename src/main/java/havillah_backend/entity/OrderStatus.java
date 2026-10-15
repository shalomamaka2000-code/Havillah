package havillah_backend.entity;

public enum OrderStatus {
    PENDING, PAID, SHIPPED, DELIVERED, CANCELLED;

    // Defines which status changes are allowed, so an order can't jump
    // from DELIVERED back to PENDING, for example
    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == PAID || next == CANCELLED;
            case PAID -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
