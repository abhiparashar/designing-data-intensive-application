package ddia.example.ddia.model;

/**
 * Order response model representing the result of an order processing operation.
 *
 * This immutable class contains the order processing result with status information,
 * payment details, and optional error messages.
 */
public class OrderResponse {

    /**
     * Order status constants
     */
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final String orderId;
    private final String status;
    private final String paymentId;
    private final String errorMessage;
    private final long timestamp;
    private final double amount;
    private final String trackingNumber;

    /**
     * Creates a basic order response.
     */
    public OrderResponse(String orderId, String status, String paymentId, String errorMessage) {
        this(orderId, status, paymentId, errorMessage, System.currentTimeMillis(), 0.0, null);
    }

    /**
     * Creates a complete order response with all details.
     */
    public OrderResponse(String orderId, String status, String paymentId, String errorMessage,
                         long timestamp, double amount, String trackingNumber) {
        this.orderId = orderId;
        this.status = status;
        this.paymentId = paymentId;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
        this.amount = amount;
        this.trackingNumber = trackingNumber;
    }

    /**
     * Default constructor for JSON deserialization.
     */
    public OrderResponse() {
        this.orderId = null;
        this.status = null;
        this.paymentId = null;
        this.errorMessage = null;
        this.timestamp = 0L;
        this.amount = 0.0;
        this.trackingNumber = null;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getAmount() {
        return amount;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    /**
     * Checks if the order was successfully confirmed.
     */
    public boolean isConfirmed() {
        return STATUS_CONFIRMED.equals(status);
    }

    /**
     * Checks if the order failed.
     */
    public boolean isFailed() {
        return STATUS_FAILED.equals(status) || STATUS_ERROR.equals(status);
    }

    /**
     * Checks if the order is pending.
     */
    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    /**
     * Checks if the order was cancelled.
     */
    public boolean isCancelled() {
        return STATUS_CANCELLED.equals(status);
    }

    @Override
    public String toString() {
        return String.format("OrderResponse{orderId='%s', status='%s', paymentId='%s', amount=%.2f, timestamp=%d, errorMessage='%s'}",
                orderId, status, paymentId, amount, timestamp, errorMessage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrderResponse that = (OrderResponse) o;

        if (timestamp != that.timestamp) return false;
        if (Double.compare(that.amount, amount) != 0) return false;
        if (orderId != null ? !orderId.equals(that.orderId) : that.orderId != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (paymentId != null ? !paymentId.equals(that.paymentId) : that.paymentId != null) return false;
        if (errorMessage != null ? !errorMessage.equals(that.errorMessage) : that.errorMessage != null)
            return false;
        return trackingNumber != null ? trackingNumber.equals(that.trackingNumber) : that.trackingNumber == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = orderId != null ? orderId.hashCode() : 0;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (paymentId != null ? paymentId.hashCode() : 0);
        result = 31 * result + (errorMessage != null ? errorMessage.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        temp = Double.doubleToLongBits(amount);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (trackingNumber != null ? trackingNumber.hashCode() : 0);
        return result;
    }
}