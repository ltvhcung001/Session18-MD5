package com.storex.payment.controller;

import com.storex.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Endpoint kiểm thử nhanh qua trình duyệt hoặc cURL/Postman
     * GET /api/payments/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testPayment() {
        log.info("[INFO] [PaymentController] Nhận request GET /api/payments/test");
        log.debug("[DEBUG] [PaymentController] Controller debug log: Chuẩn bị gọi PaymentService.processPayment");

        Map<String, Object> result = paymentService.processPayment("ORD-8888", 250.75, "CUST-1001", "VNPay");

        log.debug("[DEBUG] [PaymentController] Controller debug log: Kết quả trả về từ PaymentService -> status: {}", result.get("status"));
        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint xử lý thanh toán thực tế
     * POST /api/payments/checkout
     */
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody(required = false) Map<String, Object> request) {
        String orderId = request != null && request.containsKey("orderId") 
                ? String.valueOf(request.get("orderId")) 
                : "ORD-" + System.currentTimeMillis();
        double amount = request != null && request.containsKey("amount") 
                ? Double.parseDouble(String.valueOf(request.get("amount"))) 
                : 99.0;
        String customerId = request != null && request.containsKey("customerId") 
                ? String.valueOf(request.get("customerId")) 
                : "CUST-DEFAULT";
        String gateway = request != null && request.containsKey("gateway") 
                ? String.valueOf(request.get("gateway")) 
                : "MOMO";

        log.info("[INFO] [PaymentController] Nhận request POST /api/payments/checkout: orderId={}", orderId);
        log.debug("[DEBUG] [PaymentController] Request payload chi tiết: customerId={}, gateway={}, amount={}", customerId, gateway, amount);

        Map<String, Object> result = paymentService.processPayment(orderId, amount, customerId, gateway);
        return ResponseEntity.ok(result);
    }
}
