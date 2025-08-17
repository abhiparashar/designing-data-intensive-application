package ddia.example.ddia.chaos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Latency injection chaos experiment.
 *
 * This experiment simulates network latency or slow downstream services
 * by introducing artificial delays in the system. It helps test:
 *
 * - Circuit breaker timeout configurations
 * - Retry mechanism effectiveness
 * - System behavior under slow dependencies
 * - User experience degradation patterns
 *
 * This is typically a LOW to MEDIUM severity experiment as it usually
 * doesn't cause complete failures, just slower responses.
 */
public class LatencyInjectionExperiment implements ChaosExperiment {
    private static final Logger logger = LoggerFactory.getLogger(LatencyInjectionExperiment.class);
    private final Random random = new Random();

    // Configuration for latency injection
    private static final int MIN_LATENCY_MS = 1000;  // 1 second minimum
    private static final int MAX_LATENCY_MS = 5000;  // 5 seconds maximum
    private static final double INJECTION_PROBABILITY = 0.7; // 70% chance to inject latency

    @Override
    public String getName() {
        return "Latency Injection";
    }

    @Override
    public String getDescription() {
        return "Simulates network latency and slow dependencies to test timeout handling and circuit breakers";
    }

    @Override
    public ExperimentSeverity getSeverity() {
        return ExperimentSeverity.MEDIUM;
    }

    @Override
    public long getExpectedDurationMs() {
        return MAX_LATENCY_MS + 1000; // Max latency + buffer
    }

    @Override
    public void execute() {
        // Randomly decide whether to inject latency
        if (random.nextDouble() > INJECTION_PROBABILITY) {
            logger.info("💤 Latency injection skipped ({}% probability)",
                    (int)(INJECTION_PROBABILITY * 100));
            return;
        }

        // Calculate random latency within bounds
        int latencyMs = MIN_LATENCY_MS + random.nextInt(MAX_LATENCY_MS - MIN_LATENCY_MS);

        logger.warn("💤 Injecting {}ms latency into system", latencyMs);

        long startTime = System.currentTimeMillis();

        try {
            // Simulate the latency
            Thread.sleep(latencyMs);

            long actualDelay = System.currentTimeMillis() - startTime;
            logger.info("✅ Latency injection completed - Actual delay: {}ms", actualDelay);

            // Log potential impacts
            if (actualDelay > 3000) {
                logger.warn("⚠️ High latency detected - This may trigger circuit breakers");
            } else if (actualDelay > 2000) {
                logger.info("ℹ️ Moderate latency - Testing retry mechanisms");
            }
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            logger.warn("💤 Latency injection interrupted after {}ms",
                    System.currentTimeMillis() - startTime);
        }
    }
}
