# Reliability Engineering Workshop - Spring Boot Project

🛠️ **Build Fault-Tolerant Systems with Spring Boot & Java**

This project demonstrates reliability engineering patterns including Circuit Breakers, Bulkhead isolation, Retry mechanisms, and Chaos Engineering.

## 📁 Project Structure

```
reliability-workshop/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/
│   │   │       ├── ReliabilityWorkshopApplication.java    # Main application
│   │   │       ├── config/
│   │   │       │   └── BulkheadConfiguration.java         # Thread pool config
│   │   │       ├── controller/
│   │   │       │   ├── OrderController.java               # Order endpoints
│   │   │       │   └── PaymentController.java             # Payment endpoints
│   │   │       ├── service/
│   │   │       │   ├── OrderService.java                  # Order processing
│   │   │       │   └── PaymentService.java                # Payment with circuit breaker
│   │   │       ├── model/
│   │   │       │   ├── OrderRequest.java & OrderResponse.java
│   │   │       │   └── PaymentRequest.java & PaymentResponse.java
│   │   │       ├── exception/
│   │   │       │   └── PaymentException.java              # Custom exceptions
│   │   │       └── chaos/
│   │   │           ├── ChaosMonkey.java                   # Chaos orchestrator
│   │   │           ├── ChaosExperiment.java               # Experiment interface
│   │   │           ├── LatencyInjectionExperiment.java    # Latency chaos
│   │   │           ├── ExceptionInjectionExperiment.java  # Exception chaos
│   │   │           └── ResourceExhaustionExperiment.java  # Resource chaos
│   │   └── resources/
│   │       └── application.yml                            # Configuration
│   └── test/
│       └── java/com/example/
│           ├── service/CircuitBreakerTest.java            # Circuit breaker tests
│           └── controller/OrderControllerTest.java        # Bulkhead tests
├── pom.xml                                                # Maven dependencies
└── README.md                                              # This file
```

## 🚀 Quick Start

### Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **IDE** (IntelliJ IDEA, Eclipse, or VS Code)

### 1. Setup Project

```bash
# Create project directory
mkdir reliability-workshop
cd reliability-workshop

# Copy all the provided files into their respective directories
# (See file structure above)
```

### 2. Build and Run

```bash
# Clean and compile
mvn clean compile

# Run the application
mvn spring-boot:run
```

**Application starts on:** `http://localhost:8080`

### 3. Verify Setup

Check the logs for:
```
✅ Reliability Workshop Application is ready!
📊 Monitoring endpoints available at: http://localhost:8080/actuator
🐒 Chaos Monkey is active and will start experiments soon...
```

## 🧪 Testing the Patterns

### Circuit Breaker Pattern

**Test payment circuit breaker:**
```bash
# Test successful payment
curl -X POST http://localhost:8080/api/payments/process \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "test_order_1",
    "amount": 99.99,
    "cardToken": "card_12345"
  }'

# Test multiple payments to trigger circuit breaker
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/payments/process \
    -H "Content-Type: application/json" \
    -d "{
      \"orderId\": \"load_test_$i\",
      \"amount\": $(($i * 10)).00,
      \"cardToken\": \"card_$i\"
    }"
  echo ""
  sleep 1
done
```

**Expected behavior:**
- First few requests: Normal processing (some succeed, some fail)
- After failures exceed threshold: Circuit breaker opens
- Fallback responses: Status = "PENDING_MANUAL_REVIEW"
- After wait time: Circuit breaker allows test requests (half-open)

### Bulkhead Pattern

**Test order processing with bulkheads:**
```bash
# Submit multiple orders concurrently
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/orders/process \
    -H "Content-Type: application/json" \
    -d "{
      \"orderId\": \"bulk_test_$i\",
      \"amount\": $(($i * 25)).00,
      \"cardToken\": \"card_$i\",
      \"customerId\": \"customer_$i\"
    }" &
done

# Wait for all requests
wait
```

**Expected behavior:**
- Orders processed in parallel using different thread pools
- Payment operations in "Payment-" threads
- Inventory operations in "Inventory-" threads
- Notifications in "Notification-" threads
- No single slow operation blocks others

### Chaos Engineering

**Chaos Monkey runs automatically** and will log experiments:
```
🔥 Experiment #1: Latency Injection (MEDIUM)
💤 Injecting 3000ms latency into system
✅ Experiment #1 completed in 3001ms
```

**Manually trigger chaos experiments:**
```bash
# View chaos statistics (if you add an endpoint)
curl http://localhost:8080/actuator/health
```

## 📊 Monitoring & Observability

### Actuator Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# All metrics
curl http://localhost:8080/actuator/metrics

# Circuit breaker status
curl http://localhost:8080/actuator/circuitbreakers

# Retry statistics
curl http://localhost:8080/actuator/retries

# Bulkhead status
curl http://localhost:8080/actuator/bulkheads

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

### Key Metrics to Watch

**Circuit Breaker Metrics:**
- `resilience4j.circuitbreaker.calls` - Total calls
- `resilience4j.circuitbreaker.state` - Current state (CLOSED/OPEN/HALF_OPEN)
- `resilience4j.circuitbreaker.failure.rate` - Current failure rate

**Bulkhead Metrics:**
- `resilience4j.bulkhead.available.concurrent.calls` - Available capacity
- `resilience4j.bulkhead.max.allowed.concurrent.calls` - Maximum capacity

**Application Metrics:**
- `http.server.requests` - HTTP request metrics
- `jvm.memory.used` - Memory usage
- `jvm.threads.live` - Active threads

## 🧪 Running Tests

### Unit Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CircuitBreakerTest

# Run with detailed output
mvn test -Dtest=OrderControllerTest -Dspring.profiles.active=test
```

### Integration Tests

**Circuit Breaker Test:**
```bash
mvn test -Dtest=CircuitBreakerTest#testCircuitBreakerFallback
```

**Bulkhead Test:**
```bash
mvn test -Dtest=OrderControllerTest#testBulkheadIsolation
```

## 🔧 Configuration

### Circuit Breaker Settings (`application.yml`)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      payment-service:
        sliding-window-size: 10          # Number of calls to track
        failure-rate-threshold: 50       # 50% failure rate opens circuit
        wait-duration-in-open-state: 10s # Stay open for 10 seconds
        minimum-number-of-calls: 5       # Minimum calls before calculating rate
```

### Thread Pool Settings

```yaml
# In BulkheadConfiguration.java
paymentExecutor:
  core-pool-size: 5
  max-pool-size: 10
  queue-capacity: 25

inventoryExecutor:
  core-pool-size: 3
  max-pool-size: 5
  queue-capacity: 15
```

### Chaos Engineering Settings

```yaml
app:
  chaos:
    enabled: true
    interval: 30s
    experiments:
      latency:
        enabled: true
        min-delay: 1000
        max-delay: 5000
```

## 🎯 Learning Objectives

After running this project, you'll understand:

### ✅ Circuit Breaker Pattern
- **When it opens:** After 50% failure rate in 10 calls
- **What it does:** Returns fallback responses instead of calling failing service
- **How it recovers:** Allows test calls after wait period

### ✅ Bulkhead Pattern
- **Thread isolation:** Different operations use separate thread pools
- **Resource protection:** Slow payments don't block inventory checks
- **Graceful degradation:** Individual failures don't cascade

### ✅ Retry Pattern
- **Exponential backoff:** 2s, 4s, 8s delays
- **Failure types:** Only retries specific exceptions
- **Retry limits:** Maximum 3 attempts

### ✅ Chaos Engineering
- **Proactive testing:** Inject failures before they happen naturally
- **System resilience:** Verify fallbacks actually work
- **Failure scenarios:** Latency, exceptions, resource exhaustion

## 🐛 Troubleshooting

### Common Issues

**Circuit Breaker Not Opening:**
- Check failure rate threshold (default 50%)
- Verify minimum number of calls (default 5)
- Look for correct exception types being thrown

**Bulkhead Not Working:**
- Verify @Async annotations are present
- Check thread pool configuration
- Ensure @EnableAsync is on main application class

**Chaos Monkey Not Running:**
- Check `app.chaos.enabled=true` in application.yml
- Look for startup logs: "🐒 Chaos Monkey starting up..."
- Verify experiments are enabled individually

**Tests Failing:**
- Increase timeout values for slow test environments
- Check that Spring Boot test configuration is correct
- Verify all dependencies are properly injected

### Debug Logging

Enable debug logging for detailed information:

```yaml
logging:
  level:
    com.example: DEBUG
    io.github.resilience4j: DEBUG
```

## 📈 Next Steps

### Extend the Project

1. **Add Database Integration**
    - JPA entities for orders and payments
    - Database connection pool tuning
    - Database circuit breakers

2. **Add Metrics Dashboard**
    - Grafana dashboard for Prometheus metrics
    - Custom business metrics
    - Alerting rules

3. **Add More Chaos Experiments**
    - Network partitions
    - Disk I/O delays
    - CPU throttling
    - Random process kills

4. **Production Readiness**
    - Docker containerization
    - Kubernetes deployment
    - Production monitoring setup
    - Log aggregation

### Related Patterns to Learn

- **Rate Limiting:** Control request throughput
- **Timeout Patterns:** Handle slow operations
- **Caching:** Reduce external dependencies
- **Load Balancing:** Distribute traffic

## 📚 Additional Resources

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Chaos Engineering Principles](https://principlesofchaos.org/)
- [Building Microservices by Sam Newman](https://samnewman.io/books/building_microservices/)

---

🎉 **Congratulations!** You've built a production-ready reliability engineering demo. The patterns you've learned here are used by companies like Netflix, Amazon, and Google to build resilient distributed systems.

**Ready for the next challenge?** Try implementing these patterns in a microservices architecture with service mesh!