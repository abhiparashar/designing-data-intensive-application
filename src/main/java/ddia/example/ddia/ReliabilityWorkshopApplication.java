package ddia.example.ddia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the Reliability Engineering Workshop.
 *
 * This Spring Boot application demonstrates:
 * - Circuit Breaker patterns
 * - Bulkhead isolation
 * - Retry mechanisms
 * - Chaos engineering
 * - Monitoring and observability
 */
@SpringBootApplication
@EnableAsync
public class ReliabilityWorkshopApplication {
	private static final Logger logger = LoggerFactory.getLogger(ReliabilityWorkshopApplication.class);
	public static void main(String[] args) {
		logger.info("🚀 Starting Reliability Engineering Workshop Application...");
		SpringApplication.run(ReliabilityWorkshopApplication.class, args);
	}

	/**
	 * Application startup event handler.
	 * Logs important information about the application setup.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		logger.info("✅ Reliability Workshop Application is ready!");
		logger.info("📊 Monitoring endpoints available at: http://localhost:8080/actuator");
		logger.info("🔍 Health check: http://localhost:8080/actuator/health");
		logger.info("📈 Metrics: http://localhost:8080/actuator/metrics");
		logger.info("⚡ Circuit Breakers: http://localhost:8080/actuator/circuitbreakers");
		logger.info("🔄 Retries: http://localhost:8080/actuator/retries");
		logger.info("🛡️ Bulkheads: http://localhost:8080/actuator/bulkheads");
		logger.info("");
		logger.info("🧪 Test endpoints:");
		logger.info("   Payment: POST http://localhost:8080/api/payments/process");
		logger.info("   Order: POST http://localhost:8080/api/orders/process");
		logger.info("");
		logger.info("🐒 Chaos Monkey is active and will start experiments soon...");
	}
}
