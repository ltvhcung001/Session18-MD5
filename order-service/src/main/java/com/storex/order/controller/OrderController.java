package com.storex.order.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final RestTemplate restTemplate;

    @Value("${services.user-service.url}")
    private String userServiceUrl;

    public OrderController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processOrder(@RequestBody Map<String, Object> request) {
        String orderId = String.valueOf(request.getOrDefault("orderId", "ORD-DEFAULT"));
        String userId = String.valueOf(request.getOrDefault("userId", "USER-DEFAULT"));
        double amount = Double.parseDouble(String.valueOf(request.getOrDefault("amount", "100.0")));
        boolean shouldFail = Boolean.parseBoolean(String.valueOf(request.getOrDefault("shouldFail", "false")));

        log.info("[OrderService] Bắt đầu xử lý đơn hàng orderId={}, chuẩn bị gọi User Service xác thực...", orderId);

        // Chuẩn bị payload gọi User Service
        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("orderId", orderId);
        userRequest.put("userId", userId);
        userRequest.put("shouldFail", shouldFail);

        String targetUrl = userServiceUrl + "/api/users/verify";
        log.info("[OrderService] Gọi User Service tại URL: {}", targetUrl);

        try {
            // RestTemplate được inject traceId & spanId tự động bởi Micrometer Tracing
            ResponseEntity<Map> userResponse = restTemplate.postForEntity(targetUrl, userRequest, Map.class);
            log.info("[OrderService] Nhận kết quả từ User Service: status={}", userResponse.getStatusCode());

            Map<String, Object> response = new HashMap<>();
            response.put("service", "order-service");
            response.put("orderId", orderId);
            response.put("status", "ORDER_CREATED");
            response.put("amount", amount);
            response.put("userVerification", userResponse.getBody());
            return ResponseEntity.ok(response);

        } catch (HttpStatusCodeException ex) {
            log.error("[OrderService] GỌI USER SERVICE THẤT BẠI: HttpStatusCode={}, ResponseBody={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "order-service");
            errorResponse.put("orderId", orderId);
            errorResponse.put("status", "ORDER_FAILED");
            errorResponse.put("errorMessage", "Failed to verify user from User Service: " + ex.getStatusCode());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
        } catch (Exception ex) {
            log.error("[OrderService] Lỗi kết nối tới User Service: {}", ex.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "order-service");
            errorResponse.put("status", "SERVICE_UNAVAILABLE");
            errorResponse.put("errorMessage", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }
}
