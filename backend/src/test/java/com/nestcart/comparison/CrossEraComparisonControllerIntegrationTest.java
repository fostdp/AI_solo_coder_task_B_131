package com.nestcart.comparison;

import com.nestcart.entity.ModernDroneSpec;
import com.nestcart.entity.NestCart;
import com.nestcart.repository.ModernDroneSpecRepository;
import com.nestcart.repository.NestCartRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("跨时代对比Controller集成测试")
class CrossEraComparisonControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModernDroneSpecRepository modernDroneSpecRepository;

    @MockBean
    private NestCartRepository nestCartRepository;

    @Test
    @DisplayName("GET /api/comparison/drones 应返回无人机列表")
    void getAllDrones_shouldReturnList() throws Exception {
        ModernDroneSpec drone = ModernDroneSpec.builder()
                .id(UUID.randomUUID()).modelName("Mavic 3 Enterprise")
                .manufacturer("DJI").sortOrder(1).build();
        when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc())
                .thenReturn(Collections.singletonList(drone));

        mockMvc.perform(get("/api/comparison/drones"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].modelName").value("Mavic 3 Enterprise"));
    }

    @Test
    @DisplayName("GET /api/comparison/cross-era 应返回9个对比维度")
    void compareCrossEra_shouldReturn9Dimensions() throws Exception {
        NestCart cart = NestCart.builder()
                .id(UUID.randomUUID()).name("巢车一号").maxHeight(15.0)
                .boomLength(8.0).basketWeight(150.0).crewCapacity(3)
                .boomCrossSectionArea(0.01).boomMomentOfInertia(8.33e-6)
                .boomElasticModulus(1.2e10).baseHeight(4.0)
                .stressLimit(8e6).swayLimit(0.5).build();
        ModernDroneSpec drone = ModernDroneSpec.builder()
                .id(UUID.randomUUID()).modelName("Test Drone").manufacturer("Test")
                .maxFlightAltitudeMeters(6000.0).surveillanceRadiusKm(10.0)
                .flightEnduranceMinutes(45.0).setupTimeMinutes(5.0)
                .crewRequired(1).unitCostUsd(5000.0).sortOrder(1).build();

        when(nestCartRepository.findAll()).thenReturn(Collections.singletonList(cart));
        when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc())
                .thenReturn(Collections.singletonList(drone));

        mockMvc.perform(get("/api/comparison/cross-era"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparisonDimensions").isArray())
                .andExpect(jsonPath("$.comparisonDimensions.length()").value(9))
                .andExpect(jsonPath("$.insights").isArray())
                .andExpect(jsonPath("$.ancientSummary").isNotEmpty())
                .andExpect(jsonPath("$.modernSummary").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/comparison/vs-drone 缺少参数应返回400")
    void compareCartVsDrone_missingParams_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/comparison/vs-drone"))
                .andExpect(status().isBadRequest());
    }
}
