package com.storex.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void testActuatorLoggersEndpointAndDynamicLevelChange() throws Exception {
        // 1. Kiểm tra log level ban đầu của package com.storex.payment là INFO
        mockMvc.perform(get("/actuator/loggers/com.storex.payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveLevel", is("INFO")));

        // 2. Gửi request POST đến /actuator/loggers/com.storex.payment để đổi mức log sang DEBUG
        String payload = "{\"configuredLevel\":\"DEBUG\"}";
        mockMvc.perform(post("/actuator/loggers/com.storex.payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNoContent()); // Actuator trả về 204 No Content

        // 3. Kiểm tra lại: effectiveLevel và configuredLevel của com.storex.payment đã là DEBUG
        mockMvc.perform(get("/actuator/loggers/com.storex.payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuredLevel", is("DEBUG")))
                .andExpect(jsonPath("$.effectiveLevel", is("DEBUG")));

        // 4. Gọi endpoint payment để kiểm tra luồng nghiệp vụ hoạt động tốt
        mockMvc.perform(get("/api/payments/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")));

        // 5. Revert lại log level về INFO
        String revertPayload = "{\"configuredLevel\":\"INFO\"}";
        mockMvc.perform(post("/actuator/loggers/com.storex.payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(revertPayload))
                .andExpect(status().isNoContent());

        // Kiểm tra sau khi revert
        mockMvc.perform(get("/actuator/loggers/com.storex.payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveLevel", is("INFO")));
    }
}
