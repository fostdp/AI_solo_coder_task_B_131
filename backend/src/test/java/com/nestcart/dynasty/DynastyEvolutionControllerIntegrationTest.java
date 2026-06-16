package com.nestcart.dynasty;

import com.nestcart.dto.DynastyEvolutionResult;
import com.nestcart.entity.DynastyCart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("朝代演变Controller集成测试")
class DynastyEvolutionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DynastyCartRepository dynastyCartRepository;

    @Test
    @DisplayName("GET /api/dynasty/carts 应返回朝代列表")
    void getAllDynastyCarts_shouldReturnList() throws Exception {
        DynastyCart cart = DynastyCart.builder()
                .id(UUID.randomUUID()).dynastyName("春秋").period("公元前770-前476年")
                .maxHeight(12.0).boomLength(8.0).evolutionScore(55.0).sortOrder(1)
                .build();
        when(dynastyCartRepository.findAllByOrderBySortOrderAsc()).thenReturn(Arrays.asList(cart));

        mockMvc.perform(get("/api/dynasty/carts"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].dynastyName").value("春秋"));
    }

    @Test
    @DisplayName("GET /api/dynasty/evolution 应返回分析结果")
    void getEvolutionAnalysis_shouldReturnResult() throws Exception {
        DynastyCart c1 = DynastyCart.builder()
                .id(UUID.randomUUID()).dynastyName("西周").period("西周")
                .maxHeight(8.0).boomLength(5.0).basketWeight(80.0)
                .crewSize(2).evolutionScore(35.0).sortOrder(1).build();
        DynastyCart c2 = DynastyCart.builder()
                .id(UUID.randomUUID()).dynastyName("宋代").period("宋代")
                .maxHeight(22.0).boomLength(14.0).basketWeight(350.0)
                .crewSize(7).evolutionScore(94.0).sortOrder(2).build();
        when(dynastyCartRepository.findAllByOrderBySortOrderAsc()).thenReturn(Arrays.asList(c1, c2));

        mockMvc.perform(get("/api/dynasty/evolution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evolutionPoints").isArray())
                .andExpect(jsonPath("$.evolutionPoints.length()").value(2))
                .andExpect(jsonPath("$.evolutionTrend").isArray())
                .andExpect(jsonPath("$.performanceSummary").isMap())
                .andExpect(jsonPath("$.comparisonTable").isArray());
    }

    @Test
    @DisplayName("GET /api/dynasty/carts/{id} 不存在的ID应返回404")
    void getDynastyCart_notFound_shouldReturn404() throws Exception {
        UUID fakeId = UUID.randomUUID();
        when(dynastyCartRepository.findById(fakeId)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/dynasty/carts/{id}", fakeId))
                .andExpect(status().isNotFound());
    }
}
