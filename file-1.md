# File 1: Reliability Engineering Workshop (Complete)
*Build Fault-Tolerant Systems with Spring Boot & Java*

## 🎯 What You'll Build

**Main Project**: Resilient order processing microservice that gracefully handles:
- Database failures
- External service outages
- Network timeouts
- High load scenarios

**Time Investment**: 2-3 weeks (2-3 hours/week)

---

## 🏗️ Project Setup (15 minutes)

### **Maven Dependencies:**
```xml
<!-- pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>reliability-workshop</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <properties>
        <java.version>17</java.version>
        <resilience4j.version>2.1.0</resilience4j.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>${resilience4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### **Application Configuration:**
```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: reliability-workshop

resilience4j:
  circuitbreaker:
    instances:
      payment-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        minimum-number-of-calls: 5
      inventory-service:
        sliding-window-size: 5
        failure-rate-threshold: 60
        wait-duration-in-open-state: 5s

  retry:
    instances:
      payment-service:
        max-attempts: 3
        wait-duration: 2s
        exponential-backoff-multiplier: 2

  bulkhead:
    instances:
      payment-service:
        max-concurrent-calls: 10
      inventory-service:
        max-concurrent-calls: 5

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,circuitbreakers,retries
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.example: DEBUG
    io.github.resilience4j: DEBUG
```

---

## 💳 Implementation 1: Circuit Breaker Pattern (45 minutes)

### **Payment Service with Circuit Breaker:**
```java
// PaymentService.java
package com.example.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final Random random = new Random();
    
    @CircuitBreaker(name = "payment-service", fallbackMethod = "fallbackPayment")
    @Retry(name = "payment-service")
    public PaymentResponse processPayment(PaymentRequest request) {
        logger.info("Processing payment for order: {}", request.getOrderId());
        
        // Simulate network delay
        try {
            Thread.sleep(100 + random.nextInt(200)); // 100-300ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentException("Payment interrupted");
        }
        
        // Simulate random failures (30% failure rate)
        if (random.nextDouble() < 0.3) {
            logger.error("Payment gateway timeout for order: {}", request.getOrderId());
            throw new PaymentException("Payment gateway timeout");
        }
        
        PaymentResponse response = new PaymentResponse(
            UUID.randomUUID().toString(),
            request.getAmount(),
            "COMPLETED",
            System.currentTimeMillis()
        );
        
        logger.info("Payment successful: {}", response.getTransactionId());
        return response;
    }
    
    // Fallback method - must have same signature + Exception parameter
    public PaymentResponse fallbackPayment(PaymentRequest request, Exception ex) {
        logger.warn("Payment fallback triggered for order: {} - {}", 
                   request.getOrderId(), ex.getMessage());
        
        return new PaymentResponse(
            "FALLBACK-" + UUID.randomUUID().toString(),
            request.getAmount(),
            "PENDING_MANUAL_REVIEW",
            System.currentTimeMillis(),
            "Payment service unavailable. Order queued for manual processing."
        );
    }
}

// PaymentRequest.java
package com.example.model;

public class PaymentRequest {
    private final String orderId;
    private final double amount;
    private final String cardToken;
    
    public PaymentRequest(String orderId, double amount, String cardToken) {
        this.orderId = orderId;
        this.amount = amount;
        this.cardToken = cardToken;
    }
    
    // Getters
    public String getOrderId() { return orderId; }
    public double getAmount() { return amount; }
    public String getCardToken() { return cardToken; }
}

// PaymentResponse.java
package com.example.model;

public class PaymentResponse {
    private final String transactionId;
    private final double amount;
    private final String status;
    private final long timestamp;
    private final String errorMessage;
    
    public PaymentResponse(String transactionId, double amount, String status, long timestamp) {
        this(transactionId, amount, status, timestamp, null);
    }
    
    public PaymentResponse(String transactionId, double amount, String status, 
                          long timestamp, String errorMessage) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = status;
        this.timestamp = timestamp;
        this.errorMessage = errorMessage;
    }
    
    // Getters
    public String getTransactionId() { return transactionId; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
    public String getErrorMessage() { return errorMessage; }
}

// PaymentException.java
package com.example.exception;

public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
```

### **Test the Circuit Breaker:**
```java
// PaymentController.java
package com.example.controller;

import com.example.model.PaymentRequest;
import com.example.model.PaymentResponse;
import com.example.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }
}

// CircuitBreakerTest.java (in src/test/java)
package com.example.service;

import com.example.model.PaymentRequest;
import com.example.model.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CircuitBreakerTest {
    
    @Autowired
    private PaymentService paymentService;
    
    @Test
    void testCircuitBreakerBehavior() throws InterruptedException {
        System.out.println("🧪 Testing Circuit Breaker Behavior\n");
        
        int successCount = 0, fallbackCount = 0, errorCount = 0;
        
        for (int i = 0; i < 20; i++) {
            try {
                PaymentRequest request = new PaymentRequest("order_" + i, 100.0, "card_123");
                PaymentResponse response = paymentService.processPayment(request);
                
                if ("COMPLETED".equals(response.getStatus())) {
                    successCount++;
                    System.out.printf("✅ Payment %d: %s%n", i+1, response.getTransactionId());
                } else if ("PENDING_MANUAL_REVIEW".equals(response.getStatus())) {
                    fallbackCount++;
                    System.out.printf("🔄 Payment %d: Fallback - %s%n", i+1, response.getErrorMessage());
                }
                
            } catch (Exception e) {
                errorCount++;
                System.out.printf("❌ Payment %d: %s%n", i+1, e.getMessage());
            }
            
            Thread.sleep(500); // Half second between requests
        }
        
        System.out.printf("%n📊 Results: Success=%d, Fallback=%d, Error=%d%n", 
                         successCount, fallbackCount, errorCount);
    }
}
```

---

## 🔧 Implementation 2: Bulkhead Pattern (60 minutes)

### **Thread Pool Configuration:**
```java
// BulkheadConfiguration.java
package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class BulkheadConfiguration {
    
    @Bean(name = "paymentExecutor")
    public Executor paymentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("Payment-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "inventoryExecutor")
    public Executor inventoryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(15);
        executor.setThreadNamePrefix("Inventory-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("Notification-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
```

### **Order Service with Bulkhead:**
```java
// OrderService.java
package com.example.service;

import com.example.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final PaymentService paymentService;
    private final Random random = new Random();
    
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    public CompletableFuture<OrderResponse> processOrder(OrderRequest request) {
        logger.info("Processing order {} on thread: {}", 
                   request.getOrderId(), Thread.currentThread().getName());
        
        // Start parallel operations using different thread pools
        CompletableFuture<Boolean> inventoryCheck = checkInventory(request);
        CompletableFuture<PaymentResponse> paymentProcessing = processPayment(request);
        
        return CompletableFuture.allOf(inventoryCheck, paymentProcessing)
                .thenApply(v -> {
                    try {
                        boolean inventoryAvailable = inventoryCheck.get();
                        PaymentResponse payment = paymentProcessing.get();
                        
                        if (inventoryAvailable && "COMPLETED".equals(payment.getStatus())) {
                            sendNotification(request); // Fire and forget
                            
                            return new OrderResponse(
                                request.getOrderId(),
                                "CONFIRMED",
                                payment.getTransactionId(),
                                null
                            );
                        } else {
                            return new OrderResponse(
                                request.getOrderId(),
                                "FAILED",
                                payment.getTransactionId(),
                                "Inventory unavailable or payment failed"
                            );
                        }
                    } catch (Exception e) {
                        logger.error("Order processing failed", e);
                        return new OrderResponse(
                            request.getOrderId(),
                            "ERROR",
                            null,
                            e.getMessage()
                        );
                    }
                });
    }
    
    @Async("inventoryExecutor")
    public CompletableFuture<Boolean> checkInventory(OrderRequest request) {
        logger.info("Checking inventory for {} on thread: {}", 
                   request.getOrderId(), Thread.currentThread().getName());
        
        try {
            // Simulate slow inventory service (2-5 seconds)
            Thread.sleep(2000 + random.nextInt(3000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture(false);
        }
        
        boolean available = random.nextBoolean(); // 50% availability
        logger.info("Inventory check for {}: {}", request.getOrderId(), 
                   available ? "AVAILABLE" : "OUT_OF_STOCK");
        
        return CompletableFuture.completedFuture(available);
    }
    
    @Async("paymentExecutor")
    public CompletableFuture<PaymentResponse> processPayment(OrderRequest request) {
        logger.info("Processing payment for {} on thread: {}", 
                   request.getOrderId(), Thread.currentThread().getName());
        
        PaymentRequest paymentRequest = new PaymentRequest(
            request.getOrderId(),
            request.getAmount(),
            request.getCardToken()
        );
        
        return CompletableFuture.completedFuture(
            paymentService.processPayment(paymentRequest)
        );
    }
    
    @Async("notificationExecutor")
    public void sendNotification(OrderRequest request) {
        logger.info("Sending notification for {} on thread: {}", 
                   request.getOrderId(), Thread.currentThread().getName());
        
        try {
            Thread.sleep(1000); // Simulate email sending
            logger.info("✅ Notification sent for order {}", request.getOrderId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("❌ Failed to send notification for order {}", request.getOrderId());
        }
    }
}

// OrderRequest.java
package com.example.model;

public class OrderRequest {
    private final String orderId;
    private final double amount;
    private final String cardToken;
    private final String customerId;
    
    public OrderRequest(String orderId, double amount, String cardToken, String customerId) {
        this.orderId = orderId;
        this.amount = amount;
        this.cardToken = cardToken;
        this.customerId = customerId;
    }
    
    // Getters
    public String getOrderId() { return orderId; }
    public double getAmount() { return amount; }
    public String getCardToken() { return cardToken; }
    public String getCustomerId() { return customerId; }
}

// OrderResponse.java
package com.example.model;

public class OrderResponse {
    private final String orderId;
    private final String status;
    private final String paymentId;
    private final String errorMessage;
    
    public OrderResponse(String orderId, String status, String paymentId, String errorMessage) {
        this.orderId = orderId;
        this.status = status;
        this.paymentId = paymentId;
        this.errorMessage = errorMessage;
    }
    
    // Getters
    public String getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public String getPaymentId() { return paymentId; }
    public String getErrorMessage() { return errorMessage; }
}
```

---

## 🐒 Implementation 3: Chaos Engineering (90 minutes)

### **Simple Chaos Monkey:**
```java
// ChaosMonkey.java
package com.example.chaos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ChaosMonkey {
    
    private static final Logger logger = LoggerFactory.getLogger(ChaosMonkey.class);
    private final Random random = new Random();
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    @EventListener(ApplicationReadyEvent.class)
    public void startChaosMonkey() {
        logger.info("🐒 Chaos Monkey starting up...");
        running.set(true);
        
        CompletableFuture.runAsync(this::runChaosExperiments);
    }
    
    private void runChaosExperiments() {
        while (running.get()) {
            try {
                // Wait 30-90 seconds between experiments
                Thread.sleep(30000 + random.nextInt(60000));
                
                if (!running.get()) break;
                
                // Randomly choose an experiment
                ChaosExperiment experiment = getRandomExperiment();
                logger.info("🔥 Executing chaos experiment: {}", experiment.getName());
                
                experiment.execute();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Chaos experiment failed", e);
            }
        }
        
        logger.info("🐒 Chaos Monkey stopped");
    }
    
    private ChaosExperiment getRandomExperiment() {
        ChaosExperiment[] experiments = {
            new LatencyInjectionExperiment(),
            new ExceptionInjectionExperiment(),
            new ResourceExhaustionExperiment()
        };
        
        return experiments[random.nextInt(experiments.length)];
    }
    
    public void stop() {
        running.set(false);
    }
}

// ChaosExperiment.java
package com.example.chaos;

public interface ChaosExperiment {
    String getName();
    void execute();
}

// LatencyInjectionExperiment.java
package com.example.chaos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class LatencyInjectionExperiment implements ChaosExperiment {
    
    private static final Logger logger = LoggerFactory.getLogger(LatencyInjectionExperiment.class);
    private final Random random = new Random();
    
    @Override
    public String getName() {
        return "Latency Injection";
    }
    
    @Override
    public void execute() {
        int latencyMs = 1000 + random.nextInt(4000); // 1-5 seconds
        logger.warn("💤 Injecting {}ms latency into system", latencyMs);
        
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("✅ Latency injection completed");
    }
}

// ExceptionInjectionExperiment.java
package com.example.chaos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class ExceptionInjectionExperiment implements ChaosExperiment {
    
    private static final Logger logger = LoggerFactory.getLogger(ExceptionInjectionExperiment.class);
    private final Random random = new Random();
    
    @Override
    public String getName() {
        return "Exception Injection";
    }
    
    @Override
    public void execute() {
        String[] exceptionTypes = {
            "Database Connection Failed",
            "Network Timeout",
            "Out of Memory",
            "Service Unavailable"
        };
        
        String exception = exceptionTypes[random.nextInt(exceptionTypes.length)];
        logger.warn("💥 Simulating exception: {}", exception);
        
        // In real implementation, you'd inject actual exceptions
        // using AOP or bytecode manipulation
        
        logger.info("✅ Exception injection simulation completed");
    }
}

// ResourceExhaustionExperiment.java
package com.example.chaos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ResourceExhaustionExperiment implements ChaosExperiment {
    
    private static final Logger logger = LoggerFactory.getLogger(ResourceExhaustionExperiment.class);
    private final Random random = new Random();
    
    @Override
    public String getName() {
        return "Resource Exhaustion";
    }
    
    @Override
    public void execute() {
        logger.warn("🔥 Simulating resource exhaustion");
        
        // Allocate memory to simulate memory pressure
        List<byte[]> memoryHog = new ArrayList<>();
        
        try {
            for (int i = 0; i < 10; i++) {
                memoryHog.add(new byte[10 * 1024 * 1024]); // 10MB chunks
                Thread.sleep(100);
            }
            
            Thread.sleep(5000); // Hold memory for 5 seconds
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            memoryHog.clear(); // Release memory
            System.gc(); // Suggest garbage collection
        }
        
        logger.info("✅ Resource exhaustion simulation completed");
    }
}
```

---

## 🧪 Running and Testing (30 minutes)

### **Main Application:**
```java
// ReliabilityWorkshopApplication.java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReliabilityWorkshopApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReliabilityWorkshopApplication.class, args);
    }
}
```

### **Integration Test:**
```java
// OrderControllerTest.java
package com.example.controller;

import com.example.model.OrderRequest;
import com.example.model.OrderResponse;
import com.example.service.OrderService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
class OrderControllerTest {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderControllerTest.class);
    
    @Autowired
    private OrderService orderService;
    
    @Test
    void testBulkheadIsolation() throws InterruptedException {
        int numberOfOrders = 10;
        CountDownLatch latch = new CountDownLatch(numberOfOrders);
        
        AtomicInteger confirmed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // Submit orders concurrently
        for (int i = 0; i < numberOfOrders; i++) {
            OrderRequest request = new OrderRequest(
                "order_" + i,
                100.0 * (i + 1),
                "card_token_" + i,
                "customer_" + i
            );
            
            orderService.processOrder(request)
                .thenAccept(response -> {
                    switch (response.getStatus()) {
                        case "CONFIRMED" -> {
                            confirmed.incrementAndGet();
                            logger.info("✅ Order {}: CONFIRMED", response.getOrderId());
                        }
                        case "FAILED" -> {
                            failed.incrementAndGet();
                            logger.info("⚠️ Order {}: FAILED - {}", response.getOrderId(), response.getErrorMessage());
                        }
                        default -> {
                            errors.incrementAndGet();
                            logger.info("❌ Order {}: ERROR - {}", response.getOrderId(), response.getErrorMessage());
                        }
                    }
                    latch.countDown();
                })
                .exceptionally(throwable -> {
                    errors.incrementAndGet();
                    logger.error("Order processing exception: {}", throwable.getMessage());
                    latch.countDown();
                    return null;
                });
        }
        
        // Wait for all orders to complete (30 second timeout)
        latch.await(30, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        
        logger.info("\n📊 Bulkhead Test Results:");
        logger.info("   Total Orders: {}", numberOfOrders);
        logger.info("   Confirmed: {}", confirmed.get());
        logger.info("   Failed: {}", failed.get());
        logger.info("   Errors: {}", errors.get());
        logger.info("   Processing Time: {}ms", endTime - startTime);
    }
}
```

### **Order Controller:**
```java
// OrderController.java
package com.example.controller;

import com.example.model.OrderRequest;
import com.example.model.OrderResponse;
import com.example.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @PostMapping("/process")
    public CompletableFuture<ResponseEntity<OrderResponse>> processOrder(@RequestBody OrderRequest request) {
        return orderService.processOrder(request)
                .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order service is healthy");
    }
}
```

---

## 🚀 Running the Application

### **1. Start the Application:**
```bash
# Build and run
mvn clean compile
mvn spring-boot:run

# Application starts on http://localhost:8080
```

### **2. Test Circuit Breaker:**
```bash
# Test payment processing
curl -X POST http://localhost:8080/api/payments/process \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "test_order_1",
    "amount": 99.99,
    "cardToken": "card_12345"
  }'

# Check circuit breaker status
curl http://localhost:8080/actuator/circuitbreakers
```

### **3. Test Bulkhead with Load:**
```bash
# Submit multiple orders quickly
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/orders/process \
    -H "Content-Type: application/json" \
    -d "{
      \"orderId\": \"load_test_$i\",
      \"amount\": $(($i * 10)).00,
      \"cardToken\": \"card_$i\",
      \"customerId\": \"customer_$i\"
    }" &
done

# Wait for all requests to complete
wait
```

### **4. Monitor Metrics:**
```bash
# View health status
curl http://localhost:8080/actuator/health

# View metrics
curl http://localhost:8080/actuator/metrics

# View retry statistics
curl http://localhost:8080/actuator/retries
```

---

## 📊 Learning Outcomes & Key Insights

### **What You've Built:**
✅ **Circuit Breaker** - Automatically opens when payment service fails  
✅ **Bulkhead Pattern** - Separate thread pools prevent resource starvation  
✅ **Retry Logic** - Automatic retries with exponential backoff  
✅ **Chaos Engineering** - Proactive failure injection  
✅ **Monitoring** - Observability with Spring Actuator

### **Key Reliability Insights:**

**1. Circuit Breakers Prevent Cascade Failures**
```
Normal Flow: Request → Service → Response
Failure Flow: Request → Circuit Breaker (OPEN) → Fallback Response
```

**2. Bulkheads Provide Resource Isolation**
```
Without Bulkhead: Slow service affects entire application
With Bulkhead: Slow service only affects its thread pool
```

**3. Retry Strategies Must Be Carefully Configured**
```
Too Aggressive: Can overwhelm failing service
Too Conservative: Poor user experience
Sweet Spot: Exponential backoff with jitter
```

**4. Fallbacks Should Be Meaningful**
```
Bad Fallback: Generic error message
Good Fallback: Degraded but useful functionality
```

### **Production Patterns You've Learned:**

**Circuit Breaker States:**
- **CLOSED**: Normal operation, counting failures
- **OPEN**: Rejecting requests, returning fallback
- **HALF_OPEN**: Testing if service recovered

**Thread Pool Sizing:**
- **CPU-bound tasks**: # cores + 1
- **I/O-bound tasks**: Much higher (depends on latency)
- **Monitor queue sizes** and thread utilization

**Chaos Engineering Principles:**
- **Start small**: Begin with non-critical systems
- **Gradual increase**: Slowly expand blast radius
- **Learn and improve**: Use failures to strengthen system

---

## 🎯 Real-World Applications

### **Companies Using These Patterns:**

**Netflix:**
- Hystrix (circuit breaker library)
- Chaos Monkey in production
- Bulkhead pattern in microservices

**Amazon:**
- Cell-based architecture (bulkheads)
- Exponential backoff in AWS SDKs
- Chaos engineering across services

**Google:**
- Circuit breakers in gRPC
- SRE practices with error budgets
- Graceful degradation strategies

### **When to Use Each Pattern:**

**Circuit Breaker:**
- External service dependencies
- Database connections
- API rate limits

**Bulkhead:**
- Multiple service dependencies
- Different SLA requirements
- Resource contention issues

**Chaos Engineering:**
- Complex distributed systems
- Critical production services
- Compliance requirements

---

## 🚀 Next Steps

### **File 2: Scalability Engineering**
Learn to handle growth and performance optimization:
- Load testing with JMeter
- Database connection pooling
- Caching strategies
- Auto-scaling implementations

### **Advanced Reliability (Optional Extensions):**
- **Distributed tracing** with Zipkin
- **Circuit breaker** with custom metrics
- **Advanced chaos experiments** with Gremlin
- **SLI/SLO monitoring** with Prometheus

### **Production Deployment:**
- Docker containerization
- Kubernetes deployment
- Production monitoring setup
- Alerting configuration

---

## ✅ Mastery Checklist

**You've mastered File 1 when you can:**
- [ ] Explain when circuit breakers open and close
- [ ] Design thread pools for different workload types
- [ ] Implement meaningful fallback strategies
- [ ] Configure retry policies with proper backoff
- [ ] Design chaos experiments safely
- [ ] Monitor system reliability metrics
- [ ] Debug performance issues across bulkhead boundaries

**Portfolio Evidence:**
- Working Spring Boot application with reliability patterns
- Load test results showing bulkhead effectiveness
- Circuit breaker behavior under different failure rates
- Chaos engineering framework that reveals system weaknesses

---

**Ready for more?** Choose your next file:
- **File 2** (Scalability) - Handle massive load
- **File 3** (Relational Models) - Master SQL and JPA
- **File 6** (Storage Engines) - Build database internals

*This completes File 1. Each subsequent file will be similarly focused and complete.*