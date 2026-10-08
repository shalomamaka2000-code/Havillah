package havillah_backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/*
This class catches exceptions thrown anywhere in the controllers/services
and converts them into one consistent JSON error format (ApiError),
instead of Spring's generic error page or a raw stack trace.
*/
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /*
    Helper method used by every handler below.
    It builds the ApiError body (timestamp, status, message, request path,
    optional field errors) so we don't repeat this code in each handler.
    */
    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest request,
                                           Map<String, String> errors) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                errors
        );
        return ResponseEntity.status(status).body(body);
    }

    /*
    404 Not Found.
    Triggered when we throw ResourceNotFoundException, e.g. when a product,
    vendor or category with the requested id does not exist.
    */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex,
                                                   HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    /*
    409 Conflict (business-level duplicate check).
    Triggered when our own code detects a duplicate before saving,
    e.g. existsByName() or existsByEmail() returns true.
    */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex,
                                                    HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /*
    409 Conflict (database-level violation).
    Safety net for constraints the database enforces: UNIQUE, NOT NULL or
    foreign key violations (e.g. deleting a category that still has products).
    The generic message is deliberate so we don't leak table/column details.
    */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest req) {
        return build(HttpStatus.CONFLICT,
                "Data conflicts with an existing record or database constraint", req, null);
    }

    /*
    400 Bad Request (validation failed).
    Triggered when a @Valid request body breaks a rule such as @NotBlank,
    @Email or @Size. Returns a field -> message map so the frontend can show
    each error next to the right input.
    */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, errors);
    }

    /*
    400 Bad Request (unreadable body).
    Triggered when the request body is missing, is not valid JSON,
    or has a value of the wrong type (e.g. text where a number is expected).
    */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex,
                                                     HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed or missing request body", req, null);
    }

    /*
    400 Bad Request (wrong parameter type).
    Triggered when a path variable or query param can't be converted,
    e.g. GET /products/abc when the id must be a Long.
    */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "'", req, null);
    }

    /*
    401 Unauthorized (login failed).
    Triggered by Spring Security when the email/password don't match.
    The message is intentionally vague so attackers can't tell whether
    the email exists or the password was wrong.
    */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex,
                                                         HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password", req, null);
    }

    /*
    403 Forbidden (not allowed).
    Triggered when the user is logged in but lacks the required role,
    e.g. a vendor calling an admin-only endpoint protected by @PreAuthorize.
    Must be handled explicitly, otherwise the catch-all below returns 500.
    */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                       HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action", req, null);
    }

    /*
    500 Internal Server Error (catch-all).
    Handles anything not matched above (bugs, NullPointerException, etc).
    We log the full exception for debugging, but send the client a generic
    message so stack traces and internals are never exposed.
    */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", req, null);
    }

    /*
400 Bad Request (not enough stock).
Triggered when the requested quantity is more than what is available.
*/
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> handleInsufficientStock(InsufficientStockException ex,
                                                            HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }
}