package ddia.example.ddia.exception;

/**
 * Custom exception for payment processing errors.
 *
 * This exception is thrown when payment processing fails due to various reasons
 * such as network timeouts, gateway errors, insufficient funds, etc.
 *
 * The Resilience4j circuit breaker is configured to react to this exception.
 */
public class PaymentException extends RuntimeException {

    private final String errorCode;
    private final String orderId;

    /**
     * Creates a payment exception with a message.
     */
    public PaymentException(String message) {
        super(message);
        this.errorCode = null;
        this.orderId = null;
    }

    /**
     * Creates a payment exception with a message and cause.
     */
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.orderId = null;
    }

    /**
     * Creates a payment exception with detailed information.
     */
    public PaymentException(String message, String errorCode, String orderId) {
        super(message);
        this.errorCode = errorCode;
        this.orderId = orderId;
    }

    /**
     * Creates a payment exception with detailed information and cause.
     */
    public PaymentException(String message, String errorCode, String orderId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.orderId = orderId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getOrderId() {
        return orderId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PaymentException{");
        sb.append("message='").append(getMessage()).append("'");
        if (errorCode != null) {
            sb.append(", errorCode='").append(errorCode).append("'");
        }
        if (orderId != null) {
            sb.append(", orderId='").append(orderId).append("'");
        }
        sb.append("}");
        return sb.toString();
    }
}