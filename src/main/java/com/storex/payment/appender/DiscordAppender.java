package com.storex.payment.appender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Custom Logback Appender: Tự động bắt các log mức ERROR và bắn Webhook thông báo tới Discord / Slack.
 * - Hỗ trợ định dạng Rich Embed của Discord với mã màu đỏ (#E74C3C / 15158332).
 * - Đính kèm thông tin: Logger, Thread, RequestId (MDC từ HW2), Exception StackTrace tóm tắt.
 * - Sử dụng HttpClient gửi bất đồng bộ (Non-blocking Asynchronous) tránh làm chậm luồng nghiệp vụ chính.
 * - Cơ chế chống đệ quy (Loop Protection): Không bao giờ gọi Logger trong appender để tránh vòng lặp vô tận.
 */
public class DiscordAppender extends AppenderBase<ILoggingEvent> {

    // Cấu hình từ logback-spring.xml
    private String webhookUrl;
    private boolean enabled = true;
    private String username = "StoreX Alert Bot";
    private String avatarUrl = "https://cdn-icons-png.flaticon.com/512/564/564619.png";

    // Khởi tạo HttpClient với kết nối timeout 3s
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    protected void append(ILoggingEvent eventObject) {
        // Chỉ xử lý khi appender đã bật, cấu hình webhook hợp lệ và log ở mức ERROR
        if (!isStarted() || !enabled || webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        if (eventObject.getLevel().levelInt == Level.ERROR_INT) {
            sendDiscordAlert(eventObject);
        }
    }

    private void sendDiscordAlert(ILoggingEvent eventObject) {
        try {
            String message = eventObject.getFormattedMessage();
            String loggerName = eventObject.getLoggerName();
            String threadName = eventObject.getThreadName();

            // Trích xuất requestId từ MDC (kế thừa từ bài tập 2)
            Map<String, String> mdcMap = eventObject.getMDCPropertyMap();
            String requestId = (mdcMap != null && mdcMap.containsKey("requestId")) 
                    ? mdcMap.get("requestId") 
                    : "N/A";

            // Trích xuất thông tin lỗi Exception nếu có
            String exceptionDetails = "";
            IThrowableProxy throwableProxy = eventObject.getThrowableProxy();
            if (throwableProxy != null) {
                exceptionDetails = throwableProxy.getClassName() + ": " + throwableProxy.getMessage();
                StackTraceElementProxy[] stackTrace = throwableProxy.getStackTraceElementProxyArray();
                if (stackTrace != null && stackTrace.length > 0) {
                    exceptionDetails += "\\n  at " + stackTrace[0].getSTEAsString();
                }
            }

            // Xây dựng JSON Payload theo chuẩn Discord Webhook Embeds
            // Mã màu đỏ: 15158332 (tương đương HEX #E74C3C)
            String jsonPayload = String.format(
                    "{" +
                    "\"username\":\"%s\"," +
                    "\"avatar_url\":\"%s\"," +
                    "\"embeds\":[{" +
                        "\"title\":\"🚨 [ALERT] LỖI HỆ THỐNG PHÁT SINH\"," +
                        "\"description\":\"**Chi tiết:** %s\"," +
                        "\"color\":15158332," +
                        "\"fields\":[" +
                            "{\"name\":\"Logger\",\"value\":\"`%s`\",\"inline\":false}," +
                            "{\"name\":\"Request ID (MDC)\",\"value\":\"`%s`\",\"inline\":true}," +
                            "{\"name\":\"Thread\",\"value\":\"`%s`\",\"inline\":true}," +
                            (exceptionDetails.isBlank() ? "" : "{\"name\":\"Exception\",\"value\":\"`" + escapeJson(exceptionDetails) + "`\",\"inline\":false},") +
                            "{\"name\":\"Thời gian\",\"value\":\"%s\",\"inline\":false}" +
                        "]," +
                        "\"footer\":{\"text\":\"StoreX Monitoring System - Zero Downtime Alerts\"}" +
                    "}]" +
                    "}",
                    escapeJson(username),
                    escapeJson(avatarUrl),
                    escapeJson(message),
                    escapeJson(loggerName),
                    escapeJson(requestId),
                    escapeJson(threadName),
                    Instant.now().toString()
            );

            // Xây dựng HTTP Request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            // Gửi HTTP POST bất đồng bộ qua sendAsync
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 400) {
                            addWarn("[DiscordAppender] Discord Webhook trả về mã lỗi: " + response.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        // CHÚ Ý: Tuyệt đối không gọi log.error ở đây để tránh đệ quy vô tận
                        addError("[DiscordAppender] Không thể gửi cảnh báo tới Webhook: " + ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            addError("[DiscordAppender] Lỗi khi tạo hoặc gửi payload Webhook: " + e.getMessage());
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    // Getters and Setters phục vụ cấu hình từ logback-spring.xml
    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
