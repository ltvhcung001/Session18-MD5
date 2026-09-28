package com.storex.payment.controller;

import com.storex.payment.service.LogRotationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogRotationController {

    private final LogRotationService logRotationService;

    public LogRotationController(LogRotationService logRotationService) {
        this.logRotationService = logRotationService;
    }

    /**
     * Endpoint sinh log hàng loạt mô phỏng tải cao:
     * POST /api/logs/generate-bulk?count=500
     */
    @PostMapping("/generate-bulk")
    public ResponseEntity<Map<String, Object>> generateBulk(@RequestParam(defaultValue = "500") int count,
                                                            @RequestParam(defaultValue = "ProductionSim") String prefix) {
        Map<String, Object> result = logRotationService.generateBulkLogs(count, prefix);
        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint kiểm tra trạng thái và danh sách file log trong thư mục logs/:
     * GET /api/logs/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = logRotationService.getLogDirectoryStatus();
        return ResponseEntity.ok(status);
    }
}
