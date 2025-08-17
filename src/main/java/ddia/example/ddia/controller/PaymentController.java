package ddia.example.ddia.controller;

import ddia.example.ddia.model.PaymentRequest;
import ddia.example.ddia.model.PaymentResponse;
import ddia.example.ddia.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for payment operations.
 *
 * This controller provides endpoints for:
 * - Processing payments
 * - Emergency payment processing
 * - Health checks
 */
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*") // For testing purposes
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Processes a payment request.
     *
     * This endpoint demonstrates the circuit breaker pattern in action.
     * When the payment service is experiencing failures, the circuit breaker
     * will open and return fallback responses.
     *
     * @param request The payment request with order details
     * @return Payment response with transaction details or fallback information
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        logger.info("🏦 Received payment request for order: {}", request.getOrderId());

        try {
            PaymentResponse response = paymentService.processPayment(request);

            // Return different HTTP status based on payment result
            if (response.isSuccessful()) {
                logger.info("✅ Payment processed successfully: {}", response.getTransactionId());
                return ResponseEntity.ok(response);
            } else if (response.isPending()) {
                logger.info("🔄 Payment pending: {}", response.getTransactionId());
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            } else {
                logger.warn("❌ Payment failed: {}", response.getErrorMessage());
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
            }

        } catch (Exception e) {
            logger.error("💥 Unexpected error processing payment for order {}: {}",
                    request.getOrderId(), e.getMessage());

            // Create error response
            PaymentResponse errorResponse = new PaymentResponse(
                    "ERROR-" + System.currentTimeMillis(),
                    request.getAmount(),
                    PaymentResponse.STATUS_FAILED,
                    System.currentTimeMillis(),
                    "Internal server error: " + e.getMessage()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Processes an emergency payment request.
     *
     * This endpoint bypasses the circuit breaker for high-priority payments
     * such as VIP customers or critical orders that must be processed even
     * when the main payment service is experiencing issues.
     *
     * @param request The payment request with order details
     * @return Payment response with transaction details
     */
    @PostMapping("/emergency")
    public ResponseEntity<PaymentResponse> processEmergencyPayment(@Valid @RequestBody PaymentRequest request) {
        logger.info("🚨 Received EMERGENCY payment request for order: {}", request.getOrderId());

        try {
            PaymentResponse response = paymentService.processEmergencyPayment(request);

            if (response.isSuccessful()) {
                logger.info("⚡ Emergency payment processed successfully: {}", response.getTransactionId());
                return ResponseEntity.ok(response);
            } else {
                logger.error("❌ Emergency payment failed: {}", response.getErrorMessage());
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
            }

        } catch (Exception e) {
            logger.error("💥 Emergency payment error for order {}: {}",
                    request.getOrderId(), e.getMessage());

            PaymentResponse errorResponse = new PaymentResponse(
                    "EMERGENCY-ERROR-" + System.currentTimeMillis(),
                    request.getAmount(),
                    PaymentResponse.STATUS_FAILED,
                    System.currentTimeMillis(),
                    "Emergency payment failed: " + e.getMessage()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint for payment service.
     *
     * @return Health status of the payment service
     */
    @GetMapping("/health")
    public ResponseEntity<Object> healthCheck() {
        try {
            boolean healthy = paymentService.isHealthy();

            if (healthy) {
                return ResponseEntity.ok().body(new HealthResponse("UP", "Payment service is healthy"));
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new HealthResponse("DOWN", "Payment service is experiencing issues"));
            }

        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HealthResponse("DOWN", "Health check failed: " + e.getMessage()));
        }
    }

    /**
     * Simple status endpoint.
     *
     * @return Basic status information
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("Payment service is running");
    }

    /**
     * Inner class for health check responses.
     */
    public static class HealthResponse {
        private final String status;
        private final String message;
        private final long timestamp;

        public HealthResponse(String status, String message) {
            this.status = status;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
}