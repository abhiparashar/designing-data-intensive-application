package ddia.example.ddia.service;

import ddia.example.ddia.model.OrderRequest;
import ddia.example.ddia.model.OrderResponse;
import ddia.example.ddia.model.PaymentRequest;
import ddia.example.ddia.model.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<OrderResponse>processOrder(PaymentRequest request){
        long startTime = System.currentTimeMillis();
        CompletableFuture<Boolean>inventoryCheck = checkInventory(request);
        CompletableFuture<PaymentResponse>paymentProcessing = processPaymentAsync(request);

    }

    /**
     * Checks inventory availability using the inventory thread pool.
     *
     * This operation is isolated in its own thread pool to prevent
     * slow inventory services from affecting payment processing.
     */
    @Async("inventoryExecutor")
    public CompletableFuture<Boolean>checkInventory(PaymentRequest request){
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
            CompletableFuture.completedFuture(failedPaymentResponse);
        }
    }
}