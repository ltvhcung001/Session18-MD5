package com.storex.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Xử lý đặt hàng mô phỏng theo tình huống nghiệp vụ:
     * Toàn bộ các dòng log từ khi nhận đơn, kiểm tra giỏ hàng, gọi thanh toán
     * đến khi thành công hoặc thất bại đều mang cùng một requestId (UUID) từ MDC.
     */
    public Map<String, Object> createOrder(String userId, String orderId, double amount, boolean shouldFail) {
        log.info("[BẮT ĐẦU] Tiếp nhận yêu cầu đặt hàng từ userId={}, orderId={}, tổng tiền={} USD", userId, orderId, amount);

        log.debug("Đang kiểm tra tồn kho các sản phẩm trong đơn hàng: orderId={}", orderId);
        log.debug("Áp dụng mã khuyến mãi Voucher giảm 10% thành công");

        if (shouldFail) {
            // Tình huống đề bài: Giao dịch thất bại (Transaction Failed)
            log.warn("Cổng thanh toán báo lỗi: Số dư tài khoản không đủ hoặc thẻ bị khóa!");
            log.error("Transaction Failed: Giao dịch thanh toán thất bại cho đơn hàng orderId={}, userId={}", orderId, userId);

            Map<String, Object> failResult = new HashMap<>();
            failResult.put("orderId", orderId);
            failResult.put("userId", userId);
            failResult.put("status", "FAILED");
            failResult.put("message", "Transaction Failed: Payment declined by bank");
            return failResult;
        }

        // Gọi sang PaymentService cùng nằm trong cùng một thread và request context
        log.info("Gọi sang PaymentService để tiến hành thanh toán trực tuyến...");
        Map<String, Object> paymentResult = paymentService.processPayment(orderId, amount, userId, "VNPay");

        log.info("Transaction Success: Đơn hàng orderId={} đã hoàn tất thành công với giao dịch {}", 
                orderId, paymentResult.get("transactionId"));

        Map<String, Object> successResult = new HashMap<>();
        successResult.put("orderId", orderId);
        successResult.put("userId", userId);
        successResult.put("status", "SUCCESS");
        successResult.put("payment", paymentResult);
        return successResult;
    }
}
