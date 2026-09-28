package com.storex.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service mô phỏng phát sinh tải log dung lượng lớn
 * và theo dõi trạng thái các file trong thư mục logs/ phục vụ kiểm chứng Log Rotation.
 */
@Service
public class LogRotationService {

    private static final Logger log = LoggerFactory.getLogger(LogRotationService.class);
    private static final String LOG_DIR = "logs";

    /**
     * Sinh log hàng loạt với số lượng dòng chỉ định
     */
    public Map<String, Object> generateBulkLogs(int count, String messagePrefix) {
        long startTime = System.currentTimeMillis();
        String prefix = (messagePrefix != null && !messagePrefix.isBlank()) ? messagePrefix : "BulkTransaction";

        for (int i = 1; i <= count; i++) {
            log.info("[BULK_LOG #{}] {} - Payload dữ liệu mô phỏng giao dịch thương mại điện tử với nhiều trường thông tin: orderId=ORD-BULK-{}, amount={}, gateway=VNPay, status=PROCESSED",
                    i, prefix, i, (i * 12.5));
        }

        long duration = System.currentTimeMillis() - startTime;
        Map<String, Object> result = new HashMap<>();
        result.put("generatedLines", count);
        result.put("timeTakenMs", duration);
        result.put("status", "SUCCESS");
        result.put("logDirectoryStatus", getLogDirectoryStatus());
        return result;
    }

    /**
     * Lấy thông tin thống kê các file log trong thư mục logs/
     */
    public Map<String, Object> getLogDirectoryStatus() {
        Map<String, Object> status = new HashMap<>();
        Path logPath = Paths.get(LOG_DIR);

        if (!Files.exists(logPath)) {
            status.put("exists", false);
            status.put("files", Collections.emptyList());
            status.put("totalSizeBytes", 0L);
            return status;
        }

        try (Stream<Path> stream = Files.list(logPath)) {
            List<Map<String, Object>> fileDetails = stream
                    .filter(Files::isRegularFile)
                    .map(p -> {
                        Map<String, Object> detail = new HashMap<>();
                        detail.put("fileName", p.getFileName().toString());
                        try {
                            detail.put("sizeBytes", Files.size(p));
                            detail.put("sizeFormatted", String.format("%.2f KB", Files.size(p) / 1024.0));
                        } catch (IOException e) {
                            detail.put("sizeBytes", -1L);
                        }
                        detail.put("isRotatedArchive", p.getFileName().toString().matches("application-\\d{4}-\\d{2}-\\d{2}\\.\\d+\\.log"));
                        return detail;
                    })
                    .sorted((a, b) -> String.valueOf(a.get("fileName")).compareTo(String.valueOf(b.get("fileName"))))
                    .collect(Collectors.toList());

            long totalSize = fileDetails.stream()
                    .mapToLong(d -> (Long) d.getOrDefault("sizeBytes", 0L))
                    .filter(s -> s > 0)
                    .sum();

            status.put("exists", true);
            status.put("totalFiles", fileDetails.size());
            status.put("totalSizeBytes", totalSize);
            status.put("totalSizeFormatted", String.format("%.2f KB", totalSize / 1024.0));
            status.put("files", fileDetails);

        } catch (IOException e) {
            status.put("error", e.getMessage());
        }

        return status;
    }
}
