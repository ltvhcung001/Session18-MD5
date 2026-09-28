package com.storex.payment.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter chặn mọi HTTP request đi vào hệ thống:
 * - Trích xuất hoặc sinh mã requestId duy nhất dạng UUID.
 * - Lưu requestId vào MDC (Mapped Diagnostic Context) của SLF4J/Logback.
 * - Đính kèm header X-Request-Id vào HTTP Response để client/gateway dễ đối soát.
 * - Dọn dẹp MDC trong khối finally nhằm tránh rò rỉ bộ nhớ (ThreadLocal leak).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_KEY = "requestId";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Kiểm tra xem client/API Gateway có truyền sẵn X-Request-Id hay không
        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        // Đưa requestId vào Mapped Diagnostic Context (MDC)
        MDC.put(REQUEST_ID_KEY, requestId);

        // Trả về header X-Request-Id cho phía client
        response.setHeader(HEADER_REQUEST_ID, requestId);

        try {
            // Chuyển tiếp request vào filter chain và controller
            filterChain.doFilter(request, response);
        } finally {
            // QUAN TRỌNG: Phải luôn xóa requestId khỏi MDC sau khi xử lý xong request
            // để tránh rò rỉ dữ liệu khi luồng (Thread) được trả về ThreadPool và tái sử dụng
            MDC.remove(REQUEST_ID_KEY);
        }
    }
}
