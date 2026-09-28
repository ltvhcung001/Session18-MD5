package com.storex.gateway.controller;

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
import java.util.UUID;

@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private final RestTemplate restTemplate;

    @Value("${services.order-service.url}")
    private String orderServiceUrl;

    public GatewayController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Endpoint chính tiếp nhận luồng checkout từ Client:
     * POST /api/gateway/checkout
     * Luồng: Client -> Gateway (8080) -> Order Service (8081) -> User Service (8082)
     */
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody(required = false) Map<String, Object> request) {
        String userId = (request != null && request.containsKey("userId")) 
                ? String.valueOf(request.get("userId")) 
                : "USER-ALICE-100";
        double amount = (request != null && request.containsKey("amount")) 
                ? Double.parseDouble(String.valueOf(request.get("amount"))) 
                : 250.0;
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        boolean shouldFail = request != null && Boolean.parseBoolean(String.valueOf(request.get("shouldFail")));

        log.info("[Gateway] Nhận request checkout từ Client: userId={}, amount={}, sinh orderId={}", userId, amount, orderId);

        // Chuẩn bị payload gửi sang Order Service
        Map<String, Object> orderPayload = new HashMap<>();
        orderPayload.put("orderId", orderId);
        orderPayload.put("userId", userId);
        orderPayload.put("amount", amount);
        orderPayload.put("shouldFail", shouldFail);

        String targetOrderUrl = orderServiceUrl + "/api/orders/process";
        log.info("[Gateway] Chuyển tiếp request sang Order Service tại: {}", targetOrderUrl);

        try {
            // Brave tự động chèn header traceparent/b3 sang Order Service
            ResponseEntity<Map> orderResponse = restTemplate.postForEntity(targetOrderUrl, orderPayload, Map.class);
            log.info("[Gateway] Nhận kết quả thành công từ Order Service: status={}", orderResponse.getStatusCode());

            Map<String, Object> gatewayResponse = new HashMap<>();
            gatewayResponse.put("gatewayStatus", "SUCCESS");
            gatewayResponse.put("orderResult", orderResponse.getBody());
            return ResponseEntity.ok(gatewayResponse);

        } catch (HttpStatusCodeException ex) {
            log.error("[Gateway] Xảy ra lỗi từ chuỗi dịch vụ downstream: HttpStatusCode={}, ResponseBody={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("gatewayStatus", "DOWNSTREAM_ERROR");
            errorResponse.put("statusCode", ex.getStatusCode().value());
            errorResponse.put("downstreamError", ex.getResponseBodyAsString());
            return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
        } catch (Exception ex) {
            log.error("[Gateway] Không thể kết nối tới Order Service: {}", ex.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("gatewayStatus", "CONNECTION_FAILED");
            errorResponse.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    /**
     * Endpoint mô phỏng tình huống đề bài:
     * Client gọi API Gateway -> gọi Order Service -> gọi User Service. Một bước ở User Service bị lỗi!
     * GET /api/gateway/demo-failure
     */
    @GetMapping("/demo-failure")
    public ResponseEntity<Map<String, Object>> demoFailure() {
        log.warn("[Gateway] [MÔ PHỎNG SỰ CỐ ĐỀ BÀI] Khởi động chuỗi request sẽ bị lỗi tại User Service...");

        Map<String, Object> request = new HashMap<>();
        request.put("userId", "USER-BLOCKED-8888");
        request.put("amount", 999.0);
        request.put("shouldFail", true);

        return checkout(request);
    }
}
