package ddia.example.ddia.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Payment request model representing a payment processing request.
 *
 * This class is immutable and uses validation annotations to ensure data integrity.
 */
public class PaymentRequest {

    @NotBlank(message = "Order ID cannot be blank")
    private final String orderId;

    @Positive(message = "Amount must be positive")
    private final double amount;

    @NotBlank(message = "Card token cannot be blank")
    private final String cardToken;

    /**
     * Creates a new payment request.
     *
     * @param orderId The unique order identifier
     * @param amount The payment amount (must be positive)
     * @param cardToken The tokenized card information
     */
    public PaymentRequest(String orderId, double amount, String cardToken) {
        this.orderId = orderId;
        this.amount = amount;
        this.cardToken = cardToken;
    }

    /**
     * Default constructor for JSON deserialization.
     */
    public PaymentRequest() {
        this.orderId = null;
        this.amount = 0.0;
        this.cardToken = null;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getAmount() {
        return amount;
    }

    public String getCardToken() {
        return cardToken;
    }

    @Override
    public String toString() {
        return String.format("PaymentRequest{orderId='%s', amount=%.2f, cardToken='%s'}",
                orderId, amount, cardToken != null ? cardToken.substring(0, Math.min(8, cardToken.length())) + "***" : "null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaymentRequest that = (PaymentRequest) o;

        if (Double.compare(that.amount, amount) != 0) return false;
        if (orderId != null ? !orderId.equals(that.orderId) : that.orderId != null) return false;
        return cardToken != null ? cardToken.equals(that.cardToken) : that.cardToken == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = orderId != null ? orderId.hashCode() : 0;
        temp = Double.doubleToLongBits(amount);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (cardToken != null ? cardToken.hashCode() : 0);
        return result;
    }
}