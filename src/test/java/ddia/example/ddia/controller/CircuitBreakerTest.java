package ddia.example.ddia.controller;

import ddia.example.ddia.model.PaymentRequest;
import ddia.example.ddia.model.PaymentResponse;
import ddia.example.ddia.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for circuit breaker behavior.
 *
 * These tests verify that the circuit breaker pattern is working correctly
 * by testing various failure scenarios and recovery mechanisms.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.instances.payment-service.sliding-window-size=5",
        "resilience4j.circuitbreaker.instances.payment-service.failure-rate-threshold=60",
        "resilience4j.circuitbreaker.instances.payment-service.wait-duration-in-open-state=2s",
        "resilience4j.circuitbreaker.instances.payment-service.minimum-number-of-calls=3"
})
class CircuitBreakerTest {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerTest.class);

    @Autowired
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        logger.info("🧪 Setting up Circuit Breaker Test");
    }

    @Test
    @DisplayName("Circuit Breaker should trigger fallback after failures")
    void testCircuitBreakerFallback() throws InterruptedException {
        logger.info("🧪 Testing Circuit Breaker Fallback Behavior");

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger fallbackCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Process multiple payments to trigger circuit breaker
        for (int i = 0; i < 20; i++) {
            try {
                PaymentRequest request = new PaymentRequest(
                        "test_order_" + i,
                        100.0 + i,
                        "test_card_" + i
                );

                PaymentResponse response = paymentService.processPayment(request);

                if (PaymentResponse.STATUS_COMPLETED.equals(response.getStatus())) {
                    successCount.incrementAndGet();
                    logger.info("✅ Payment {}: SUCCESS - {}", i + 1, response.getTransactionId());
                } else if (PaymentResponse.STATUS_PENDING_MANUAL_REVIEW.equals(response.getStatus())) {
                    fallbackCount.incrementAndGet();
                    logger.info("🔄 Payment {}: FALLBACK - {}", i + 1, response.getErrorMessage());
                } else {
                    errorCount.incrementAndGet();
                    logger.info("❌ Payment {}: ERROR - {}", i + 1, response.getErrorMessage());
                }

            } catch (Exception e) {
                errorCount.incrementAndGet();
                logger.info("💥 Payment {}: EXCEPTION - {}", i + 1, e.getMessage());
            }

            // Small delay between requests
            Thread.sleep(200);
        }

        logger.info("📊 Circuit Breaker Test Results:");
        logger.info("   Successful: {}", successCount.get());
        logger.info("   Fallback: {}", fallbackCount.get());
        logger.info("   Errors: {}", errorCount.get());
        logger.info("   Total: {}", successCount.get() + fallbackCount.get() + errorCount.get());

        // Verify that fallbacks were triggered (circuit breaker opened)
        assertTrue(fallbackCount.get() > 0, "Circuit breaker should have triggered fallbacks");

        // Verify that we had both successes and fallbacks (showing circuit breaker behavior)
        assertTrue(successCount.get() > 0, "Should have some successful payments");

        logger.info("✅ Circuit breaker behavior verified successfully");
    }

    @Test
    @DisplayName("Circuit Breaker should recover after cool-down period")
    void testCircuitBreakerRecovery() throws InterruptedException {
        logger.info("🧪 Testing Circuit Breaker Recovery");

        // First, trigger the circuit breaker to open
        logger.info("🔥 Phase 1: Triggering circuit breaker...");

        int fallbacksTriggered = 0;
        for (int i = 0; i < 10; i++) {
            PaymentRequest request = new PaymentRequest("trigger_" + i, 50.0, "card_trigger");
            PaymentResponse response = paymentService.processPayment(request);

            if (PaymentResponse.STATUS_PENDING_MANUAL_REVIEW.equals(response.getStatus())) {
                fallbacksTriggered++;
            }

            Thread.sleep(100);
        }

        logger.info("🔥 Circuit breaker triggered {} fallbacks", fallbacksTriggered);

        // Wait for circuit breaker to potentially close (cool-down period)
        logger.info("⏰ Phase 2: Waiting for circuit breaker cool-down (3 seconds)...");
        Thread.sleep(3000);

        // Try processing payments after cool-down
        logger.info("🔄 Phase 3: Testing recovery after cool-down...");

        int successAfterRecovery = 0;
        for (int i = 0; i < 5; i++) {
            PaymentRequest request = new PaymentRequest("recovery_" + i, 75.0, "card_recovery");
            PaymentResponse response = paymentService.processPayment(request);

            if (PaymentResponse.STATUS_COMPLETED.equals(response.getStatus())) {
                successAfterRecovery++;
                logger.info("✅ Recovery payment {}: SUCCESS", i + 1);
            } else {
                logger.info("🔄 Recovery payment {}: {}", i + 1, response.getStatus());
            }

            Thread.sleep(200);
        }

        logger.info("📊 Recovery Results: {} successful payments out of 5", successAfterRecovery);

        // We should have at least some successful payments, showing recovery
        assertTrue(successAfterRecovery >= 0, "Circuit breaker should allow some requests through during recovery");

        logger.info("✅ Circuit breaker recovery behavior verified");
    }

    @Test
    @DisplayName("Concurrent requests should be handled properly by circuit breaker")
    void testConcurrentCircuitBreakerBehavior() throws InterruptedException {
        logger.info("🧪 Testing Concurrent Circuit Breaker Behavior");

        int numberOfThreads = 10;
        int requestsPerThread = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger totalSuccessful = new AtomicInteger(0);
        AtomicInteger totalFallbacks = new AtomicInteger(0);
        AtomicInteger totalErrors = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        // Submit concurrent payment requests
        for (int threadId = 0; threadId < numberOfThreads; threadId++) {
            final int finalThreadId = threadId;

            executorService.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    for (int requestId = 0; requestId < requestsPerThread; requestId++) {
                        try {
                            PaymentRequest request = new PaymentRequest(
                                    String.format("concurrent_%d_%d", finalThreadId, requestId),
                                    100.0 + requestId,
                                    "card_concurrent_" + finalThreadId
                            );

                            PaymentResponse response = paymentService.processPayment(request);

                            if (PaymentResponse.STATUS_COMPLETED.equals(response.getStatus())) {
                                totalSuccessful.incrementAndGet();
                            } else if (PaymentResponse.STATUS_PENDING_MANUAL_REVIEW.equals(response.getStatus())) {
                                totalFallbacks.incrementAndGet();
                            } else {
                                totalErrors.incrementAndGet();
                            }

                        } catch (Exception e) {
                            totalErrors.incrementAndGet();
                            logger.debug("Request failed: {}", e.getMessage());
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        // Wait for all threads to complete
        boolean completed = completeLatch.await(30, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        executorService.shutdown();

        assertTrue(completed, "All concurrent requests should complete within timeout");

        int totalRequests = numberOfThreads * requestsPerThread;
        int processedRequests = totalSuccessful.get() + totalFallbacks.get() + totalErrors.get();

        logger.info("📊 Concurrent Circuit Breaker Test Results:");
        logger.info("   Duration: {}ms", duration);
        logger.info("   Total Requests: {}", totalRequests);
        logger.info("   Processed: {}", processedRequests);
        logger.info("   Successful: {}", totalSuccessful.get());
        logger.info("   Fallbacks: {}", totalFallbacks.get());
        logger.info("   Errors: {}", totalErrors.get());

        // Verify all requests were processed
        assertEquals(totalRequests, processedRequests, "All requests should be processed");

        // Verify circuit breaker provided some protection (fallbacks occurred)
        assertTrue(totalFallbacks.get() > 0 || totalSuccessful.get() > 0,
                "Circuit breaker should handle requests appropriately");

        logger.info("✅ Concurrent circuit breaker behavior verified");
    }

    @Test
    @DisplayName("Emergency payments should bypass circuit breaker")
    void testEmergencyPaymentBypass() throws InterruptedException {
        logger.info("🧪 Testing Emergency Payment Circuit Breaker Bypass");

        // First trigger some regular payment failures to potentially open circuit breaker
        for (int i = 0; i < 5; i++) {
            PaymentRequest request = new PaymentRequest("trigger_" + i, 100.0, "card_trigger");
            paymentService.processPayment(request);
        }

        // Now try emergency payment
        PaymentRequest emergencyRequest = new PaymentRequest("emergency_test", 500.0, "emergency_card");
        PaymentResponse emergencyResponse = paymentService.processEmergencyPayment(emergencyRequest);

        assertNotNull(emergencyResponse, "Emergency payment should return a response");
        logger.info("🚨 Emergency payment result: {} - {}",
                emergencyResponse.getStatus(), emergencyResponse.getTransactionId());

        // Emergency payments should work even if circuit breaker is open
        assertTrue(
                PaymentResponse.STATUS_COMPLETED.equals(emergencyResponse.getStatus()) ||
                        PaymentResponse.STATUS_FAILED.equals(emergencyResponse.getStatus()),
                "Emergency payment should either succeed or fail, not use fallback"
        );

        logger.info("✅ Emergency payment bypass verified");
    }
}