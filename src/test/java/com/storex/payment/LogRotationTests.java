package com.storex.payment;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LogRotationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testRollingFileAppenderConfiguration() throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        // 1. Kiểm tra Appender FILE đã được cấu hình và là RollingFileAppender
        Appender<ILoggingEvent> fileAppender = rootLogger.getAppender("FILE");
        assertThat(fileAppender)
                .as("Appender FILE phải là thể hiện của RollingFileAppender để tránh lỗi tràn ổ đĩa")
                .isNotNull()
                .isInstanceOf(RollingFileAppender.class);

        RollingFileAppender<ILoggingEvent> rollingAppender = (RollingFileAppender<ILoggingEvent>) fileAppender;

        // 2. Kiểm tra tên file log hiện hành
        assertThat(rollingAppender.getFile()).isEqualTo("logs/application.log");

        // 3. Kiểm tra Chính sách xoay vòng là SizeAndTimeBasedRollingPolicy
        assertThat(rollingAppender.getRollingPolicy())
                .as("Chính sách xoay vòng phải là SizeAndTimeBasedRollingPolicy kết hợp thời gian và kích thước")
                .isNotNull()
                .isInstanceOf(SizeAndTimeBasedRollingPolicy.class);

        SizeAndTimeBasedRollingPolicy<?> policy = (SizeAndTimeBasedRollingPolicy<?>) rollingAppender.getRollingPolicy();

        // 4. Kiểm tra mẫu tên file xoay vòng theo ngày và chỉ số index (%i)
        assertThat(policy.getFileNamePattern())
                .as("Mẫu tên file phải chứa %d{yyyy-MM-dd} và %i để phân tách các file khi đạt ngưỡng")
                .isEqualTo("logs/application-%d{yyyy-MM-dd}.%i.log");

        // 5. Kiểm tra thời gian lưu trữ tối đa là 30 ngày theo yêu cầu đề bài
        assertThat(policy.getMaxHistory())
                .as("Thời gian lưu trữ tối đa phải là 30 ngày")
                .isEqualTo(30);

        // 6. Kiểm tra giới hạn kích thước mỗi file là 10MB theo yêu cầu đề bài
        Field maxFileSizeField = SizeAndTimeBasedRollingPolicy.class.getDeclaredField("maxFileSize");
        maxFileSizeField.setAccessible(true);
        Object maxFileSize = maxFileSizeField.get(policy);
        assertThat(maxFileSize).isNotNull();
        assertThat(maxFileSize.toString())
                .as("Giới hạn kích thước mỗi file phải là 10MB")
                .contains("10");
    }

    @Test
    void testGenerateBulkLogsAndStatusEndpoints() throws Exception {
        // Kiểm thử gọi API sinh log hàng loạt
        mockMvc.perform(post("/api/logs/generate-bulk")
                        .param("count", "50")
                        .param("prefix", "TestBatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.generatedLines").value(50));

        // Kiểm thử API kiểm tra trạng thái thư mục logs
        mockMvc.perform(get("/api/logs/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.totalFiles").isNumber());
    }
}
