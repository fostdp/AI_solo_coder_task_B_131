package com.nestcart.collaborative;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("协同覆盖Controller集成测试")
class CollaborativeCoverageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/collaborative/coverage 应返回覆盖优化结果")
    void getCoverage_shouldReturnResult() throws Exception {
        mockMvc.perform(get("/api/collaborative/coverage")
                        .param("region", "test")
                        .param("cartCount", "2")
                        .param("widthKm", "10")
                        .param("heightKm", "10")
                        .param("strategy", "greedy_spread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageRatio").isNumber())
                .andExpect(jsonPath("$.cartCount").value(2))
                .andExpect(jsonPath("$.placements").isArray())
                .andExpect(jsonPath("$.optimizationMetrics").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/collaborative/optimize 应接受JSON请求体")
    void optimizeCoverage_shouldAcceptJsonBody() throws Exception {
        String json = """
                {
                  "region": "test_battlefield",
                  "regionWidthKm": 10,
                  "regionHeightKm": 10,
                  "strategy": "greedy_spread",
                  "maxIterations": 50,
                  "carts": [
                    {
                      "cartName": "巢车-A",
                      "x": 3,
                      "y": 3,
                      "height": 12,
                      "movable": true
                    },
                    {
                      "cartName": "巢车-B",
                      "x": 7,
                      "y": 7,
                      "height": 15,
                      "movable": true
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/collaborative/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageRatio").isNumber())
                .andExpect(jsonPath("$.placements.length()").value(2))
                .andExpect(jsonPath("$.coverageHeatmap").isArray())
                .andExpect(jsonPath("$.blindZones").isArray());
    }

    @Test
    @DisplayName("POST /api/collaborative/optimize 空请求体应使用默认值")
    void optimizeCoverage_emptyBody_shouldUseDefaults() throws Exception {
        String json = "{}";

        mockMvc.perform(post("/api/collaborative/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartCount").value(2));
    }
}
