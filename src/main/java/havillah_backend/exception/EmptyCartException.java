package havillah_backend.exception;

public class EmptyCartException extends RuntimeException {
    public EmptyCartException() { super("Your cart is empty"); }
}

