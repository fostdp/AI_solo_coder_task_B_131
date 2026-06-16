package com.nestcart.modules.era_comparator;

import com.nestcart.entity.NestCart;
import com.nestcart.modules.era_comparator.dto.CrossEraComparisonResult;
import com.nestcart.modules.era_comparator.entity.ModernDroneSpec;
import com.nestcart.modules.era_comparator.repository.ModernDroneSpecRepository;
import com.nestcart.modules.era_comparator.service.CrossEraComparisonService;
import com.nestcart.repository.NestCartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("跨时代对比服务测试")
class CrossEraComparisonServiceTest {

    @Mock
    private ModernDroneSpecRepository modernDroneSpecRepository;

    @Mock
    private NestCartRepository nestCartRepository;

    @InjectMocks
    private CrossEraComparisonService crossEraComparisonService;

    private List<NestCart> testCarts;
    private List<ModernDroneSpec> testDrones;

    @BeforeEach
    void setUp() {
        testCarts = new ArrayList<>();
        testCarts.add(NestCart.builder()
                .id(UUID.randomUUID()).name("巢车一号")
                .maxHeight(15.0).boomLength(8.0).basketWeight(150.0)
                .crewCapacity(3).boomCrossSectionArea(0.01)
                .boomMomentOfInertia(8.33e-6).boomElasticModulus(1.2e10)
                .baseHeight(4.0).stressLimit(8e6).swayLimit(0.5)
                .build());
        testCarts.add(NestCart.builder()
                .id(UUID.randomUUID()).name("巢车二号")
                .maxHeight(18.0).boomLength(10.0).basketWeight(200.0)
                .crewCapacity(4).boomCrossSectionArea(0.015)
                .boomMomentOfInertia(1.25e-5).boomElasticModulus(1.2e10)
                .baseHeight(5.0).stressLimit(1e7).swayLimit(0.4)
                .build());

        testDrones = new ArrayList<>();
        testDrones.add(ModernDroneSpec.builder()
                .id(UUID.randomUUID()).modelName("Mavic 3 Enterprise").manufacturer("DJI")
                .category("消费级多旋翼").yearIntroduced(2021)
                .maxFlightAltitudeMeters(6000.0).maxCeilingMeters(6000.0)
                .maxFlightRangeKm(30.0).flightEnduranceMinutes(45.0)
                .cruiseSpeedKmh(50.0).maxSpeedKmh(75.0)
                .cameraResolutionMp(20.0).opticalZoom(7.0)
                .thermalCamera(true).surveillanceRadiusKm(10.0)
                .dataLinkRangeKm(15.0).payloadCapacityKg(0.5)
                .takeoffWeightKg(1.0).unitCostUsd(4500.0)
                .operatingCostPerHourUsd(20.0).crewRequired(1)
                .setupTimeMinutes(5.0).noiseLevelDb(72.0)
                .stealthRating(65.0).weatherResistance("IP44")
                .maxWindResistanceMetersPerSec(12.0).sortOrder(1)
                .build());
        testDrones.add(ModernDroneSpec.builder()
                .id(UUID.randomUUID()).modelName("MQ-9 Reaper").manufacturer("General Atomics")
                .category("长航时中空无人机").yearIntroduced(2007)
                .maxFlightAltitudeMeters(15240.0).maxCeilingMeters(15240.0)
                .maxFlightRangeKm(1850.0).flightEnduranceMinutes(1740.0)
                .cruiseSpeedKmh(313.0).maxSpeedKmh(482.0)
                .cameraResolutionMp(1.8).opticalZoom(30.0)
                .thermalCamera(true).surveillanceRadiusKm(300.0)
                .dataLinkRangeKm(1850.0).payloadCapacityKg(1700.0)
                .takeoffWeightKg(4760.0).unitCostUsd(32000000.0)
                .operatingCostPerHourUsd(3500.0).crewRequired(5)
                .setupTimeMinutes(600.0).noiseLevelDb(95.0)
                .stealthRating(30.0).weatherResistance("全天候")
                .maxWindResistanceMetersPerSec(15.0).sortOrder(2)
                .build());
    }

    @Nested
    @DisplayName("正常场景测试")
    class NormalScenarioTests {

        @Test
        @DisplayName("跨时代对比应返回9个对比维度")
        void compareCrossEra_shouldReturn9Dimensions() {
            when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc()).thenReturn(testDrones);
            when(nestCartRepository.findAll()).thenReturn(testCarts);

            CrossEraComparisonResult result = crossEraComparisonService.compareCrossEra();

            assertNotNull(result);
            assertEquals(9, result.getComparisonDimensions().size());
        }

        @Test
        @DisplayName("无人机在升空高度维度上应明显领先")
        void compareCrossEra_droneShouldLeadInAltitudeDimension() {
            when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc()).thenReturn(testDrones);
            when(nestCartRepository.findAll()).thenReturn(testCarts);

            CrossEraComparisonResult result = crossEraComparisonService.compareCrossEra();

            Optional<CrossEraComparisonResult.ComparisonDimension> heightDim =
                    result.getComparisonDimensions().stream()
                            .filter(d -> "最大升空高度".equals(d.getDimension()))
                            .findFirst();

            assertTrue(heightDim.isPresent());
            assertTrue(heightDim.get().getModernScore() > heightDim.get().getAncientScore(),
                    "无人机升空高度得分应高于巢车");
            assertEquals("现代无人机明显领先", heightDim.get().getAdvantage());
        }

        @Test
        @DisplayName("巢车在隐蔽性维度上应占优")
        void compareCrossEra_cartShouldHaveStealthAdvantage() {
            when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc()).thenReturn(testDrones);
            when(nestCartRepository.findAll()).thenReturn(testCarts);

            CrossEraComparisonResult result = crossEraComparisonService.compareCrossEra();

            Optional<CrossEraComparisonResult.ComparisonDimension> stealthDim =
                    result.getComparisonDimensions().stream()
                            .filter(d -> "隐蔽性".equals(d.getDimension()))
                            .findFirst();

            assertTrue(stealthDim.isPresent());
            assertTrue(stealthDim.get().getAncientScore() >= stealthDim.get().getModernScore(),
                    "巢车隐蔽性得分应≥无人机");
        }

        @Test
        @DisplayName("无人机侦察半径应远超巢车理论视距")
        void compareCrossEra_droneRangeShouldExceedCartHorizon() {
            when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc()).thenReturn(testDrones);
            when(nestCartRepository.findAll()).thenReturn(testCarts);

            CrossEraComparisonResult result = crossEraComparisonService.compareCrossEra();

            Optional<CrossEraComparisonResult.ComparisonDimension> rangeDim =
                    result.getComparisonDimensions().stream()
                            .filter(d -> "侦察半径".equals(d.getDimension()))
                            .findFirst();

            assertTrue(rangeDim.isPresent());
            assertTrue(rangeDim.get().getModernValue() > rangeDim.get().getAncientValue(),
                    String.format("无人机侦察半径(%.1fkm)应>巢车(%.1fkm)",
                            rangeDim.get().getModernValue(), rangeDim.get().getAncientValue()));
        }

        @Test
        @DisplayName("一对一对比应使用指定的巢车和无人机")
        void compareCartVsDrone_shouldUseSpecificCartAndDrone() {
            NestCart cart = testCarts.get(0);
            ModernDroneSpec drone = testDrones.get(0);
            when(nestCartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));
            when(modernDroneSpecRepository.findById(drone.getId())).thenReturn(Optional.of(drone));

            CrossEraComparisonResult result = crossEraComparisonService.compareCartVsDrone(cart.getId(), drone.getId());

            assertEquals(cart.getName(), result.getAncientSummary().getPlatformName());
            assertEquals(drone.getModelName(), result.getModernSummary().getPlatformName());
        }

        @Test
        @DisplayName("洞察应包含升空能力倍数提升信息")
        void compareCrossEra_insightsShouldContainAltitudeMultiple() {
            when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc()).thenReturn(testDrones);
            when(nestCartRepository.findAll()).thenReturn(testCarts);

            CrossEraComparisonResult result = crossEraComparisonService.compareCrossEra();

            assertTrue(result.getInsights().stream()
                    .anyMatch(i -> i.contains("升空能力提升") && i.contains("倍")));
        }

        @Test
        @DisplayName("古代视野范围能力评分应为25分")
        void compareCrossEra_ancientVisionScoreShouldBe25() {
            when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc()).thenReturn(testDrones);
            when(nestCartRepository.findAll()).thenReturn(testCarts);

            CrossEraComparisonResult result = crossEraComparisonService.compareCrossEra();

            assertEquals(25.0, result.getAncientSummary().getCapabilityScores().get("视野范围"));
            assertEquals(95.0, result.getModernSummary().getCapabilityScores().get("视野范围"));
        }
    }

    @Nested
    @DisplayName("边界场景测试")
    class BoundaryScenarioTests {

        @Test
        @DisplayName("巢车高度为0时视距应为0km")
        void compareCartVsDrone_zeroHeightCart_visionDistanceShouldBeZero() {
            NestCart zeroCart = NestCart.builder()
                    .id(UUID.randomUUID()).name("零高度巢车").maxHeight(0.0)
                    .boomLength(5.0).basketWeight(50.0).crewCapacity(1)
                    .boomCrossSectionArea(0.01).boomMomentOfInertia(1e-6)
                    .boomElasticModulus(1e10).baseHeight(0.0)
                    .stressLimit(1e6).swayLimit(1.0).build();
            when(nestCartRepository.findById(zeroCart.getId())).thenReturn(Optional.of(zeroCart));
            when(modernDroneSpecRepository.findById(testDrones.get(0).getId()))
                    .thenReturn(Optional.of(testDrones.get(0)));

            CrossEraComparisonResult result = crossEraComparisonService.compareCartVsDrone(
                    zeroCart.getId(), testDrones.get(0).getId());

            assertEquals(0.0, result.getAncientSummary().getMaxRangeKm());
        }

        @Test
        @DisplayName("无人机setupTime为null时应使用默认值且不抛异常")
        void compareCrossEra_nullSetupTime_shouldNotThrow() {
            ModernDroneSpec noSetupDrone = ModernDroneSpec.builder()
                    .id(UUID.randomUUID()).modelName("测试无人机").manufacturer("Test")
                    .setupTimeMinutes(null).maxFlightAltitudeMeters(100.0)
                    .surveillanceRadiusKm(5.0).flightEnduranceMinutes(30.0)
                    .crewRequired(1).unitCostUsd(1000.0).sortOrder(1)
                    .build();
            when(nestCartRepository.findAll()).thenReturn(testCarts);
            when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc())
                    .thenReturn(Collections.singletonList(noSetupDrone));

            assertDoesNotThrow(() -> crossEraComparisonService.compareCrossEra());
        }

        @Test
        @DisplayName("极高巢车视距应符合地平线公式计算")
        void compareCrossEra_veryHighCart_visionDistanceShouldMatchFormula() {
            NestCart highCart = NestCart.builder()
                    .id(UUID.randomUUID()).name("超高巢车").maxHeight(1000.0)
                    .boomLength(50.0).basketWeight(500.0).crewCapacity(10)
                    .boomCrossSectionArea(0.1).boomMomentOfInertia(1e-4)
                    .boomElasticModulus(1e10).baseHeight(100.0)
                    .stressLimit(1e8).swayLimit(0.1).build();
            when(nestCartRepository.findAll()).thenReturn(Collections.singletonList(highCart));
            when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc()).thenReturn(testDrones);

            CrossEraComparisonResult result = crossEraComparisonService.compareCrossEra();

            double expectedDist = Math.sqrt(2 * 6371000 * 1000.0) / 1000;
            assertEquals(expectedDist, result.getAncientSummary().getMaxRangeKm(), 0.1);
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarioTests {

        @Test
        @DisplayName("空数据库时不应抛异常")
        void compareCrossEra_emptyDatabase_shouldNotThrow() {
            when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc()).thenReturn(Collections.emptyList());
            when(nestCartRepository.findAll()).thenReturn(Collections.emptyList());

            CrossEraComparisonResult result = assertDoesNotThrow(
                    () -> crossEraComparisonService.compareCrossEra());

            assertNotNull(result);
        }

        @Test
        @DisplayName("仅巢车存在时对比维度仍应正常生成")
        void compareCrossEra_onlyCartsExist_shouldStillGenerateDimensions() {
            when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc()).thenReturn(Collections.emptyList());
            when(nestCartRepository.findAll()).thenReturn(testCarts);

            CrossEraComparisonResult result = crossEraComparisonService.compareCrossEra();

            assertEquals(9, result.getComparisonDimensions().size());
            assertNotNull(result.getAncientSummary().getPlatformName());
        }

        @Test
        @DisplayName("巢车和无人机都不存在时应返回未选择状态")
        void compareCartVsDrone_bothNotFound_shouldReturnNotSelected() {
            UUID fakeCartId = UUID.randomUUID();
            UUID fakeDroneId = UUID.randomUUID();
            when(nestCartRepository.findById(fakeCartId)).thenReturn(Optional.empty());
            when(modernDroneSpecRepository.findById(fakeDroneId)).thenReturn(Optional.empty());

            CrossEraComparisonResult result = crossEraComparisonService.compareCartVsDrone(fakeCartId, fakeDroneId);

            assertEquals("未选择", result.getAncientSummary().getPlatformName());
            assertEquals("未选择", result.getModernSummary().getPlatformName());
        }
    }

    @Nested
    @DisplayName("视野效率专项测试")
    class VisionEfficiencyTests {

        @Test
        @DisplayName("无人机侦察半径应≥巢车的5倍")
        void visionEfficiency_droneRangeShouldBeAtLeast5xCart() {
            when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc()).thenReturn(testDrones);
            when(nestCartRepository.findAll()).thenReturn(testCarts);

            CrossEraComparisonResult result = crossEraComparisonService.compareCrossEra();

            Optional<CrossEraComparisonResult.ComparisonDimension> rangeDim =
                    result.getComparisonDimensions().stream()
                            .filter(d -> "侦察半径".equals(d.getDimension()))
                            .findFirst();

            assertTrue(rangeDim.isPresent());
            assertTrue(rangeDim.get().getModernValue() / rangeDim.get().getAncientValue() >= 5,
                    String.format("无人机侦察半径(%.1fkm)应≥巢车(%.1fkm)的5倍",
                            rangeDim.get().getModernValue(), rangeDim.get().getAncientValue()));
        }

        @Test
        @DisplayName("升空高度差异应达2个数量级以上")
        void visionEfficiency_altitudeDifferenceShouldExceed2OrdersOfMagnitude() {
            when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc()).thenReturn(testDrones);
            when(nestCartRepository.findAll()).thenReturn(testCarts);

            CrossEraComparisonResult result = crossEraComparisonService.compareCrossEra();

            double ratio = result.getModernSummary().getMaxAltitudeMeters()
                    / Math.max(result.getAncientSummary().getMaxAltitudeMeters(), 1);

            assertTrue(ratio >= 100,
                    String.format("升空高度比 %.0f 应≥100（2个数量级）", ratio));
        }

        @Test
        @DisplayName("巢车全天候能力评分应极低（≤20分）")
        void visionEfficiency_cartAllWeatherCapabilityShouldBeVeryLow() {
            when(modernDroneSpecRepository.findAllByOrderBySortOrderAsc()).thenReturn(testDrones);
            when(nestCartRepository.findAll()).thenReturn(testCarts);

            CrossEraComparisonResult result = crossEraComparisonService.compareCrossEra();

            Optional<CrossEraComparisonResult.ComparisonDimension> weatherDim =
                    result.getComparisonDimensions().stream()
                            .filter(d -> "全天候能力".equals(d.getDimension()))
                            .findFirst();

            assertTrue(weatherDim.isPresent());
            assertTrue(weatherDim.get().getAncientScore() <= 20,
                    String.format("巢车全天候能力得分 %.1f 应≤20", weatherDim.get().getAncientScore()));
        }
    }
}
