package com.storex.payment.controller;

import com.storex.payment.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Endpoint mô phỏng tạo đơn hàng:
     * POST /api/orders/checkout
     */
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkoutOrder(@RequestBody(required = false) Map<String, Object> request) {
        String userId = request != null && request.containsKey("userId") 
                ? String.valueOf(request.get("userId")) 
                : "USER-9999";
        String orderId = request != null && request.containsKey("orderId") 
                ? String.valueOf(request.get("orderId")) 
                : "ORD-" + System.currentTimeMillis();
        double amount = request != null && request.containsKey("amount") 
                ? Double.parseDouble(String.valueOf(request.get("amount"))) 
                : 350.0;
        boolean shouldFail = request != null && Boolean.parseBoolean(String.valueOf(request.get("shouldFail")));

        log.info("[OrderController] Nhận request checkout: current MDC requestId = {}", MDC.get("requestId"));

        Map<String, Object> result = orderService.createOrder(userId, orderId, amount, shouldFail);
        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint kiểm thử nhanh mô phỏng tình huống đề bài (Transaction Failed)
     * GET /api/orders/fail-demo
     */
    @GetMapping("/fail-demo")
    public ResponseEntity<Map<String, Object>> failDemo() {
        log.info("[OrderController] Thực hiện mô phỏng giao dịch thất bại để test Kibana filter theo requestId");
        Map<String, Object> result = orderService.createOrder("USER-KH-8888", "ORD-FAILED-101", 1200.0, true);
        return ResponseEntity.ok(result);
    }
}
