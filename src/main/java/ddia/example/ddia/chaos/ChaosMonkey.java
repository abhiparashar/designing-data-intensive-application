package ddia.example.ddia.chaos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chaos Monkey - Orchestrates chaos engineering experiments.
 *
 * This component:
 * - Automatically starts chaos experiments at random intervals
 * - Manages different types of chaos experiments
 * - Provides safety mechanisms to prevent system damage
 * - Logs all chaos activities for analysis
 *
 * Inspired by Netflix's Chaos Monkey but simplified for educational purposes.
 */
@Component
public class ChaosMonkey {

    private static final Logger logger = LoggerFactory.getLogger(ChaosMonkey.class);
    private final Random random = new Random();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger experimentsExecuted = new AtomicInteger(0);
    private final List<ChaosExperiment> availableExperiments = new ArrayList<>();

    @Value("${app.chaos.enabled:true}")
    private boolean chaosEnabled;

    @Value("${app.chaos.interval:30s}")
    private String chaosInterval;

    /**
     * Initializes available chaos experiments.
     */
    public ChaosMonkey() {
        initializeExperiments();
    }

    /**
     * Starts the Chaos Monkey when the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startChaosMonkey() {
        if (!chaosEnabled) {
            logger.info("🐒 Chaos Monkey is DISABLED");
            return;
        }

        logger.info("🐒 Chaos Monkey starting up...");
        logger.info("📋 Available experiments: {}", availableExperiments.size());

        for (ChaosExperiment experiment : availableExperiments) {
            logger.info("   - {} ({}): {}",
                    experiment.getName(),
                    experiment.getSeverity(),
                    experiment.getDescription());
        }

        running.set(true);
        CompletableFuture.runAsync(this::runChaosLoop);

        logger.info("🚀 Chaos Monkey is now active!");
    }

    /**
     * Main chaos experiment loop.
     */
    private void runChaosLoop() {
        while (running.get()) {
            try {
                // Wait for next experiment (30-90 seconds by default)
                long waitTime = getRandomWaitTime();
                logger.debug("🐒 Chaos Monkey sleeping for {}ms", waitTime);
                Thread.sleep(waitTime);

                if (!running.get()) break;

                // Select and execute a random experiment
                ChaosExperiment experiment = selectRandomExperiment();
                if (experiment != null) {
                    executeExperiment(experiment);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("🐒 Chaos Monkey interrupted");
                break;
            } catch (Exception e) {
                logger.error("🐒 Chaos Monkey encountered an error: {}", e.getMessage(), e);
                // Continue running even if one experiment fails
            }
        }

        logger.info("🐒 Chaos Monkey stopped after executing {} experiments",
                experimentsExecuted.get());
    }

    /**
     * Executes a specific chaos experiment with safety measures.
     */
    private void executeExperiment(ChaosExperiment experiment) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
        int experimentNumber = experimentsExecuted.incrementAndGet();

        logger.info("🔥 [{}] Experiment #{}: {} ({})",
                timestamp, experimentNumber, experiment.getName(), experiment.getSeverity());

        long startTime = System.currentTimeMillis();

        try {
            // Execute the experiment
            experiment.execute();

            long duration = System.currentTimeMillis() - startTime;
            logger.info("✅ [{}] Experiment #{} completed in {}ms",
                    timestamp, experimentNumber, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("❌ [{}] Experiment #{} failed after {}ms: {}",
                    timestamp, experimentNumber, duration, e.getMessage());
        }
    }

    /**
     * Selects a random enabled experiment.
     */
    private ChaosExperiment selectRandomExperiment() {
        List<ChaosExperiment> enabledExperiments = availableExperiments.stream()
                .filter(ChaosExperiment::isEnabled)
                .toList();

        if (enabledExperiments.isEmpty()) {
            logger.warn("🐒 No enabled chaos experiments available");
            return null;
        }

        return enabledExperiments.get(random.nextInt(enabledExperiments.size()));
    }

    /**
     * Gets a random wait time between experiments.
     */
    private long getRandomWaitTime() {
        // Base interval: 30 seconds, with random variation of ±50%
        long baseInterval = 30000; // 30 seconds
        long variation = (long) (baseInterval * 0.5); // ±50%

        return baseInterval - variation + random.nextLong(2 * variation);
    }

    /**
     * Initializes the available chaos experiments.
     */
    private void initializeExperiments() {
        availableExperiments.add(new LatencyInjectionExperiment());
        availableExperiments.add(new ExceptionInjectionExperiment());
        availableExperiments.add(new ResourceExhaustionExperiment());

        logger.debug("🔧 Initialized {} chaos experiments", availableExperiments.size());
    }

    /**
     * Manually triggers a specific experiment (for testing).
     */
    public void triggerExperiment(String experimentName) {
        ChaosExperiment experiment = availableExperiments.stream()
                .filter(e -> e.getName().equalsIgnoreCase(experimentName))
                .findFirst()
                .orElse(null);

        if (experiment != null) {
            logger.info("🎯 Manually triggering experiment: {}", experimentName);
            executeExperiment(experiment);
        } else {
            logger.warn("🎯 Unknown experiment: {}", experimentName);
        }
    }

    /**
     * Stops the Chaos Monkey.
     */
    public void stop() {
        logger.info("🛑 Stopping Chaos Monkey...");
        running.set(false);
    }

    /**
     * Gets statistics about chaos experiments.
     */
    public ChaosStats getStats() {
        return new ChaosStats(
                running.get(),
                experimentsExecuted.get(),
                availableExperiments.size(),
                (int) availableExperiments.stream().filter(ChaosExperiment::isEnabled).count()
        );
    }

    /**
     * Statistics about chaos experiments.
     */
    public static class ChaosStats {
        private final boolean running;
        private final int experimentsExecuted;
        private final int totalExperiments;
        private final int enabledExperiments;

        public ChaosStats(boolean running, int experimentsExecuted,
                          int totalExperiments, int enabledExperiments) {
            this.running = running;
            this.experimentsExecuted = experimentsExecuted;
            this.totalExperiments = totalExperiments;
            this.enabledExperiments = enabledExperiments;
        }

        public boolean isRunning() { return running; }
        public int getExperimentsExecuted() { return experimentsExecuted; }
        public int getTotalExperiments() { return totalExperiments; }
        public int getEnabledExperiments() { return enabledExperiments; }
    }
}