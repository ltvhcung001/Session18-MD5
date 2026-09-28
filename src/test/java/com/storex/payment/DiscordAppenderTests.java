package com.storex.payment;

import com.storex.payment.appender.DiscordAppender;
import com.storex.payment.controller.MockDiscordWebhookController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DiscordAppenderTests {

    private static final Logger log = LoggerFactory.getLogger(DiscordAppenderTests.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockDiscordWebhookController mockDiscordWebhookController;

    @BeforeEach
    void setUp() {
        mockDiscordWebhookController.clearHistory();
    }

    @Test
    void testSimulateDbErrorEndpoint() throws Exception {
        // Gọi endpoint mô phỏng lỗi DB
        mockMvc.perform(get("/api/errors/simulate-db-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR_SIMULATED"));
    }

    @Test
    void testSimulateExceptionEndpoint() throws Exception {
        // Gọi endpoint mô phỏng Exception thực tế
        mockMvc.perform(get("/api/errors/simulate-exception"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXCEPTION_LOGGED"));
    }

    @Test
    void testMockDiscordWebhookReceivesRedEmbedPayload() throws Exception {
        // Gửi thử một payload Discord Embed mô phỏng
        String discordPayload = "{" +
                "\"username\": \"StoreX Alert Bot\"," +
                "\"embeds\": [{" +
                    "\"title\": \"🚨 [ALERT] LỖI HỆ THỐNG PHÁT SINH\"," +
                    "\"description\": \"**Chi tiết:** Lỗi DB\"," +
                    "\"color\": 15158332," +
                    "\"fields\": [" +
                        "{\"name\": \"Logger\", \"value\": \"com.storex.payment.service.OrderService\"}," +
                        "{\"name\": \"Request ID (MDC)\", \"value\": \"test-uuid-12345\"}" +
                    "]" +
                "}]" +
                "}";

        mockMvc.perform(post("/mock/discord-webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(discordPayload))
                .andExpect(status().isNoContent());

        List<Map<String, Object>> history = mockDiscordWebhookController.getAlertHistory().getBody();
        assertThat(history).isNotNull().hasSize(1);

        Map<String, Object> firstAlert = history.get(0);
        assertThat(firstAlert.get("username")).isEqualTo("StoreX Alert Bot");

        List<?> embeds = (List<?>) firstAlert.get("embeds");
        assertThat(embeds).isNotEmpty();

        Map<?, ?> embed = (Map<?, ?>) embeds.get(0);
        // Kiểm tra mã màu đỏ chuẩn của Discord: 15158332
        assertThat(embed.get("color")).isEqualTo(15158332);
        assertThat(embed.get("title")).isEqualTo("🚨 [ALERT] LỖI HỆ THỐNG PHÁT SINH");
        assertThat(embed.get("description")).isEqualTo("**Chi tiết:** Lỗi DB");
    }
}
