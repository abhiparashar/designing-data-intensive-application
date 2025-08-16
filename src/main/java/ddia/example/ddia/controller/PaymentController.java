package ddia.example.ddia.controller;


import ddia.example.ddia.model.PaymentRequest;
import ddia.example.ddia.model.PaymentResponse;
import ddia.example.ddia.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for payment operations.
 *
 * This controller provides endpoints for:
 * - Processing payments
 * - Emergency payment processing
 * - Health checks
 */
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*") // For testing purposes
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Processes an emergency payment request.
     *
     * This endpoint bypasses the circuit breaker for high-priority payments
     * such as VIP customers or critical orders that must be processed even
     * when the main payment service is experiencing issues.
     *
     * @param request The payment request with order details
     * @return Payment response with transaction details
     */
}
