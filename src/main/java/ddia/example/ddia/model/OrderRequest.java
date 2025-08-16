package ddia.example.ddia.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Order request model representing an order processing request.
 *
 * This class contains all the information needed to process an order,
 * including payment details and customer information.
 */
public class OrderRequest {

    @NotBlank(message = "Order ID cannot be blank")
    private final String orderId;

    @Positive(message = "Amount must be positive")
    private final double amount;

    @NotBlank(message = "Card token cannot be blank")
    private final String cardToken;

    @NotBlank(message = "Customer ID cannot be blank")
    private final String customerId;

    private final String productId;
    private final int quantity;
    private final String shippingAddress;

    /**
     * Creates a new order request with all details.
     */
    public OrderRequest(String orderId, double amount, String cardToken, String customerId,
                        String productId, int quantity, String shippingAddress) {
        this.orderId = orderId;
        this.amount = amount;
        this.cardToken = cardToken;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.shippingAddress = shippingAddress;
    }

    /**
     * Creates a basic order request (for testing).
     */
    public OrderRequest(String orderId, double amount, String cardToken, String customerId) {
        this(orderId, amount, cardToken, customerId, null, 1, null);
    }

    /**
     * Default constructor for JSON deserialization.
     */
    public OrderRequest() {
        this.orderId = null;
        this.amount = 0.0;
        this.cardToken = null;
        this.customerId = null;
        this.productId = null;
        this.quantity = 0;
        this.shippingAddress = null;
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

    public String getCustomerId() {
        return customerId;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    @Override
    public String toString() {
        return String.format("OrderRequest{orderId='%s', amount=%.2f, customerId='%s', productId='%s', quantity=%d}",
                orderId, amount, customerId, productId, quantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrderRequest that = (OrderRequest) o;

        if (Double.compare(that.amount, amount) != 0) return false;
        if (quantity != that.quantity) return false;
        if (orderId != null ? !orderId.equals(that.orderId) : that.orderId != null) return false;
        if (cardToken != null ? !cardToken.equals(that.cardToken) : that.cardToken != null) return false;
        if (customerId != null ? !customerId.equals(that.customerId) : that.customerId != null) return false;
        if (productId != null ? !productId.equals(that.productId) : that.productId != null) return false;
        return shippingAddress != null ? shippingAddress.equals(that.shippingAddress) : that.shippingAddress == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = orderId != null ? orderId.hashCode() : 0;
        temp = Double.doubleToLongBits(amount);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (cardToken != null ? cardToken.hashCode() : 0);
        result = 31 * result + (customerId != null ? customerId.hashCode() : 0);
        result = 31 * result + (productId != null ? productId.hashCode() : 0);
        result = 31 * result + quantity;
        result = 31 * result + (shippingAddress != null ? shippingAddress.hashCode() : 0);
        return result;
    }
}