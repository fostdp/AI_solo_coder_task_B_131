package com.nestcart.modules.coverage_optimizer.algorithm;

import com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("覆盖优化策略测试")
class CoverageOptimizationStrategyTest {

    private static final double WIDTH_KM = 10.0;
    private static final double HEIGHT_KM = 10.0;
    private static final int GRID_RES = 25;
    private static final int MAX_ITERATIONS = 100;
    private static final double VISION_RADIUS_KM = 12.0;

    private GreedySpreadStrategy greedySpreadStrategy;
    private SimulatedAnnealingStrategy simulatedAnnealingStrategy;
    private ForceDirectedStrategy forceDirectedStrategy;

    private List<CollaborativeCoverageRequest.CartSpec> buildCarts(int count, boolean clustered) {
        List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double x, y;
            if (clustered) {
                x = WIDTH_KM / 2 + (i - count / 2.0) * 0.5;
                y = HEIGHT_KM / 2 + (i - count / 2.0) * 0.5;
            } else {
                x = WIDTH_KM * (0.2 + 0.6 * (i / (double) count));
                y = HEIGHT_KM * (0.2 + 0.6 * (i / (double) count));
            }
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID())
                    .cartName("巢车-" + (char) ('A' + i))
                    .x(x)
                    .y(y)
                    .height(12.0)
                    .visionRadiusKm(VISION_RADIUS_KM)
                    .movable(true)
                    .build());
        }
        return carts;
    }

    private double computeCoverage(List<CollaborativeCoverageRequest.CartSpec> carts) {
        int covered = 0;
        double cellW = WIDTH_KM / GRID_RES;
        double cellH = HEIGHT_KM / GRID_RES;
        for (int i = 0; i < GRID_RES; i++) {
            for (int j = 0; j < GRID_RES; j++) {
                double px = (i + 0.5) * cellW;
                double py = (j + 0.5) * cellH;
                for (CollaborativeCoverageRequest.CartSpec c : carts) {
                    double dx = px - c.getX();
                    double dy = py - c.getY();
                    double r = c.getVisionRadiusKm();
                    if (dx * dx + dy * dy <= r * r) {
                        covered++;
                        break;
                    }
                }
            }
        }
        return (double) covered / (GRID_RES * GRID_RES);
    }

    private double computeMinDistance(List<CollaborativeCoverageRequest.CartSpec> carts) {
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < carts.size(); i++) {
            for (int j = i + 1; j < carts.size(); j++) {
                double dx = carts.get(i).getX() - carts.get(j).getX();
                double dy = carts.get(i).getY() - carts.get(j).getY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < minDist) {
                    minDist = dist;
                }
            }
        }
        return minDist;
    }

    @BeforeEach
    void setUp() {
        greedySpreadStrategy = new GreedySpreadStrategy();
        simulatedAnnealingStrategy = new SimulatedAnnealingStrategy();
        forceDirectedStrategy = new ForceDirectedStrategy();
    }

    @Nested
    @DisplayName("贪心扩散策略测试")
    class GreedySpreadStrategyTests {

        @Test
        @DisplayName("优化后覆盖率不应低于初始覆盖率")
        void optimize_coverageShouldNotDecrease() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(3, true);
            double initialCoverage = computeCoverage(carts);

            List<CollaborativeCoverageRequest.CartSpec> result = greedySpreadStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);
            double finalCoverage = computeCoverage(result);

            assertTrue(finalCoverage >= initialCoverage - 0.01,
                    String.format("优化后覆盖率%.3f应≥初始%.3f", finalCoverage, initialCoverage));
        }

        @Test
        @DisplayName("优化后车辆数量应保持不变")
        void optimize_cartCountShouldRemainSame() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(4, false);

            List<CollaborativeCoverageRequest.CartSpec> result = greedySpreadStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);

            assertEquals(4, result.size());
        }

        @Test
        @DisplayName("0次迭代时结果应与初始状态一致")
        void optimize_zeroIterations_shouldReturnInitial() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(3, false);

            List<CollaborativeCoverageRequest.CartSpec> result = greedySpreadStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, 0, GRID_RES);

            assertEquals(carts.size(), result.size());
            for (int i = 0; i < carts.size(); i++) {
                assertEquals(carts.get(i).getX(), result.get(i).getX(), 0.001);
                assertEquals(carts.get(i).getY(), result.get(i).getY(), 0.001);
            }
        }

        @Test
        @DisplayName("不可移动的车辆位置不应改变")
        void optimize_immovableCarts_shouldNotMove() {
            List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID())
                    .cartName("固定车")
                    .x(5.0).y(5.0)
                    .height(12.0).visionRadiusKm(VISION_RADIUS_KM)
                    .movable(false).build());
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID())
                    .cartName("移动车")
                    .x(5.0).y(5.0)
                    .height(12.0).visionRadiusKm(VISION_RADIUS_KM)
                    .movable(true).build());

            List<CollaborativeCoverageRequest.CartSpec> result = greedySpreadStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);

            CollaborativeCoverageRequest.CartSpec fixed = result.stream()
                    .filter(c -> "固定车".equals(c.getCartName()))
                    .findFirst().orElse(null);
            assertNotNull(fixed);
            assertEquals(5.0, fixed.getX(), 0.001);
            assertEquals(5.0, fixed.getY(), 0.001);
        }

        @Test
        @DisplayName("所有车辆坐标应在边界范围内")
        void optimize_allCarts_shouldBeWithinBounds() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(5, true);

            List<CollaborativeCoverageRequest.CartSpec> result = greedySpreadStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);

            for (CollaborativeCoverageRequest.CartSpec cart : result) {
                assertTrue(cart.getX() >= 0 && cart.getX() <= WIDTH_KM,
                        "车辆X坐标" + cart.getX() + "应在[0, " + WIDTH_KM + "]范围内");
                assertTrue(cart.getY() >= 0 && cart.getY() <= HEIGHT_KM,
                        "车辆Y坐标" + cart.getY() + "应在[0, " + HEIGHT_KM + "]范围内");
            }
        }

        @Test
        @DisplayName("单辆车优化后仍应在边界内")
        void optimize_singleCart_shouldStayInBounds() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(1, false);

            List<CollaborativeCoverageRequest.CartSpec> result = greedySpreadStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);

            assertEquals(1, result.size());
            assertTrue(result.get(0).getX() >= 0 && result.get(0).getX() <= WIDTH_KM);
            assertTrue(result.get(0).getY() >= 0 && result.get(0).getY() <= HEIGHT_KM);
        }

        @Test
        @DisplayName("车辆ID和名称应保持不变")
        void optimize_cartIdentity_shouldBePreserved() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(3, false);
            List<UUID> originalIds = new ArrayList<>();
            List<String> originalNames = new ArrayList<>();
            for (CollaborativeCoverageRequest.CartSpec c : carts) {
                originalIds.add(c.getCartId());
                originalNames.add(c.getCartName());
            }

            List<CollaborativeCoverageRequest.CartSpec> result = greedySpreadStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, 50, GRID_RES);

            for (int i = 0; i < result.size(); i++) {
                assertEquals(originalIds.get(i), result.get(i).getCartId());
                assertEquals(originalNames.get(i), result.get(i).getCartName());
            }
        }

        @Test
        @DisplayName("空列表应返回空列表")
        void optimize_emptyList_shouldReturnEmpty() {
            List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();

            List<CollaborativeCoverageRequest.CartSpec> result = greedySpreadStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("模拟退火策略测试")
    class SimulatedAnnealingStrategyTests {

        @RepeatedTest(3)
        @DisplayName("3次独立运行均应产生有效结果")
        void optimize_repeatedRuns_shouldAllBeValid() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(3, true);

            List<CollaborativeCoverageRequest.CartSpec> result = simulatedAnnealingStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);
            double coverage = computeCoverage(result);

            assertTrue(coverage >= 0 && coverage <= 1.0,
                    "覆盖率" + coverage + "应在[0, 1]范围内");
            assertFalse(result.isEmpty());
            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("优化后覆盖率不应显著低于初始")
        void optimize_coverageShouldNotDecreaseSignificantly() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(3, true);
            double initialCoverage = computeCoverage(carts);

            List<CollaborativeCoverageRequest.CartSpec> result = simulatedAnnealingStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS * 2, GRID_RES);
            double finalCoverage = computeCoverage(result);

            assertTrue(finalCoverage >= initialCoverage - 0.05,
                    String.format("优化后覆盖率%.3f不应显著低于初始%.3f", finalCoverage, initialCoverage));
        }

        @Test
        @DisplayName("优化后车辆数量应保持不变")
        void optimize_cartCountShouldRemainSame() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(5, false);

            List<CollaborativeCoverageRequest.CartSpec> result = simulatedAnnealingStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, 50, GRID_RES);

            assertEquals(5, result.size());
        }

        @Test
        @DisplayName("所有车辆坐标应在边界范围内")
        void optimize_allCarts_shouldBeWithinBounds() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(4, true);

            List<CollaborativeCoverageRequest.CartSpec> result = simulatedAnnealingStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);

            for (CollaborativeCoverageRequest.CartSpec cart : result) {
                assertTrue(cart.getX() >= 0 && cart.getX() <= WIDTH_KM,
                        "车辆X坐标应在范围内");
                assertTrue(cart.getY() >= 0 && cart.getY() <= HEIGHT_KM,
                        "车辆Y坐标应在范围内");
            }
        }

        @Test
        @DisplayName("不可移动的车辆位置不应改变")
        void optimize_immovableCarts_shouldNotMove() {
            List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID())
                    .cartName("固定车SA")
                    .x(3.0).y(7.0)
                    .height(12.0).visionRadiusKm(VISION_RADIUS_KM)
                    .movable(false).build());
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID())
                    .cartName("移动车SA")
                    .x(5.0).y(5.0)
                    .height(12.0).visionRadiusKm(VISION_RADIUS_KM)
                    .movable(true).build());

            List<CollaborativeCoverageRequest.CartSpec> result = simulatedAnnealingStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);

            CollaborativeCoverageRequest.CartSpec fixed = result.stream()
                    .filter(c -> "固定车SA".equals(c.getCartName()))
                    .findFirst().orElse(null);
            assertNotNull(fixed);
            assertEquals(3.0, fixed.getX(), 0.001);
            assertEquals(7.0, fixed.getY(), 0.001);
        }

        @Test
        @DisplayName("0次迭代时结果应与初始一致")
        void optimize_zeroIterations_shouldReturnInitial() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(3, false);

            List<CollaborativeCoverageRequest.CartSpec> result = simulatedAnnealingStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, 0, GRID_RES);

            assertEquals(carts.size(), result.size());
            for (int i = 0; i < carts.size(); i++) {
                assertEquals(carts.get(i).getX(), result.get(i).getX(), 0.001);
                assertEquals(carts.get(i).getY(), result.get(i).getY(), 0.001);
            }
        }

        @Test
        @DisplayName("单辆车应正常优化")
        void optimize_singleCart_shouldWork() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(1, false);

            List<CollaborativeCoverageRequest.CartSpec> result = simulatedAnnealingStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, 50, GRID_RES);

            assertEquals(1, result.size());
            assertNotNull(result.get(0).getCartId());
        }

        @Test
        @DisplayName("车辆视野半径应保持不变")
        void optimize_visionRadius_shouldBePreserved() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(3, false);

            List<CollaborativeCoverageRequest.CartSpec> result = simulatedAnnealingStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, 50, GRID_RES);

            for (int i = 0; i < result.size(); i++) {
                assertEquals(carts.get(i).getVisionRadiusKm(), result.get(i).getVisionRadiusKm());
            }
        }
    }

    @Nested
    @DisplayName("力导向策略测试")
    class ForceDirectedStrategyTests {

        @Test
        @DisplayName("优化后车辆最小间距应增大")
        void optimize_minDistanceShouldIncrease() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(4, true);
            double initialMinDist = computeMinDistance(carts);

            List<CollaborativeCoverageRequest.CartSpec> result = forceDirectedStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);
            double finalMinDist = computeMinDistance(result);

            assertTrue(finalMinDist >= initialMinDist - 0.01,
                    String.format("优化后最小间距%.3f应≥初始%.3f", finalMinDist, initialMinDist));
        }

        @Test
        @DisplayName("优化后车辆数量应保持不变")
        void optimize_cartCountShouldRemainSame() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(5, true);

            List<CollaborativeCoverageRequest.CartSpec> result = forceDirectedStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);

            assertEquals(5, result.size());
        }

        @Test
        @DisplayName("所有车辆坐标应在边界范围内")
        void optimize_allCarts_shouldBeWithinBounds() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(6, true);

            List<CollaborativeCoverageRequest.CartSpec> result = forceDirectedStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);

            for (CollaborativeCoverageRequest.CartSpec cart : result) {
                assertTrue(cart.getX() >= 0 && cart.getX() <= WIDTH_KM,
                        "车辆X坐标" + cart.getX() + "应在范围内");
                assertTrue(cart.getY() >= 0 && cart.getY() <= HEIGHT_KM,
                        "车辆Y坐标" + cart.getY() + "应在范围内");
            }
        }

        @Test
        @DisplayName("不可移动的车辆位置不应改变")
        void optimize_immovableCarts_shouldNotMove() {
            List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID())
                    .cartName("锚点车")
                    .x(5.0).y(5.0)
                    .height(12.0).visionRadiusKm(VISION_RADIUS_KM)
                    .movable(false).build());
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID())
                    .cartName("斥力车")
                    .x(5.0).y(5.0)
                    .height(12.0).visionRadiusKm(VISION_RADIUS_KM)
                    .movable(true).build());

            List<CollaborativeCoverageRequest.CartSpec> result = forceDirectedStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);

            CollaborativeCoverageRequest.CartSpec fixed = result.stream()
                    .filter(c -> "锚点车".equals(c.getCartName()))
                    .findFirst().orElse(null);
            assertNotNull(fixed);
            assertEquals(5.0, fixed.getX(), 0.001);
            assertEquals(5.0, fixed.getY(), 0.001);
        }

        @Test
        @DisplayName("0次迭代时结果应与初始一致")
        void optimize_zeroIterations_shouldReturnInitial() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(3, true);

            List<CollaborativeCoverageRequest.CartSpec> result = forceDirectedStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, 0, GRID_RES);

            assertEquals(carts.size(), result.size());
            for (int i = 0; i < carts.size(); i++) {
                assertEquals(carts.get(i).getX(), result.get(i).getX(), 0.001);
                assertEquals(carts.get(i).getY(), result.get(i).getY(), 0.001);
            }
        }

        @Test
        @DisplayName("两辆车应相互排斥远离")
        void optimize_twoCarts_shouldRepelEachOther() {
            List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID())
                    .cartName("车1")
                    .x(4.5).y(5.0)
                    .height(12.0).visionRadiusKm(VISION_RADIUS_KM)
                    .movable(true).build());
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(UUID.randomUUID())
                    .cartName("车2")
                    .x(5.5).y(5.0)
                    .height(12.0).visionRadiusKm(VISION_RADIUS_KM)
                    .movable(true).build());
            double initialDist = computeMinDistance(carts);

            List<CollaborativeCoverageRequest.CartSpec> result = forceDirectedStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);
            double finalDist = computeMinDistance(result);

            assertTrue(finalDist > initialDist,
                    String.format("两辆车优化后间距%.3f应大于初始%.3f", finalDist, initialDist));
        }

        @Test
        @DisplayName("单辆车优化后仍在边界内")
        void optimize_singleCart_shouldStayInBounds() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(1, false);

            List<CollaborativeCoverageRequest.CartSpec> result = forceDirectedStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);

            assertEquals(1, result.size());
            assertTrue(result.get(0).getX() >= 0 && result.get(0).getX() <= WIDTH_KM);
            assertTrue(result.get(0).getY() >= 0 && result.get(0).getY() <= HEIGHT_KM);
        }

        @Test
        @DisplayName("多辆车应分散开")
        void optimize_multipleCarts_shouldSpreadOut() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(5, true);
            double initialAvgDist = 0;
            int count = 0;
            for (int i = 0; i < carts.size(); i++) {
                for (int j = i + 1; j < carts.size(); j++) {
                    double dx = carts.get(i).getX() - carts.get(j).getX();
                    double dy = carts.get(i).getY() - carts.get(j).getY();
                    initialAvgDist += Math.sqrt(dx * dx + dy * dy);
                    count++;
                }
            }
            initialAvgDist /= count;

            List<CollaborativeCoverageRequest.CartSpec> result = forceDirectedStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, MAX_ITERATIONS, GRID_RES);

            double finalAvgDist = 0;
            for (int i = 0; i < result.size(); i++) {
                for (int j = i + 1; j < result.size(); j++) {
                    double dx = result.get(i).getX() - result.get(j).getX();
                    double dy = result.get(i).getY() - result.get(j).getY();
                    finalAvgDist += Math.sqrt(dx * dx + dy * dy);
                }
            }
            finalAvgDist /= count;

            assertTrue(finalAvgDist > initialAvgDist,
                    String.format("平均间距%.3f应大于初始%.3f", finalAvgDist, initialAvgDist));
        }
    }

    @Nested
    @DisplayName("策略接口通用测试")
    class StrategyInterfaceTests {

        @Test
        @DisplayName("三种策略均实现CoverageOptimizationStrategy接口")
        void allStrategies_shouldImplementInterface() {
            assertTrue(greedySpreadStrategy instanceof CoverageOptimizationStrategy);
            assertTrue(simulatedAnnealingStrategy instanceof CoverageOptimizationStrategy);
            assertTrue(forceDirectedStrategy instanceof CoverageOptimizationStrategy);
        }

        @Test
        @DisplayName("三种策略均继承自AbstractCoverageOptimizationStrategy")
        void allStrategies_shouldExtendAbstractClass() {
            assertTrue(greedySpreadStrategy instanceof AbstractCoverageOptimizationStrategy);
            assertTrue(simulatedAnnealingStrategy instanceof AbstractCoverageOptimizationStrategy);
            assertTrue(forceDirectedStrategy instanceof AbstractCoverageOptimizationStrategy);
        }

        @Test
        @DisplayName("所有策略optimize方法不应抛出异常")
        void optimize_allStrategies_shouldNotThrow() {
            List<CollaborativeCoverageRequest.CartSpec> carts = buildCarts(3, false);

            assertDoesNotThrow(() -> greedySpreadStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, 50, GRID_RES));
            assertDoesNotThrow(() -> simulatedAnnealingStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, 50, GRID_RES));
            assertDoesNotThrow(() -> forceDirectedStrategy.optimize(
                    carts, WIDTH_KM, HEIGHT_KM, 50, GRID_RES));
        }
    }
}
