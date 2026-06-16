package com.nestcart.modules.visibility_compute;

import com.nestcart.modules.visibility_compute.dto.LineOfSightResult;
import com.nestcart.modules.visibility_compute.service.LineOfSightService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LineOfSightService 单元测试")
class LineOfSightServiceTest {

    private final LineOfSightService service = new LineOfSightService();

    private static final double EARTH_RADIUS = 6371000.0;
    private static final double GRID_RESOLUTION = 1.0;

    private double flatTerrain(int x, int y) {
        return 0.0;
    }

    private double mountainTerrain(int x, int y) {
        if (x == 5 && y == 5) {
            return 100.0;
        }
        return 0.0;
    }

    private double ridgeTerrain(int x, int y) {
        if (x >= 3 && x <= 7 && y == 5) {
            return 50.0;
        }
        return 0.0;
    }

    @Nested
    @DisplayName("Bresenham 通视算法 - 基本测试")
    class BresenhamBasicTests {

        @Test
        @DisplayName("平坦地形：水平直线应全可见")
        void isLineOfSightBresenham_flatTerrain_horizontal_shouldBeVisible() {
            boolean visible = service.isLineOfSightBresenham(
                    0, 5, 10.0,
                    10, 5,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::flatTerrain
            );
            assertTrue(visible, "平坦地形水平视线应可见");
        }

        @Test
        @DisplayName("平坦地形：垂直直线应全可见")
        void isLineOfSightBresenham_flatTerrain_vertical_shouldBeVisible() {
            boolean visible = service.isLineOfSightBresenham(
                    5, 0, 10.0,
                    5, 10,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::flatTerrain
            );
            assertTrue(visible, "平坦地形垂直视线应可见");
        }

        @Test
        @DisplayName("平坦地形：对角线应全可见")
        void isLineOfSightBresenham_flatTerrain_diagonal_shouldBeVisible() {
            boolean visible = service.isLineOfSightBresenham(
                    0, 0, 10.0,
                    10, 10,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::flatTerrain
            );
            assertTrue(visible, "平坦地形对角线视线应可见");
        }
    }

    @Nested
    @DisplayName("Bresenham 通视算法 - 障碍物测试")
    class BresenhamObstacleTests {

        @Test
        @DisplayName("有山峰阻挡时应不可见")
        void isLineOfSightBresenham_withMountain_shouldBeBlocked() {
            boolean visible = service.isLineOfSightBresenham(
                    0, 5, 10.0,
                    10, 5,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::mountainTerrain
            );
            assertFalse(visible, "山峰阻挡时应不可见");
        }

        @Test
        @DisplayName("观察者足够高时应能越过山峰")
        void isLineOfSightBresenham_highObserver_overMountain_shouldBeVisible() {
            boolean visible = service.isLineOfSightBresenham(
                    0, 5, 200.0,
                    10, 5,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::mountainTerrain
            );
            assertTrue(visible, "足够高的观察者应能越过山峰");
        }

        @Test
        @DisplayName("山脊阻挡视线应不可见")
        void isLineOfSightBresenham_ridgeBlocking_shouldBeBlocked() {
            boolean visible = service.isLineOfSightBresenham(
                    0, 5, 10.0,
                    10, 5,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::ridgeTerrain
            );
            assertFalse(visible, "山脊阻挡时应不可见");
        }
    }

    @Nested
    @DisplayName("Bresenham 通视算法 - 边界条件")
    class BresenhamBoundaryTests {

        @Test
        @DisplayName("边界条件：起点等于终点")
        void isLineOfSightBresenham_sameStartEnd_shouldBeVisible() {
            boolean visible = service.isLineOfSightBresenham(
                    5, 5, 10.0,
                    5, 5,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::flatTerrain
            );
            assertTrue(visible, "起点等于终点时应可见");
        }

        @Test
        @DisplayName("边界条件：观察者在目标正上方（短距离）")
        void isLineOfSightBresenham_observerAboveTarget_shouldBeVisible() {
            boolean visible = service.isLineOfSightBresenham(
                    5, 5, 100.0,
                    5, 5,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::flatTerrain
            );
            assertTrue(visible, "观察者在目标正上方应可见");
        }

        @Test
        @DisplayName("负方向：从右到左应正确计算")
        void isLineOfSightBresenham_rightToLeft_shouldWork() {
            boolean visible = service.isLineOfSightBresenham(
                    10, 5, 10.0,
                    0, 5,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::flatTerrain
            );
            assertTrue(visible, "从右到左视线应正确计算");
        }

        @Test
        @DisplayName("负方向：从下到上应正确计算")
        void isLineOfSightBresenham_bottomToTop_shouldWork() {
            boolean visible = service.isLineOfSightBresenham(
                    5, 10, 10.0,
                    5, 0,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::flatTerrain
            );
            assertTrue(visible, "从下到上视线应正确计算");
        }
    }

    @Nested
    @DisplayName("Bresenham 通视分析结果测试")
    class BresenhamResultTests {

        @Test
        @DisplayName("可见时应返回 visible=true 和正确距离")
        void analyzeLineOfSightBresenham_visible_shouldReturnCorrectResult() {
            LineOfSightResult result = service.analyzeLineOfSightBresenham(
                    0, 0, 10.0,
                    10, 0,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::flatTerrain
            );
            assertTrue(result.isVisible(), "应可见");
            assertEquals(10.0, result.getDistance(), 0.1, "距离应为10");
        }

        @Test
        @DisplayName("不可见时应返回阻挡点信息")
        void analyzeLineOfSightBresenham_blocked_shouldReturnBlockerInfo() {
            LineOfSightResult result = service.analyzeLineOfSightBresenham(
                    0, 5, 10.0,
                    10, 5,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::mountainTerrain
            );
            assertFalse(result.isVisible(), "应不可见");
            assertEquals(5, result.getBlockerX(), "阻挡点X坐标应为5");
            assertEquals(5, result.getBlockerY(), "阻挡点Y坐标应为5");
            assertTrue(result.getBlockerElevation() > 0, "阻挡点海拔应>0");
            assertTrue(result.getDistance() > 0, "阻挡距离应>0");
        }

        @Test
        @DisplayName("起点等于终点时距离应为0")
        void analyzeLineOfSightBresenham_samePoint_distanceShouldBeZero() {
            LineOfSightResult result = service.analyzeLineOfSightBresenham(
                    5, 5, 10.0,
                    5, 5,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::flatTerrain
            );
            assertTrue(result.isVisible());
            assertEquals(0.0, result.getDistance(), 0.001);
        }
    }

    @Nested
    @DisplayName("四叉树通视判断测试")
    class QuadtreeTests {

        private LineOfSightService.QuadtreeNode createFlatQuadtree(int size) {
            return new LineOfSightService.QuadtreeNode(
                    0, 0, size - 1, size - 1,
                    0.0, 0.0,
                    true
            );
        }

        private LineOfSightService.QuadtreeNode createMountainQuadtree(int size, int mountainX, int mountainY, double mountainHeight) {
            return new LineOfSightService.QuadtreeNode(
                    0, 0, size - 1, size - 1,
                    0.0, mountainHeight,
                    true
            );
        }

        @Test
        @DisplayName("平坦地形四叉树判断应可见")
        void isLineOfSightQuadtree_flatTerrain_shouldBeVisible() {
            LineOfSightService.QuadtreeNode root = createFlatQuadtree(16);
            boolean visible = service.isLineOfSightQuadtree(
                    root, 0, 0, 10.0,
                    15, 15,
                    EARTH_RADIUS, GRID_RESOLUTION
            );
            assertTrue(visible, "平坦地形四叉树应判断为可见");
        }

        @Test
        @DisplayName("四叉树根节点为null时应返回可见")
        void isLineOfSightQuadtree_nullRoot_shouldBeVisible() {
            boolean visible = service.isLineOfSightQuadtree(
                    null, 0, 0, 10.0,
                    10, 10,
                    EARTH_RADIUS, GRID_RESOLUTION
            );
            assertTrue(visible, "根节点为null时应可见");
        }

        @Test
        @DisplayName("起点等于终点时四叉树判断应可见")
        void isLineOfSightQuadtree_sameStartEnd_shouldBeVisible() {
            LineOfSightService.QuadtreeNode root = createFlatQuadtree(16);
            boolean visible = service.isLineOfSightQuadtree(
                    root, 5, 5, 10.0,
                    5, 5,
                    EARTH_RADIUS, GRID_RESOLUTION
            );
            assertTrue(visible, "同一点应可见");
        }

        @Test
        @DisplayName("四叉树分析结果应包含正确距离")
        void analyzeLineOfSightQuadtree_shouldReturnDistance() {
            LineOfSightService.QuadtreeNode root = createFlatQuadtree(16);
            LineOfSightResult result = service.analyzeLineOfSightQuadtree(
                    root, 0, 0, 10.0,
                    10, 0,
                    EARTH_RADIUS, GRID_RESOLUTION
            );
            assertNotNull(result);
            assertEquals(10.0, result.getDistance(), 0.1, "距离应约为10");
        }

        @Test
        @DisplayName("高障碍物四叉树应判断为不可见")
        void isLineOfSightQuadtree_withHighObstacle_shouldBeBlocked() {
            LineOfSightService.QuadtreeNode root = createMountainQuadtree(16, 8, 8, 200.0);
            boolean visible = service.isLineOfSightQuadtree(
                    root, 0, 8, 10.0,
                    15, 8,
                    EARTH_RADIUS, GRID_RESOLUTION
            );
            assertFalse(visible, "高障碍物应阻挡视线");
        }
    }

    @Nested
    @DisplayName("地球曲率影响测试")
    class EarthCurvatureTests {

        @Test
        @DisplayName("短距离地球曲率影响可忽略")
        void isLineOfSightBresenham_shortDistance_curvatureNegligible() {
            boolean visible = service.isLineOfSightBresenham(
                    0, 0, 2.0,
                    1000, 0,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::flatTerrain
            );
            assertTrue(visible, "短距离地球曲率不应阻挡视线");
        }

        @Test
        @DisplayName("地球曲率应降低有效视线高度")
        void analyzeLineOfSightBresenham_earthCurvature_shouldReduceLOS() {
            LineOfSightResult resultClose = service.analyzeLineOfSightBresenham(
                    0, 0, 10.0,
                    100, 0,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::flatTerrain
            );
            LineOfSightResult resultFar = service.analyzeLineOfSightBresenham(
                    0, 0, 10.0,
                    10000, 0,
                    EARTH_RADIUS, GRID_RESOLUTION,
                    this::flatTerrain
            );
            assertTrue(resultClose.isVisible(), "近距离应可见");
        }
    }

    @Nested
    @DisplayName("LineOfSightResult DTO 测试")
    class LineOfSightResultDtoTests {

        @Test
        @DisplayName("Builder 模式应正确构建对象")
        void builder_shouldBuildCorrectObject() {
            LineOfSightResult result = LineOfSightResult.builder()
                    .visible(true)
                    .distance(100.0)
                    .build();

            assertTrue(result.isVisible());
            assertEquals(100.0, result.getDistance(), 0.001);
        }

        @Test
        @DisplayName("阻挡信息应正确设置")
        void blockerInfo_shouldBeSettable() {
            LineOfSightResult result = LineOfSightResult.builder()
                    .visible(false)
                    .blockerX(5)
                    .blockerY(10)
                    .blockerElevation(50.0)
                    .distance(30.0)
                    .build();

            assertFalse(result.isVisible());
            assertEquals(5, result.getBlockerX());
            assertEquals(10, result.getBlockerY());
            assertEquals(50.0, result.getBlockerElevation(), 0.001);
            assertEquals(30.0, result.getDistance(), 0.001);
        }

        @Test
        @DisplayName("无参构造应创建默认值对象")
        void noArgsConstructor_shouldCreateDefault() {
            LineOfSightResult result = new LineOfSightResult();
            assertFalse(result.isVisible());
            assertEquals(0, result.getBlockerX());
            assertEquals(0, result.getBlockerY());
        }
    }
}
