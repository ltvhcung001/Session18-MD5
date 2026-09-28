package com.storex.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controller giả lập Webhook của Discord/Slack khi chạy trên môi trường cục bộ:
 * - Tiếp nhận payload POST từ DiscordAppender.
 * - In trực quan thông báo đỏ mô phỏng giao diện Discord ra màn hình console.
 * - Lưu lịch sử các cảnh báo để phục vụ kiểm thử tự động.
 */
@RestController
@RequestMapping("/mock/discord-webhook")
public class MockDiscordWebhookController {

    private final List<Map<String, Object>> receivedAlerts = Collections.synchronizedList(new ArrayList<>());

    @PostMapping
    public ResponseEntity<Void> receiveDiscordWebhook(@RequestBody Map<String, Object> payload) {
        receivedAlerts.add(payload);

        // In trực quan giao diện thông báo đỏ của Discord ra Console
        System.out.println("\n" +
                "========================================================================================\n" +
                "🔔 [DISCORD BOT RECEIVED WEBHOOK - THÔNG BÁO ĐỎ]\n" +
                "========================================================================================");
        System.out.println("🤖 Bot Name   : " + payload.get("username"));
        if (payload.containsKey("embeds")) {
            List<?> embeds = (List<?>) payload.get("embeds");
            for (Object embedObj : embeds) {
                if (embedObj instanceof Map<?, ?> embed) {
                    System.out.println("🟥 Color Code : " + embed.get("color") + " (Màu đỏ cảnh báo khẩn cấp #E74C3C)");
                    System.out.println("📌 Tiêu đề    : " + embed.get("title"));
                    System.out.println("📝 Mô tả      : " + embed.get("description"));
                    if (embed.containsKey("fields")) {
                        System.out.println("📋 Chi tiết fields:");
                        List<?> fields = (List<?>) embed.get("fields");
                        for (Object f : fields) {
                            if (f instanceof Map<?, ?> fm) {
                                System.out.println("   • " + fm.get("name") + " : " + fm.get("value"));
                            }
                        }
                    }
                    if (embed.containsKey("footer")) {
                        Map<?, ?> footer = (Map<?, ?>) embed.get("footer");
                        System.out.println("🔖 Footer     : " + footer.get("text"));
                    }
                }
            }
        }
        System.out.println("========================================================================================\n");

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getAlertHistory() {
        return ResponseEntity.ok(new ArrayList<>(receivedAlerts));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearHistory() {
        receivedAlerts.clear();
        return ResponseEntity.noContent().build();
    }
}
