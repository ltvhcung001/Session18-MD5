package com.storex.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyUser(@RequestBody Map<String, Object> request) {
        String userId = String.valueOf(request.getOrDefault("userId", "USER-UNKNOWN"));
        String orderId = String.valueOf(request.getOrDefault("orderId", "N/A"));
        boolean shouldFail = Boolean.parseBoolean(String.valueOf(request.getOrDefault("shouldFail", "false")));

        log.info("[UserService] Tiếp nhận yêu cầu xác thực người dùng: userId={}, orderId={}", userId, orderId);

        // Mô phỏng tình huống đề bài: "Một bước ở User Service bị lỗi"
        if (shouldFail || userId.contains("FAIL") || userId.contains("BLOCKED")) {
            log.error("[UserService] GẶP LỖI: Người dùng {} bị khóa tài khoản hoặc vi phạm quy tắc tín dụng!", userId);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("service", "user-service");
            errorResponse.put("userId", userId);
            errorResponse.put("status", "FAILED");
            errorResponse.put("error", "User account is suspended or invalid");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        log.info("[UserService] Xác thực thành công: Người dùng {} đủ điều kiện mua sắm", userId);

        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("service", "user-service");
        successResponse.put("userId", userId);
        successResponse.put("status", "VERIFIED");
        successResponse.put("userType", "GOLD_MEMBER");
        return ResponseEntity.ok(successResponse);
    }
}
