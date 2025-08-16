package ddia.example.ddia.service;

import ddia.example.ddia.exception.PaymentException;
import ddia.example.ddia.model.PaymentRequest;
import ddia.example.ddia.model.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class PaymentService {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(PaymentService.class);
    private final Random random = new Random();

    // Simulated failure rate (30% by default)
    private static final double FAILURE_RATE = 0.3;


    /**
     * Processes a payment with circuit breaker and retry protection.
     *
     * The @CircuitBreaker annotation:
     * - Opens the circuit after 50% failure rate in a 10-call sliding window
     * - Stays open for 10 seconds before attempting half-open state
     * - Falls back to fallbackPayment method when circuit is open
     *
     * The @Retry annotation:
     * - Retries up to 3 times with 2-second initial delay
     * - Uses exponential backoff with multiplier of 2
     */
    @CircuitBreaker(name = "payment-service", fallbackMethod = "fallbackPayment")
    @Retry(name = "payment-service")
    public PaymentResponse processPayment(PaymentRequest request) throws InterruptedException {
        // Simulate network delay (100-300ms)
        simulateNetworkDelay();

        // Simulate random failures
        if(shouldSimulateFailure()){
            String errorMessage = "Payment gateway timeout for order: " + request.getOrderId();
            throw new PaymentException(errorMessage, "GATEWAY_TIMEOUT", request.getOrderId());
        }

        // Simulate successful payment processing
        return createSuccessfulPaymentResponse(request);
    }

    /**
     * Fallback method called when circuit breaker is open or retries are exhausted.
     *
     * This method must have the same signature as the original method plus an Exception parameter.
     * It provides graceful degradation by queueing orders for manual processing.
     */
    public PaymentResponse fallbackPayment(PaymentRequest request, PaymentException ex){
        // Create a fallback response indicating manual review is needed
        PaymentResponse fallbackResponse = new PaymentResponse(
                "FALLBACK-" + UUID.randomUUID().toString(),
                request.getAmount(),
                PaymentResponse.STATUS_PENDING_MANUAL_REVIEW,
                System.currentTimeMillis(),
                "Payment service unavailable. Order queued for manual processing.",
                "FALLBACK_GATEWAY"
        );
        return fallbackResponse;
    }

    /**
     * Alternative payment method that bypasses circuit breaker for emergency processing.
     * This could be used for high-priority orders or VIP customers.
     */
    public PaymentResponse processEmergencyPayment(PaymentRequest request){
        try{
            // Simulate faster processing for emergency payments
            Thread.sleep(50+ random.nextInt(100));

            PaymentResponse response = createSuccessfulPaymentResponse(request);
            response = new PaymentResponse(
                    "EMERGENCY-" + response.getTransactionId(),
                    response.getAmount(),
                    response.getStatus(),
                    response.getTimestamp(),
                    null,
                    "EMERGENCY_GATEWAY"
            );
            return response;
        }catch (InterruptedException e ){
            Thread.currentThread().interrupt();
            throw new PaymentException("Emergency payment interrupted", "EMERGENCY_INTERRUPTED", request.getOrderId());
        }
    }

    /**
     * Checks if the payment service is healthy.
     * This could be used by health checks and monitoring systems.
     */
    public boolean isHealthy() {
        try {
            // Simple health check - try to process a test transaction
            Thread.sleep(10); // Minimal delay
            return random.nextDouble() > 0.1; // 90% healthy
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Simulates network delay typical of payment gateway calls.
     */
    private void simulateNetworkDelay() {
        int delay = 100 + random.nextInt(200);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentException("Payment processing interrupted");
        }
    }

    /**
     * Determines if we should simulate a failure based on the configured failure rate.
     */
    private boolean shouldSimulateFailure() {
        return random.nextDouble() < FAILURE_RATE;
    }

    /**
     * Creates a successful payment response.
     */
    public PaymentResponse createSuccessfulPaymentResponse(PaymentRequest request){
        return new PaymentResponse(UUID.randomUUID().toString(), request.getAmount(), PaymentResponse.STATUS_COMPLETED, System.currentTimeMillis(),null, "MOCK_GATEWAY");
    }

}
