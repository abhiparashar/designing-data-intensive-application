package ddia.example.ddia.chaos;

import ddia.example.ddia.chaos.ChaosExperiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Exception injection chaos experiment.
 *
 * This experiment simulates various types of failures that can occur
 * in distributed systems by throwing or simulating exceptions. It helps test:
 *
 * - Exception handling and error recovery
 * - Circuit breaker behavior on failures
 * - Retry logic effectiveness
 * - Graceful degradation mechanisms
 * - Fallback strategy implementation
 *
 * This is typically a MEDIUM to HIGH severity experiment as it can
 * cause actual failures in the system flow.
 */
public class ExceptionInjectionExperiment implements ChaosExperiment {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionInjectionExperiment.class);
    private final Random random = new Random();

    // Different types of exceptions we can simulate
    private static final String[] EXCEPTION_TYPES = {
            "Database Connection Failed",
            "Network Timeout",
            "Service Unavailable",
            "Authentication Failed",
            "Rate Limit Exceeded",
            "Memory Allocation Error",
            "Disk Space Full",
            "External API Error"
    };

    // Configuration
    private static final double INJECTION_PROBABILITY = 0.6; // 60% chance to inject exception
    private static final int SIMULATION_DURATION_MS = 3000; // How long to "simulate" the exception

    @Override
    public String getName() {
        return "Exception Injection";
    }

    @Override
    public String getDescription() {
        return "Simulates various system failures and exceptions to test error handling and recovery mechanisms";
    }

    @Override
    public ExperimentSeverity getSeverity() {
        return ExperimentSeverity.MEDIUM;
    }

    @Override
    public long getExpectedDurationMs() {
        return SIMULATION_DURATION_MS + 1000; // Simulation time + buffer
    }

    @Override
    public void execute() {
        // Randomly decide whether to inject an exception scenario
        if (random.nextDouble() > INJECTION_PROBABILITY) {
            logger.info("💥 Exception injection skipped ({}% probability)",
                    (int) (INJECTION_PROBABILITY * 100));
            return;
        }

        // Select a random exception type to simulate
        String exceptionType = EXCEPTION_TYPES[random.nextInt(EXCEPTION_TYPES.length)];
        String errorCode = generateErrorCode();

        logger.warn("💥 Simulating exception: {} (Code: {})", exceptionType, errorCode);

        long startTime = System.currentTimeMillis();

        try {
            // Simulate the time it takes for the system to detect and handle the exception
            Thread.sleep(random.nextInt(SIMULATION_DURATION_MS));

            // Log the type of failure and its potential impact
            logFailureImpact(exceptionType);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("✅ Exception simulation completed after {}ms", duration);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("💥 Exception simulation interrupted after {}ms",
                    System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Logs the potential impact of different exception types.
     */
    private void logFailureImpact(String exceptionType) {
        switch (exceptionType) {
            case "Database Connection Failed" ->
                    logger.warn("🗄️ Database failure - Circuit breakers should open, fallbacks should activate");

            case "Network Timeout" -> logger.warn("🌐 Network timeout - Retry mechanisms should be triggered");

            case "Service Unavailable" ->
                    logger.warn("🚫 Service unavailable - Load balancers should route to healthy instances");

            case "Authentication Failed" ->
                    logger.warn("🔐 Authentication failure - Security fallbacks should activate");

            case "Rate Limit Exceeded" -> logger.warn("🚦 Rate limit hit - Backoff mechanisms should engage");

            case "Memory Allocation Error" ->
                    logger.warn("💾 Memory pressure - Garbage collection and resource cleanup needed");

            case "Disk Space Full" -> logger.warn("💽 Disk space critical - Log rotation and cleanup should trigger");

            case "External API Error" ->
                    logger.warn("🔌 External API failure - Circuit breakers and caching should help");

            default -> logger.warn("❓ Unknown exception type - General error handling should apply");
        }
    }

    /**
     * Generates a random error code for the simulated exception.
     */
    private String generateErrorCode() {
        String[] prefixes = {"ERR", "FAIL", "EX", "SYS"};
        String prefix = prefixes[random.nextInt(prefixes.length)];
        int code = 1000 + random.nextInt(9000); // 1000-9999
        return prefix + "-" + code;
    }

    /**
     * Gets a random exception type for external use.
     */
    public String getRandomExceptionType() {
        return EXCEPTION_TYPES[random.nextInt(EXCEPTION_TYPES.length)];
    }

    /**
     * Checks if an exception should be injected based on probability.
     */
    public boolean shouldInjectException() {
        return random.nextDouble() <= INJECTION_PROBABILITY;
    }

    /**
     * Simulates a specific type of exception.
     */
    public void simulateSpecificException(String exceptionType) {
        logger.warn("💥 Manually simulating exception: {}", exceptionType);
        logFailureImpact(exceptionType);
    }
}