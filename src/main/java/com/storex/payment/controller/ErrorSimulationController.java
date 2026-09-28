package com.storex.payment.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/errors")
public class ErrorSimulationController {

    private static final Logger log = LoggerFactory.getLogger(ErrorSimulationController.class);

    /**
     * Endpoint mô phỏng trực tiếp theo yêu cầu đề bài:
     * Chạy lệnh log.error("Lỗi DB") để kích hoạt DiscordAppender bắn cảnh báo đỏ.
     * GET /api/errors/simulate-db-error
     */
    @GetMapping("/simulate-db-error")
    public ResponseEntity<Map<String, Object>> simulateDbError() {
        log.info("Bắt đầu thực thi truy vấn nghiệp vụ...");
        
        // KÍCH HOẠT LỆNH YÊU CẦU CỦA ĐỀ BÀI:
        log.error("Lỗi DB: Không thể kết nối tới cơ sở dữ liệu PostgreSQL (Connection timeout sau 30000ms)");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "ERROR_SIMULATED");
        response.put("message", "Đã kích hoạt log.error('Lỗi DB'). Kiểm tra kênh Discord hoặc Mock Webhook để xem thông báo đỏ.");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint mô phỏng lỗi kèm Exception StackTrace thực tế
     * GET /api/errors/simulate-exception
     */
    @GetMapping("/simulate-exception")
    public ResponseEntity<Map<String, Object>> simulateException() {
        try {
            throw new SQLException("HikariPool-1 - Connection is not available, request timed out after 30000ms");
        } catch (SQLException ex) {
            log.error("Lỗi DB: Gặp ngoại lệ khi truy vấn bảng payment_transactions", ex);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "EXCEPTION_LOGGED");
        response.put("message", "Đã ghi nhận ngoại lệ cơ sở dữ liệu và gửi cảnh báo đỏ lên Discord.");
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint tùy biến nội dung log ERROR từ người dùng
     * POST /api/errors/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerCustomError(@RequestBody(required = false) Map<String, String> body) {
        String message = (body != null && body.containsKey("message")) 
                ? body.get("message") 
                : "Lỗi DB: Đơn hàng bị khóa do deadlock giao dịch";
        
        log.error(message);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "TRIGGERED");
        response.put("loggedMessage", message);
        return ResponseEntity.ok(response);
    }
}
