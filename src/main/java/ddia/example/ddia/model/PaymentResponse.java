package ddia.example.ddia.model;

/**
 * Payment response model representing the result of a payment processing operation.
 *
 * This immutable class contains the payment result with transaction details,
 * status information, and optional error messages.
 */
public class PaymentResponse {

    /**
     * Payment status constants
     */
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PENDING_MANUAL_REVIEW = "PENDING_MANUAL_REVIEW";

    private final String transactionId;
    private final double amount;
    private final String status;
    private final long timestamp;
    private final String errorMessage;
    private final String gatewayResponse;

    /**
     * Creates a successful payment response.
     */
    public PaymentResponse(String transactionId, double amount, String status, long timestamp) {
        this(transactionId, amount, status, timestamp, null, null);
    }

    /**
     * Creates a payment response with error message.
     */
    public PaymentResponse(String transactionId, double amount, String status,
                           long timestamp, String errorMessage) {
        this(transactionId, amount, status, timestamp, errorMessage, null);
    }

    /**
     * Creates a complete payment response with all details.
     */
    public PaymentResponse(String transactionId, double amount, String status,
                           long timestamp, String errorMessage, String gatewayResponse) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = status;
        this.timestamp = timestamp;
        this.errorMessage = errorMessage;
        this.gatewayResponse = gatewayResponse;
    }

    /**
     * Default constructor for JSON deserialization.
     */
    public PaymentResponse() {
        this.transactionId = null;
        this.amount = 0.0;
        this.status = null;
        this.timestamp = 0L;
        this.errorMessage = null;
        this.gatewayResponse = null;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    /**
     * Checks if the payment was successful.
     */
    public boolean isSuccessful() {
        return STATUS_COMPLETED.equals(status);
    }

    /**
     * Checks if the payment failed.
     */
    public boolean isFailed() {
        return STATUS_FAILED.equals(status);
    }

    /**
     * Checks if the payment is pending.
     */
    public boolean isPending() {
        return STATUS_PENDING.equals(status) || STATUS_PENDING_MANUAL_REVIEW.equals(status);
    }

    @Override
    public String toString() {
        return String.format("PaymentResponse{transactionId='%s', amount=%.2f, status='%s', timestamp=%d, errorMessage='%s'}",
                transactionId, amount, status, timestamp, errorMessage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaymentResponse that = (PaymentResponse) o;

        if (Double.compare(that.amount, amount) != 0) return false;
        if (timestamp != that.timestamp) return false;
        if (transactionId != null ? !transactionId.equals(that.transactionId) : that.transactionId != null)
            return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (errorMessage != null ? !errorMessage.equals(that.errorMessage) : that.errorMessage != null)
            return false;
        return gatewayResponse != null ? gatewayResponse.equals(that.gatewayResponse) : that.gatewayResponse == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = transactionId != null ? transactionId.hashCode() : 0;
        temp = Double.doubleToLongBits(amount);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (errorMessage != null ? errorMessage.hashCode() : 0);
        result = 31 * result + (gatewayResponse != null ? gatewayResponse.hashCode() : 0);
        return result;
    }
}