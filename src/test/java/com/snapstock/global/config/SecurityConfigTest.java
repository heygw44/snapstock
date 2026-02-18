package com.snapstock.global.config;

import com.snapstock.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("/actuator/health는 인증 없이 200 응답한다")
    void actuator_health_인증없이_200() throws Exception {
        // when
        var result = mockMvc.perform(get("/actuator/health"));

        // then
        result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("보호 리소스에 인증 없이 접근하면 401 + ApiResponse Envelope을 반환한다")
    void 보호리소스_인증없이_401_Envelope() throws Exception {
        // when
        var result = mockMvc.perform(get("/api/v1/protected"));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }
}
