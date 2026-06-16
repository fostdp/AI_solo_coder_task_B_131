package com.nestcart.modules.coverage_optimizer.service;

import com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest;
import com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("覆盖优化服务测试")
class CoverageOptimizerServiceTest {

    private final CoverageOptimizerService service = new CoverageOptimizerService();

    private CollaborativeCoverageRequest buildRequest(int cartCount, double w, double h,
                                                       String strategy, int iterations) {
        List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();
        for (int i = 0; i < cartCount; i++) {
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID())
                    .cartName("巢车-" + (char) ('A' + i))
                    .x(w * (0.2 + 0.6 * (i / (double) cartCount)))
                    .y(h * (0.2 + 0.6 * (i / (double) cartCount)))
                    .height(12.0)
                    .visionRadiusKm(null)
                    .movable(true)
                    .build());
        }
        return CollaborativeCoverageRequest.builder()
                .region("test_battlefield")
                .regionWidthKm(w)
                .regionHeightKm(h)
                .strategy(strategy)
                .maxIterations(iterations)
                .carts(carts)
                .build();
    }

    @Nested
    @DisplayName("正常场景：协同覆盖验证")
    class NormalCoverageTests {

        @Test
        @DisplayName("优化后覆盖率应≥优化前初始覆盖率")
        void optimizeCoverage_finalCoverageShouldNotDecrease() {
            CollaborativeCoverageRequest request = buildRequest(3, 10, 10, "greedy_spread", 100);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertTrue(result.getCoverageRatio() >= result.getOptimizationMetrics().getInitialCoverage() - 0.01,
                    String.format("最终覆盖率%.2f应≥初始%.2f",
                            result.getCoverageRatio(), result.getOptimizationMetrics().getInitialCoverage()));
        }

        @Test
        @DisplayName("增加巢车数量应减少盲区数量")
        void optimizeCoverage_moreCartsShouldReduceBlindZones() {
            CollaborativeCoverageRequest req2 = buildRequest(2, 10, 10, "greedy_spread", 100);
            CollaborativeCoverageRequest req5 = buildRequest(5, 10, 10, "greedy_spread", 100);

            CollaborativeCoverageResult result2 = service.optimizeCoverage(req2);
            CollaborativeCoverageResult result5 = service.optimizeCoverage(req5);

            assertTrue(result5.getBlindZones().size() <= result2.getBlindZones().size(),
                    String.format("5车盲区%d应≤2车盲区%d", result5.getBlindZones().size(), result2.getBlindZones().size()));
        }

        @Test
        @DisplayName("增加巢车数量应提升覆盖率")
        void optimizeCoverage_moreCartsShouldIncreaseCoverage() {
            CollaborativeCoverageRequest req2 = buildRequest(2, 10, 10, "greedy_spread", 100);
            CollaborativeCoverageRequest req5 = buildRequest(5, 10, 10, "greedy_spread", 100);

            CollaborativeCoverageResult result2 = service.optimizeCoverage(req2);
            CollaborativeCoverageResult result5 = service.optimizeCoverage(req5);

            assertTrue(result5.getCoverageRatio() >= result2.getCoverageRatio(),
                    String.format("5车覆盖率%.2f应≥2车%.2f", result5.getCoverageRatio(), result2.getCoverageRatio()));
        }

        @Test
        @DisplayName("热力图应包含2500个数据点（50×50网格）")
        void optimizeCoverage_heatmapShouldHave2500Points() {
            CollaborativeCoverageRequest request = buildRequest(3, 10, 10, "greedy_spread", 50);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertEquals(2500, result.getCoverageHeatmap().size());
        }

        @Test
        @DisplayName("放置结果应包含正确数量的巢车")
        void optimizeCoverage_placementsShouldMatchCartCount() {
            CollaborativeCoverageRequest request = buildRequest(4, 10, 10, "greedy_spread", 50);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertEquals(4, result.getPlacements().size());
            assertEquals(4, result.getCartCount());
        }

        @Test
        @DisplayName("优化指标应正确记录策略名称")
        void optimizeCoverage_metricsShouldRecordStrategy() {
            CollaborativeCoverageRequest request = buildRequest(3, 10, 10, "simulated_annealing", 50);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertEquals("simulated_annealing", result.getOptimizationMetrics().getStrategy());
        }

        @Test
        @DisplayName("计算耗时应在合理范围内（<5秒）")
        void optimizeCoverage_computeTimeShouldBeReasonable() {
            CollaborativeCoverageRequest request = buildRequest(3, 10, 10, "greedy_spread", 200);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertTrue(result.getOptimizationMetrics().getComputeTimeMs() < 5000,
                    String.format("计算耗时%dms应<5000ms", result.getOptimizationMetrics().getComputeTimeMs()));
        }

        @Test
        @DisplayName("总覆盖面积应等于总面积乘以覆盖率")
        void optimizeCoverage_coveredAreaShouldMatchRatio() {
            CollaborativeCoverageRequest request = buildRequest(3, 10, 10, "greedy_spread", 50);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            double expectedCovered = result.getTotalAreaSqKm() * result.getCoverageRatio();
            assertEquals(expectedCovered, result.getCoveredAreaSqKm(), 0.01,
                    "覆盖面积应等于总面积乘以覆盖率");
        }
    }

    @Nested
    @DisplayName("三种优化策略对比")
    class StrategyComparisonTests {

        @Test
        @DisplayName("贪心扩散策略应正常完成")
        void greedySpread_shouldComplete() {
            CollaborativeCoverageRequest request = buildRequest(3, 10, 10, "greedy_spread", 50);
            assertDoesNotThrow(() -> service.optimizeCoverage(request));
        }

        @Test
        @DisplayName("模拟退火策略应正常完成")
        void simulatedAnnealing_shouldComplete() {
            CollaborativeCoverageRequest request = buildRequest(3, 10, 10, "simulated_annealing", 50);
            assertDoesNotThrow(() -> service.optimizeCoverage(request));
        }

        @Test
        @DisplayName("力导向布局策略应正常完成")
        void forceDirected_shouldComplete() {
            CollaborativeCoverageRequest request = buildRequest(3, 10, 10, "force_directed", 50);
            assertDoesNotThrow(() -> service.optimizeCoverage(request));
        }

        @Test
        @DisplayName("未知策略应降级为贪心扩散")
        void unknownStrategy_shouldFallbackToGreedy() {
            CollaborativeCoverageRequest request = buildRequest(3, 10, 10, "unknown_strategy", 50);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertEquals("unknown_strategy", result.getOptimizationMetrics().getStrategy());
            assertTrue(result.getCoverageRatio() >= 0);
        }

        @Test
        @DisplayName("默认策略应为贪心扩散")
        void defaultStrategy_shouldBeGreedySpread() {
            CollaborativeCoverageRequest request = CollaborativeCoverageRequest.builder()
                    .region("test").regionWidthKm(10.0).regionHeightKm(10.0)
                    .maxIterations(10).build();

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertEquals("greedy_spread", result.getOptimizationMetrics().getStrategy());
        }

        @Test
        @DisplayName("力导向策略的覆盖率提升应为非负")
        void forceDirected_coverageImprovementShouldBeNonNegative() {
            List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                        .cartId(UUID.randomUUID()).cartName("巢车-" + i)
                        .x(5.0).y(5.0).height(12.0).movable(true).build());
            }
            CollaborativeCoverageRequest request = CollaborativeCoverageRequest.builder()
                    .region("test").regionWidthKm(20.0).regionHeightKm(20.0)
                    .strategy("force_directed").maxIterations(200).carts(carts).build();

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertTrue(result.getOptimizationMetrics().getCoverageImprovement() >= -0.01,
                    "力导向策略覆盖率提升应接近非负");
        }
    }

    @Nested
    @DisplayName("边界场景")
    class BoundaryTests {

        @Test
        @DisplayName("0次迭代时优化结果应等于初始状态")
        void zeroIterations_resultShouldEqualInitial() {
            CollaborativeCoverageRequest request = buildRequest(2, 10, 10, "greedy_spread", 0);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertEquals(0, result.getOptimizationMetrics().getCoverageImprovement(), 0.01);
        }

        @Test
        @DisplayName("单辆巢车应产生盲区")
        void singleCart_shouldHaveBlindZones() {
            CollaborativeCoverageRequest request = buildRequest(1, 10, 10, "greedy_spread", 50);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertTrue(result.getCoverageRatio() < 1.0, "单辆车无法100%覆盖10km×10km");
        }

        @Test
        @DisplayName("极小战场单辆车应可全覆盖")
        void tinyBattlefield_singleCartShouldCoverAll() {
            CollaborativeCoverageRequest request = buildRequest(1, 0.1, 0.1, "greedy_spread", 10);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertTrue(result.getCoverageRatio() >= 0.9,
                    String.format("极小战场覆盖率%.2f应≥0.9", result.getCoverageRatio()));
        }

        @Test
        @DisplayName("极大战场覆盖率应极低")
        void hugeBattlefield_coverageShouldBeVeryLow() {
            CollaborativeCoverageRequest request = buildRequest(3, 1000, 1000, "greedy_spread", 10);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertTrue(result.getCoverageRatio() < 0.01,
                    String.format("极大战场覆盖率%.4f应<0.01", result.getCoverageRatio()));
        }

        @Test
        @DisplayName("大量巢车应正常处理")
        void manyCarts_shouldProcessCorrectly() {
            CollaborativeCoverageRequest request = buildRequest(20, 10, 10, "greedy_spread", 10);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertEquals(20, result.getPlacements().size());
            assertTrue(result.getCoverageRatio() > 0);
        }

        @Test
        @DisplayName("不可移动的巢车位置不应改变")
        void immovableCart_positionShouldNotChange() {
            List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID()).cartName("固定巢车")
                    .x(5.0).y(5.0).height(12.0)
                    .visionRadiusKm(null).movable(false).build());
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID()).cartName("移动巢车")
                    .x(7.0).y(7.0).height(12.0)
                    .visionRadiusKm(null).movable(true).build());

            CollaborativeCoverageRequest request = CollaborativeCoverageRequest.builder()
                    .region("test").regionWidthKm(10.0).regionHeightKm(10.0)
                    .strategy("greedy_spread").maxIterations(50).carts(carts).build();

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            CollaborativeCoverageResult.CartPlacement fixed = result.getPlacements().stream()
                    .filter(p -> "固定巢车".equals(p.getCartName())).findFirst().orElse(null);
            assertNotNull(fixed);
            assertEquals(5.0, fixed.getX(), 0.01);
            assertEquals(5.0, fixed.getY(), 0.01);
        }

        @Test
        @DisplayName("巢车坐标超出战场范围应被钳位")
        void outOfBoundsCart_shouldBeClamped() {
            List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID()).cartName("越界巢车")
                    .x(-5.0).y(15.0).height(12.0)
                    .visionRadiusKm(null).movable(true).build());
            CollaborativeCoverageRequest request = CollaborativeCoverageRequest.builder()
                    .region("test").regionWidthKm(10.0).regionHeightKm(10.0)
                    .strategy("greedy_spread").maxIterations(10).carts(carts).build();

            assertDoesNotThrow(() -> service.optimizeCoverage(request));
            CollaborativeCoverageResult result = service.optimizeCoverage(request);
            assertTrue(result.getCoverageRatio() >= 0);
        }

        @Test
        @DisplayName("总面积计算应正确")
        void totalArea_shouldBeCalculatedCorrectly() {
            CollaborativeCoverageRequest request = buildRequest(3, 8, 6, "greedy_spread", 10);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertEquals(48.0, result.getTotalAreaSqKm(), 0.01);
        }
    }

    @Nested
    @DisplayName("空数据与异常场景")
    class ExceptionTests {

        @Test
        @DisplayName("carts为null时应自动生成默认巢车")
        void nullCarts_shouldGenerateDefaultCarts() {
            CollaborativeCoverageRequest request = CollaborativeCoverageRequest.builder()
                    .region("test").regionWidthKm(10.0).regionHeightKm(10.0)
                    .strategy("greedy_spread").maxIterations(10).carts(null).build();

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertEquals(2, result.getCartCount());
        }

        @Test
        @DisplayName("所有字段为null时应使用默认值且不抛异常")
        void allNullFields_shouldUseDefaults() {
            CollaborativeCoverageRequest request = CollaborativeCoverageRequest.builder().build();

            assertDoesNotThrow(() -> service.optimizeCoverage(request));
            CollaborativeCoverageResult result = service.optimizeCoverage(request);
            assertNotNull(result);
            assertNotNull(result.getPlacements());
            assertFalse(result.getPlacements().isEmpty());
        }

        @Test
        @DisplayName("空carts列表应生成默认巢车")
        void emptyCarts_shouldGenerateDefaultCarts() {
            CollaborativeCoverageRequest request = CollaborativeCoverageRequest.builder()
                    .region("test").regionWidthKm(10.0).regionHeightKm(10.0)
                    .strategy("greedy_spread").maxIterations(10)
                    .carts(new ArrayList<>()).build();

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertEquals(2, result.getCartCount());
        }

        @Test
        @DisplayName("region为null时应使用默认区域名")
        void nullRegion_shouldUseDefault() {
            CollaborativeCoverageRequest request = CollaborativeCoverageRequest.builder()
                    .regionWidthKm(10.0).regionHeightKm(10.0)
                    .maxIterations(10).build();

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertNotNull(result.getRegion());
            assertFalse(result.getRegion().isEmpty());
        }
    }

    @Nested
    @DisplayName("盲区减少专项验证")
    class BlindZoneReductionTests {

        @Test
        @DisplayName("2车→4车，盲区数量应减少")
        void blindZoneReduction_2to4Carts_shouldReduce() {
            CollaborativeCoverageRequest req2 = buildRequest(2, 10, 10, "greedy_spread", 100);
            CollaborativeCoverageRequest req4 = buildRequest(4, 10, 10, "greedy_spread", 100);

            CollaborativeCoverageResult result2 = service.optimizeCoverage(req2);
            CollaborativeCoverageResult result4 = service.optimizeCoverage(req4);

            int blindZones2 = result2.getBlindZones().size();
            int blindZones4 = result4.getBlindZones().size();
            assertTrue(blindZones4 <= blindZones2 || blindZones4 == 0,
                    String.format("4车盲区%d应≤2车盲区%d", blindZones4, blindZones2));
        }

        @Test
        @DisplayName("提高巢车高度应增加视野半径从而减少盲区")
        void blindZoneReduction_higherCartShouldReduceBlindZones() {
            List<CollaborativeCoverageRequest.CartSpec> lowCarts = new ArrayList<>();
            lowCarts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID()).cartName("低巢车")
                    .x(5.0).y(5.0).height(6.0).movable(false).build());
            List<CollaborativeCoverageRequest.CartSpec> highCarts = new ArrayList<>();
            highCarts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID()).cartName("高巢车")
                    .x(5.0).y(5.0).height(20.0).movable(false).build());

            CollaborativeCoverageRequest reqLow = CollaborativeCoverageRequest.builder()
                    .region("test").regionWidthKm(10.0).regionHeightKm(10.0)
                    .strategy("greedy_spread").maxIterations(10).carts(lowCarts).build();
            CollaborativeCoverageRequest reqHigh = CollaborativeCoverageRequest.builder()
                    .region("test").regionWidthKm(10.0).regionHeightKm(10.0)
                    .strategy("greedy_spread").maxIterations(10).carts(highCarts).build();

            CollaborativeCoverageResult resultLow = service.optimizeCoverage(reqLow);
            CollaborativeCoverageResult resultHigh = service.optimizeCoverage(reqHigh);

            assertTrue(resultHigh.getCoverageRatio() > resultLow.getCoverageRatio(),
                    String.format("高车覆盖率%.2f应>低车%.2f", resultHigh.getCoverageRatio(), resultLow.getCoverageRatio()));
        }

        @Test
        @DisplayName("优化后覆盖率提升应≥0（初始位置集中时）")
        void blindZoneReduction_optimizationShouldImproveCoverage() {
            List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                        .cartId(UUID.randomUUID()).cartName("巢车-" + i)
                        .x(5.0).y(5.0).height(12.0).movable(true).build());
            }
            CollaborativeCoverageRequest request = CollaborativeCoverageRequest.builder()
                    .region("test").regionWidthKm(20.0).regionHeightKm(20.0)
                    .strategy("greedy_spread").maxIterations(200).carts(carts).build();

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertTrue(result.getOptimizationMetrics().getCoverageImprovement() >= 0,
                    "优化后覆盖率提升应≥0");
        }

        @Test
        @DisplayName("指定视野半径时应使用指定值")
        void customVisionRadius_shouldBeUsed() {
            double customRadius = 5.0;
            List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID()).cartName("自定义半径车")
                    .x(5.0).y(5.0).height(12.0)
                    .visionRadiusKm(customRadius).movable(false).build());

            CollaborativeCoverageRequest request = CollaborativeCoverageRequest.builder()
                    .region("test").regionWidthKm(10.0).regionHeightKm(10.0)
                    .strategy("greedy_spread").maxIterations(10).carts(carts).build();

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertEquals(customRadius, result.getPlacements().get(0).getVisionRadius(), 0.001);
        }

        @Test
        @DisplayName("热力图每个点应包含3个值：x, y, count")
        void heatmapPoints_shouldHaveThreeValues() {
            CollaborativeCoverageRequest request = buildRequest(2, 10, 10, "greedy_spread", 10);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertFalse(result.getCoverageHeatmap().isEmpty());
            for (double[] point : result.getCoverageHeatmap()) {
                assertEquals(3, point.length);
            }
        }

        @Test
        @DisplayName("覆盖区域应非负且不超过总面积")
        void coveredArea_shouldBeWithinValidRange() {
            CollaborativeCoverageRequest request = buildRequest(3, 10, 10, "greedy_spread", 50);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertTrue(result.getCoveredAreaSqKm() >= 0, "覆盖面积应≥0");
            assertTrue(result.getCoveredAreaSqKm() <= result.getTotalAreaSqKm(),
                    "覆盖面积应≤总面积");
        }

        @Test
        @DisplayName("重叠面积应非负")
        void overlapArea_shouldBeNonNegative() {
            CollaborativeCoverageRequest request = buildRequest(3, 10, 10, "greedy_spread", 50);

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertTrue(result.getOverlapAreaSqKm() >= 0, "重叠面积应≥0");
            assertTrue(result.getOverlapRatio() >= 0, "重叠率应≥0");
        }

        @Test
        @DisplayName("快速模式应正常工作")
        void fastMode_shouldWork() {
            CollaborativeCoverageRequest request = buildRequest(3, 30, 30, "greedy_spread", 100);
            request.setFastMode("fast");

            CollaborativeCoverageResult result = service.optimizeCoverage(request);

            assertTrue(result.getOptimizationMetrics().getFastMode());
            assertTrue(result.getCoverageRatio() >= 0);
        }
    }
}
