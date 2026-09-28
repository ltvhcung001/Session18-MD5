package com.storex.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public Map<String, Object> processPayment(String orderId, double amount, String customerId, String gateway) {
        // Log mức INFO: Luôn xuất hiện khi ở mức cấu hình mặc định (INFO)
        log.info("[INFO] Nhận yêu cầu xử lý thanh toán cho đơn hàng: orderId={}, amount={} USD", orderId, amount);

        // Các log mức DEBUG: Chỉ xuất hiện sau khi đổi log level sang DEBUG qua Actuator
        log.debug("[DEBUG] Bắt đầu xác thực thông tin khách hàng: customerId={}", customerId);
        log.debug("[DEBUG] Kiểm tra số dư và hạn mức thanh toán với cổng: gateway={}", gateway);
        
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.debug("[DEBUG] Sinh mã giao dịch nội bộ: transactionId={}", transactionId);
        log.debug("[DEBUG] Đang kết nối tới cổng thanh toán đối tác {} qua giao thức bảo mật...", gateway);

        // Kết quả xử lý
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("transactionId", transactionId);
        response.put("amount", amount);
        response.put("customerId", customerId);
        response.put("gateway", gateway);
        response.put("status", "SUCCESS");
        response.put("timestamp", System.currentTimeMillis());

        log.info("[INFO] Xử lý thanh toán thành công cho đơn hàng: orderId={}, transactionId={}", orderId, transactionId);

        return response;
    }
}
