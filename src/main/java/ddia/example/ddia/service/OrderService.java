package ddia.example.ddia.service;

import ddia.example.ddia.model.OrderRequest;
import ddia.example.ddia.model.OrderResponse;
import ddia.example.ddia.model.PaymentRequest;
import ddia.example.ddia.model.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final PaymentService paymentService;
    private final Random random = new Random();

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Processes an order by coordinating multiple async operations.
     *
     * This method orchestrates:
     * 1. Inventory checking (using inventory thread pool)
     * 2. Payment processing (using payment thread pool)
     * 3. Notification sending (using notification thread pool)
     *
     * Operations run in parallel where possible to minimize total processing time.
     */
    public CompletableFuture<OrderResponse>processOrder(OrderRequest request){
        long startTime = System.currentTimeMillis();
        CompletableFuture<Boolean>inventoryCheck = checkInventory(request);
        CompletableFuture<PaymentResponse>paymentProcessing = processPaymentAsync(request);

        // Combine results when both operations complete
        return CompletableFuture.allOf(inventoryCheck,paymentProcessing)
                .thenApply(v->{
                    try {
                        boolean isInventoryAvailable = inventoryCheck.get();
                        PaymentResponse paymentResponse = paymentProcessing.get();
                        long processingTime = System.currentTimeMillis() - startTime;
                        return processOrderResult(request,isInventoryAvailable,paymentResponse, processingTime);
                    }catch (Exception e){
                        OrderResponse failedOrderResponse = new OrderResponse(
                            request.getOrderId(),
                            OrderResponse.STATUS_ERROR,
                            null,
                            "Order processing failed: " + e.getMessage(),
                             System.currentTimeMillis(),
                             request.getAmount(),
                             null
                        );
                        return failedOrderResponse;
                    }
                })
                .exceptionally(throwable -> {
                    return new OrderResponse(
                            request.getOrderId(),
                            OrderResponse.STATUS_ERROR,
                            null,
                            "Unexpected error: " + throwable.getMessage()
                    );
                });
    }

    /**
     * Checks inventory availability using the inventory thread pool.
     *
     * This operation is isolated in its own thread pool to prevent
     * slow inventory services from affecting payment processing.
     */
    @Async("inventoryExecutor")
    public CompletableFuture<Boolean>checkInventory(OrderRequest request){
        try{
            int delay = 2000 + random.nextInt(3000);
            Thread.sleep(delay);
            // Simulate 70% availability rate
            boolean available  = random.nextDouble()<0.7;
            if(available){
                logger.info("✅ Inventory available for order: {}", request.getOrderId());
            }else {
                logger.warn("⚠️ Inventory unavailable for order: {}", request.getOrderId());
            }
            return CompletableFuture.completedFuture(available);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Processes payment using the payment thread pool.
     *
     * This operation runs in its own thread pool to provide isolation
     * from other operations and better resource management.
     */
    public CompletableFuture<PaymentResponse>processPaymentAsync(OrderRequest request){
        PaymentRequest paymentRequest = new PaymentRequest(request.getOrderId(), request.getAmount(), request.getCardToken());
        try{
            PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);
            return CompletableFuture.completedFuture(paymentResponse);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            PaymentResponse failedPaymentResponse = new PaymentResponse(
                    "FAILED-" + UUID.randomUUID().toString(),
                    request.getAmount(),
                    PaymentResponse.STATUS_FAILED,
                    System.currentTimeMillis(),
                    e.getMessage()
            );
           return CompletableFuture.completedFuture(failedPaymentResponse);
        }
    }

    /**
     * Sends order confirmation notification using the notification thread pool.
     *
     * This is a fire-and-forget operation that runs in its own thread pool.
     * If notifications fail, it won't affect the main order processing.
     */
    @Async("notificationExecutor")
    public CompletableFuture<Void>sendNotification(OrderRequest request, String status){
        try {
            Thread.sleep(500 + random.nextInt(1000));
            if(random.nextDouble()<0.95){
                logger.info("✅ Notification sent successfully for order: {}", request.getOrderId());
            }else {
                logger.warn("⚠️ Failed to send notification for order: {}", request.getOrderId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (Exception ex){
            logger.error("❌ Failed to send notification for order {}: {}", request.getOrderId(), ex.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Processes the combined results of inventory and payment operations.
     */
    private OrderResponse processOrderResult(
            OrderRequest request,
            boolean isInventoryAvilable,
            PaymentResponse paymentResponse,
            long processingTime
    ){
       // String orderId, String status, String paymentId, String errorMessage
        if(isInventoryAvilable && paymentResponse.isSuccessful()){
            sendNotification(request, "CONFIRMED");
            return new OrderResponse(
                    request.getOrderId(),
                    OrderResponse.STATUS_CONFIRMED,
                    paymentResponse.getTransactionId(),
                    null,
                    System.currentTimeMillis(),
                    request.getAmount(),
                    generateTrackingNumber()
            );
        }else if(!isInventoryAvilable && paymentResponse.isSuccessful()){
            sendNotification(request, "FAILED_INVENTORY");
            return new OrderResponse(
                    request.getOrderId(),
                    OrderResponse.STATUS_FAILED,
                    paymentResponse.getTransactionId(),
                    "Product out of stock. Payment will be refunded.",
                    System.currentTimeMillis(),
                    request.getAmount(),
                    null
            );
        } else if (isInventoryAvilable && !paymentResponse.isSuccessful()) {
          sendNotification(request, "FAILED_PAYMENT");
            return new OrderResponse(
                    request.getOrderId(),
                    OrderResponse.STATUS_FAILED,
                    paymentResponse.getTransactionId(),
                    "Payment processing failed: " + paymentResponse.getErrorMessage(),
                    System.currentTimeMillis(),
                    request.getAmount(),
                    null
            );
        }else {
            sendNotification(request, "FAILED");

            return new OrderResponse(
                    request.getOrderId(),
                    OrderResponse.STATUS_FAILED,
                    paymentResponse.getTransactionId(),
                    "Order failed due to inventory and payment issues",
                    System.currentTimeMillis(),
                    request.getAmount(),
                    null
            );
        }
    };

    /**
     * Generates a mock tracking number for successful orders.
     */
    private String generateTrackingNumber() {
        return "TRK-" + System.currentTimeMillis() + "-" + random.nextInt(1000);
    }

    /**
     * Health check method for the order service.
     */
    public boolean isHealthy() {
        return paymentService.isHealthy();
    }
}