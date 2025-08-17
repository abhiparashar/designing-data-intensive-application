package ddia.example.ddia.chaos;

import ddia.example.ddia.chaos.ChaosExperiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Resource exhaustion chaos experiment.
 *
 * This experiment simulates resource pressure scenarios that can occur
 * in production systems. It helps test:
 *
 * - Memory pressure handling
 * - CPU utilization under stress
 * - Thread pool exhaustion scenarios
 * - Garbage collection behavior
 * - Resource cleanup mechanisms
 *
 * This is typically a HIGH severity experiment as it can significantly
 * impact system performance and stability.
 */
public class ResourceExhaustionExperiment implements ChaosExperiment {

    private static final Logger logger = LoggerFactory.getLogger(ResourceExhaustionExperiment.class);
    private final Random random = new Random();

    // Configuration
    private static final int MEMORY_CHUNK_SIZE_MB = 10; // 10MB chunks
    private static final int MAX_MEMORY_CHUNKS = 20;    // Max 200MB allocation
    private static final long PRESSURE_DURATION_MS = 8000; // 8 seconds of pressure
    private static final double INJECTION_PROBABILITY = 0.5; // 50% chance

    @Override
    public String getName() {
        return "Resource Exhaustion";
    }

    @Override
    public String getDescription() {
        return "Simulates memory pressure and CPU load to test resource management and cleanup mechanisms";
    }

    @Override
    public ExperimentSeverity getSeverity() {
        return ExperimentSeverity.HIGH;
    }

    @Override
    public long getExpectedDurationMs() {
        return PRESSURE_DURATION_MS + 2000; // Pressure duration + cleanup time
    }

    @Override
    public void execute() {
        // Randomly decide whether to inject resource pressure
        if (random.nextDouble() > INJECTION_PROBABILITY) {
            logger.info("🔥 Resource exhaustion skipped ({}% probability)",
                    (int)(INJECTION_PROBABILITY * 100));
            return;
        }

        ResourceType resourceType = selectRandomResourceType();

        logger.warn("🔥 Starting resource exhaustion experiment: {}", resourceType);

        long startTime = System.currentTimeMillis();

        try {
            switch (resourceType) {
                case MEMORY -> executeMemoryPressure();
                case CPU -> executeCpuPressure();
                case THREADS -> executeThreadPressure();
                case COMBINED -> executeCombinedPressure();
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("✅ Resource exhaustion experiment completed in {}ms", duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("❌ Resource exhaustion experiment failed after {}ms: {}",
                    duration, e.getMessage());
        }
    }

    /**
     * Executes memory pressure by allocating large amounts of memory.
     */
    private void executeMemoryPressure() throws InterruptedException {
        logger.warn("💾 Simulating memory pressure...");

        List<byte[]> memoryHog = new ArrayList<>();
        int chunksAllocated = 0;

        try {
            // Get initial memory stats
            Runtime runtime = Runtime.getRuntime();
            long initialFreeMemory = runtime.freeMemory();
            long initialTotalMemory = runtime.totalMemory();

            logger.info("📊 Initial memory: Free={}MB, Total={}MB",
                    initialFreeMemory / (1024 * 1024),
                    initialTotalMemory / (1024 * 1024));

            // Allocate memory in chunks
            int targetChunks = Math.min(MAX_MEMORY_CHUNKS, random.nextInt(15) + 5); // 5-20 chunks

            for (int i = 0; i < targetChunks; i++) {
                byte[] chunk = new byte[MEMORY_CHUNK_SIZE_MB * 1024 * 1024];
                memoryHog.add(chunk);
                chunksAllocated++;

                // Fill the chunk with random data to prevent optimization
                random.nextBytes(chunk);

                Thread.sleep(100); // Small delay between allocations

                if (i % 5 == 0) { // Log every 5 chunks
                    long currentFreeMemory = runtime.freeMemory();
                    logger.info("💾 Allocated {}MB ({}MB free)",
                            chunksAllocated * MEMORY_CHUNK_SIZE_MB,
                            currentFreeMemory / (1024 * 1024));
                }
            }

            logger.warn("💾 Memory pressure peak: {}MB allocated",
                    chunksAllocated * MEMORY_CHUNK_SIZE_MB);

            // Hold memory for the pressure duration
            Thread.sleep(PRESSURE_DURATION_MS / 2);

        } finally {
            // Clean up allocated memory
            memoryHog.clear();

            // Suggest garbage collection
            System.gc();

            // Wait for GC to potentially run
            Thread.sleep(1000);

            Runtime runtime = Runtime.getRuntime();
            long finalFreeMemory = runtime.freeMemory();

            logger.info("🧹 Memory cleanup completed. Free memory: {}MB",
                    finalFreeMemory / (1024 * 1024));
        }
    }

    /**
     * Executes CPU pressure by running intensive calculations.
     */
    private void executeCpuPressure() throws InterruptedException {
        logger.warn("⚡ Simulating CPU pressure...");

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(availableProcessors);

        try {
            logger.info("🖥️ Starting CPU-intensive tasks on {} cores", availableProcessors);

            // Start CPU-intensive tasks on all available cores
            for (int i = 0; i < availableProcessors; i++) {
                final int taskId = i;
                executorService.submit(() -> {
                    logger.debug("⚡ CPU task {} starting", taskId);

                    long endTime = System.currentTimeMillis() + PRESSURE_DURATION_MS;
                    long iterations = 0;

                    // CPU-intensive loop
                    while (System.currentTimeMillis() < endTime) {
                        // Perform some CPU-intensive calculations
                        double result = 0;
                        for (int j = 0; j < 10000; j++) {
                            result += Math.sqrt(Math.random() * 1000000);
                            result = Math.sin(result) + Math.cos(result);
                        }
                        iterations++;

                        // Prevent infinite tight loops
                        if (iterations % 1000 == 0) {
                            Thread.yield();
                        }
                    }

                    logger.debug("⚡ CPU task {} completed {} iterations", taskId, iterations);
                });
            }

            // Wait for tasks to complete
            Thread.sleep(PRESSURE_DURATION_MS + 1000);

        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }

            logger.info("🧹 CPU pressure experiment completed");
        }
    }

    /**
     * Executes thread exhaustion by creating many threads.
     */
    private void executeThreadPressure() throws InterruptedException {
        logger.warn("🧵 Simulating thread pressure...");

        List<Thread> threads = new ArrayList<>();
        int threadCount = 50 + random.nextInt(50); // 50-100 threads

        try {
            logger.info("🧵 Creating {} threads", threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                Thread thread = new Thread(() -> {
                    try {
                        logger.debug("🧵 Thread {} sleeping", threadId);
                        Thread.sleep(PRESSURE_DURATION_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, "ChaosThread-" + i);

                threads.add(thread);
                thread.start();

                // Small delay between thread creation
                Thread.sleep(10);
            }

            logger.warn("🧵 Thread pressure peak: {} active threads", threads.size());

            // Wait for threads to complete
            for (Thread thread : threads) {
                thread.join(PRESSURE_DURATION_MS + 1000);
            }

        } finally {
            // Ensure all threads are cleaned up
            for (Thread thread : threads) {
                if (thread.isAlive()) {
                    thread.interrupt();
                }
            }

            logger.info("🧹 Thread pressure experiment completed");
        }
    }

    /**
     * Executes combined resource pressure (memory + CPU).
     */
    private void executeCombinedPressure() throws InterruptedException {
        logger.warn("💥 Simulating COMBINED resource pressure...");

        // Run memory and CPU pressure in parallel
        Thread memoryThread = new Thread(() -> {
            try {
                executeMemoryPressure();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "ChaosMemoryPressure");

        Thread cpuThread = new Thread(() -> {
            try {
                executeCpuPressure();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "ChaosCpuPressure");

        memoryThread.start();
        Thread.sleep(1000); // Stagger the start
        cpuThread.start();

        memoryThread.join();
        cpuThread.join();

        logger.info("💥 Combined resource pressure completed");
    }

    /**
     * Selects a random resource type for the experiment.
     */
    private ResourceType selectRandomResourceType() {
        ResourceType[] types = ResourceType.values();
        return types[random.nextInt(types.length)];
    }

    /**
     * Types of resources to pressure test.
     */
    private enum ResourceType {
        MEMORY,
        CPU,
        THREADS,
        COMBINED
    }
}