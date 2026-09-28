package com.storex.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testVerifyUserSuccess() throws Exception {
        String payload = "{\"userId\":\"USER-101\",\"orderId\":\"ORD-101\",\"shouldFail\":false}";
        mockMvc.perform(post("/api/users/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    @Test
    void testVerifyUserFailure() throws Exception {
        String payload = "{\"userId\":\"USER-BLOCKED\",\"orderId\":\"ORD-102\",\"shouldFail\":true}";
        mockMvc.perform(post("/api/users/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }
}
