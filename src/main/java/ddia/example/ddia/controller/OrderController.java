package ddia.example.ddia.controller;

import ddia.example.ddia.model.OrderRequest;
import ddia.example.ddia.model.OrderResponse;
import ddia.example.ddia.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * REST controller for order operations.
 *
 * This controller demonstrates:
 * - Asynchronous request processing
 * - Bulkhead pattern in action
 * - Error handling and status mapping
 * - Health checks
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*") // For testing purposes
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Processes an order request asynchronously.
     *
     * This endpoint demonstrates the bulkhead pattern where different operations
     * (inventory, payment, notifications) run in isolated thread pools.
     *
     * @param request The order request with customer and product details
     * @return CompletableFuture with order response
     */
    @PostMapping("/process")
    public CompletableFuture<ResponseEntity<OrderResponse>> processOrder(@Valid @RequestBody OrderRequest request) {
        logger.info("📦 Received order request: {}", request.getOrderId());

        return orderService.processOrder(request)
                .thenApply(response -> {
                    // Map order status to appropriate HTTP status
                    HttpStatus httpStatus = mapOrderStatusToHttpStatus(response);

                    logger.info("📋 Order {} completed with status: {} (HTTP: {})",
                            response.getOrderId(), response.getStatus(), httpStatus.value());

                    return ResponseEntity.status(httpStatus).body(response);
                })
                .exceptionally(throwable -> {
                    logger.error("💥 Order processing exception for {}: {}",
                            request.getOrderId(), throwable.getMessage());

                    OrderResponse errorResponse = new OrderResponse(
                            request.getOrderId(),
                            OrderResponse.STATUS_ERROR,
                            null,
                            "Order processing failed: " + throwable.getMessage(),
                            System.currentTimeMillis(),
                            request.getAmount(),
                            null
                    );

                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * Synchronous order processing endpoint for comparison.
     *
     * This endpoint shows the difference between async and sync processing.
     * Note: This still uses async operations internally but blocks on the result.
     */
    @PostMapping("/process-sync")
    public ResponseEntity<OrderResponse> processOrderSync(@Valid @RequestBody OrderRequest request) {
        logger.info("📦 Received synchronous order request: {}", request.getOrderId());

        try {
            // Block and wait for the async operation to complete
            OrderResponse response = orderService.processOrder(request).get();

            HttpStatus httpStatus = mapOrderStatusToHttpStatus(response);

            logger.info("📋 Synchronous order {} completed with status: {}",
                    response.getOrderId(), response.getStatus());

            return ResponseEntity.status(httpStatus).body(response);

        } catch (Exception e) {
            logger.error("💥 Synchronous order processing failed for {}: {}",
                    request.getOrderId(), e.getMessage());

            OrderResponse errorResponse = new OrderResponse(
                    request.getOrderId(),
                    OrderResponse.STATUS_ERROR,
                    null,
                    "Synchronous order processing failed: " + e.getMessage()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint for order service.
     */
    @GetMapping("/health")
    public ResponseEntity<Object> healthCheck() {
        try {
            boolean healthy = orderService.isHealthy();

            if (healthy) {
                return ResponseEntity.ok().body(new HealthResponse("UP", "Order service is healthy"));
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new HealthResponse("DOWN", "Order service is experiencing issues"));
            }

        } catch (Exception e) {
            logger.error("Order service health check failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HealthResponse("DOWN", "Health check failed: " + e.getMessage()));
        }
    }

    /**
     * Simple status endpoint.
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("Order service is running");
    }

    /**
     * Endpoint to get service statistics (for monitoring).
     */
    @GetMapping("/stats")
    public ResponseEntity<Object> getStats() {
        // In a real application, you would collect actual metrics
        ServiceStats stats = new ServiceStats(
                System.currentTimeMillis(),
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().totalMemory(),
                Runtime.getRuntime().freeMemory(),
                Thread.activeCount()
        );

        return ResponseEntity.ok(stats);
    }

    /**
     * Maps order status to appropriate HTTP status code.
     */
    private HttpStatus mapOrderStatusToHttpStatus(OrderResponse response) {
        return switch (response.getStatus()) {
            case OrderResponse.STATUS_CONFIRMED -> HttpStatus.OK;
            case OrderResponse.STATUS_PENDING -> HttpStatus.ACCEPTED;
            case OrderResponse.STATUS_FAILED -> HttpStatus.BAD_REQUEST;
            case OrderResponse.STATUS_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            case OrderResponse.STATUS_CANCELLED -> HttpStatus.GONE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * Health response class.
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

    /**
     * Service statistics class.
     */
    public static class ServiceStats {
        private final long timestamp;
        private final int availableProcessors;
        private final long totalMemory;
        private final long freeMemory;
        private final int activeThreads;

        public ServiceStats(long timestamp, int availableProcessors, long totalMemory,
                            long freeMemory, int activeThreads) {
            this.timestamp = timestamp;
            this.availableProcessors = availableProcessors;
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.activeThreads = activeThreads;
        }

        public long getTimestamp() { return timestamp; }
        public int getAvailableProcessors() { return availableProcessors; }
        public long getTotalMemory() { return totalMemory; }
        public long getFreeMemory() { return freeMemory; }
        public long getUsedMemory() { return totalMemory - freeMemory; }
        public int getActiveThreads() { return activeThreads; }
    }
}