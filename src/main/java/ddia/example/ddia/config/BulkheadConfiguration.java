package ddia.example.ddia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration class for implementing the Bulkhead pattern.
 *
 * The Bulkhead pattern isolates different types of work into separate thread pools
 * to prevent one slow or failing service from affecting others.
 *
 * This configuration creates separate thread pools for:
 * - Payment processing (high priority, more threads)
 * - Inventory checking (medium priority, moderate threads)
 * - Notifications (low priority, fewer threads)
 */
public class BulkheadConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(BulkheadConfiguration.class);

    /**
     * Thread pool for payment processing operations.
     *
     * Payment is critical, so we allocate more resources:
     * - Core pool: 5 threads (always available)
     * - Max pool: 10 threads (can scale up under load)
     * - Queue: 25 capacity (buffer for burst traffic)
     * - Rejection policy: CallerRunsPolicy (use calling thread if pool is full)
     */
    @Bean(name = "paymentExecutor")
    public Executor paymentExecutor(){
        logger.info("🏦 Configuring Payment thread pool executor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("Payment-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Allow core threads to timeout when
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        logger.info("✅ Payment executor configured: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * Thread pool for inventory checking operations.
     *
     * Inventory checks are important but can tolerate some delay:
     * - Core pool: 3 threads
     * - Max pool: 5 threads
     * - Queue: 15 capacity
     * - Rejection policy: CallerRunsPolicy (graceful degradation)
     */
    @Bean(name = "inventoryExecutor")
    public Executor inventoryExecutor() {
        logger.info("📦 Configuring Inventory thread pool executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(15);
        executor.setThreadNamePrefix("Inventory-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);

        executor.initialize();

        logger.info("✅ Inventory executor configured: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }


    /**
     * Thread pool for notification operations.
     *
     * Notifications are nice-to-have and can be dropped under extreme load:
     * - Core pool: 2 threads
     * - Max pool: 4 threads
     * - Queue: 10 capacity
     * - Rejection policy: AbortPolicy (drop notifications if overwhelmed)
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        logger.info("📧 Configuring Notification thread pool executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("Notification-");

        // AbortPolicy: Drop notifications if pool is overwhelmed
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(false); // Don't wait for notifications
        executor.setAwaitTerminationSeconds(10);

        executor.initialize();

        logger.info("✅ Notification executor configured: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * General purpose thread pool for miscellaneous async operations.
     */
    @Bean(name = "generalExecutor")
    public Executor generalExecutor() {
        logger.info("⚙️ Configuring General purpose thread pool executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("General-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        logger.info("✅ General executor configured: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}
