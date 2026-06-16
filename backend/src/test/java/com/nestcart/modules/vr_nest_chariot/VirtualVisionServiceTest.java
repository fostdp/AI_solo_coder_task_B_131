package com.nestcart.modules.vr_nest_chariot;

import com.nestcart.modules.vr_nest_chariot.dto.VirtualVisionResult;
import com.nestcart.modules.vr_nest_chariot.service.VirtualVisionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VirtualVisionService 单元测试")
class VirtualVisionServiceTest {

    private final VirtualVisionService service = new VirtualVisionService();

    private static final double EARTH_RADIUS_M = 6371000.0;
    private static final double HUMAN_EYE_ANGULAR_RESOLUTION_RAD = 0.0003;

    @Nested
    @DisplayName("地平线距离测试")
    class HorizonDistanceTests {

        @Test
        @DisplayName("正常高度：8米高度视距应约10.1km")
        void calculateHorizonDistance_8m_shouldBeAbout10km() {
            double dist = service.calculateHorizonDistance(8.0);
            double distKm = dist / 1000.0;
            assertTrue(distKm > 9 && distKm < 11,
                    String.format("8m高度视距%.2fkm应在9-11km范围", distKm));
        }

        @Test
        @DisplayName("正常高度：15米高度视距应约13.8km")
        void calculateHorizonDistance_15m_shouldBeAbout14km() {
            double dist = service.calculateHorizonDistance(15.0);
            double distKm = dist / 1000.0;
            assertTrue(distKm > 12 && distKm < 16,
                    String.format("15m高度视距%.2fkm应在12-16km范围", distKm));
        }

        @Test
        @DisplayName("边界条件：0米高度视距应为0")
        void calculateHorizonDistance_0m_shouldBeZero() {
            assertEquals(0.0, service.calculateHorizonDistance(0.0), 0.001);
        }

        @Test
        @DisplayName("异常场景：负高度应返回0")
        void calculateHorizonDistance_negativeHeight_shouldReturnZero() {
            assertEquals(0.0, service.calculateHorizonDistance(-5.0), 0.001);
        }

        @Test
        @DisplayName("比例关系：视距应与高度的平方根成正比")
        void calculateHorizonDistance_shouldBeProportionalToSqrtOfHeight() {
            double dist8 = service.calculateHorizonDistance(8.0);
            double dist32 = service.calculateHorizonDistance(32.0);
            double ratio = dist32 / dist8;
            assertEquals(2.0, ratio, 0.01, "高度4倍时视距应2倍");
        }
    }

    @Nested
    @DisplayName("可见面积测试")
    class VisibleAreaTests {

        @Test
        @DisplayName("正常场景：15米高度可见面积应>500km²")
        void calculateVisibleArea_15m_shouldExceed500sqKm() {
            double area = service.calculateVisibleArea(15.0);
            assertTrue(area > 500, String.format("15m可见面积%.1fkm²应>500km²", area));
        }

        @Test
        @DisplayName("单调性：可见面积应随高度递增")
        void calculateVisibleArea_shouldIncreaseWithHeight() {
            double prev = 0;
            for (double h = 1; h <= 25; h += 1) {
                double area = service.calculateVisibleArea(h);
                assertTrue(area > prev, String.format("高度%.0fm面积%.2fkm²应>前一步", h, area));
                prev = area;
            }
        }

        @Test
        @DisplayName("边界条件：0米高度可见面积应为0")
        void calculateVisibleArea_0m_shouldBeZero() {
            assertEquals(0.0, service.calculateVisibleArea(0.0), 0.001);
        }
    }

    @Nested
    @DisplayName("分辨率测试")
    class ResolutionTests {

        @Test
        @DisplayName("正常场景：1km处分辨率应约0.3m")
        void calculateResolutionAtDistance_1km_shouldBeAbout03m() {
            double res = service.calculateResolutionAtDistance(1.0, 15.0);
            assertTrue(res < 0.5, String.format("1km处分辨率%.3fm应<0.5m", res));
        }

        @Test
        @DisplayName("异常场景：超过地平线距离应返回无穷大")
        void calculateResolutionAtDistance_beyondHorizon_shouldBeInfinite() {
            double horizonDistKm = service.calculateHorizonDistance(15.0) / 1000.0;
            double res = service.calculateResolutionAtDistance(horizonDistKm + 1, 15.0);
            assertTrue(Double.isInfinite(res), "超过地平线距离应返回无穷大");
        }

        @Test
        @DisplayName("比例关系：分辨率应随距离线性变化")
        void calculateResolutionAtDistance_shouldDegradeLinearlyWithDistance() {
            double res1 = service.calculateResolutionAtDistance(1.0, 15.0);
            double res3 = service.calculateResolutionAtDistance(3.0, 15.0);
            double ratio = res3 / res1;
            assertEquals(3.0, ratio, 0.01, "3倍距离分辨率应约3倍");
        }
    }

    @Nested
    @DisplayName("地球曲率测试")
    class EarthCurvatureTests {

        @Test
        @DisplayName("正常场景：1km处曲率下降应约0.078m")
        void calculateEarthCurvatureDrop_1km_shouldBeAbout008m() {
            double drop = service.calculateEarthCurvatureDrop(1.0, 15.0);
            double expected = 1000.0 * 1000.0 / (2 * EARTH_RADIUS_M);
            assertEquals(expected, drop, 0.001);
        }

        @Test
        @DisplayName("比例关系：曲率下降量应与距离平方成正比")
        void calculateEarthCurvatureDrop_shouldFollowDistanceSquared() {
            double drop5 = service.calculateEarthCurvatureDrop(5.0, 10.0);
            double drop10 = service.calculateEarthCurvatureDrop(10.0, 10.0);
            double ratio = drop10 / drop5;
            assertEquals(4.0, ratio, 0.01, "2倍距离曲率下降应4倍");
        }

        @Test
        @DisplayName("边界条件：0距离曲率下降应为0")
        void calculateEarthCurvatureDrop_0distance_shouldBeZero() {
            assertEquals(0.0, service.calculateEarthCurvatureDrop(0.0, 10.0), 0.001);
        }
    }

    @Nested
    @DisplayName("晃动位移测试")
    class SwayDisplacementTests {

        @Test
        @DisplayName("正常场景：无风时晃动位移应为0")
        void calculateSwayDisplacement_noWind_shouldBeZero() {
            double displacement = service.calculateSwayDisplacement(15.0, 0.0);
            assertEquals(0.0, displacement, 0.001, "无风时晃动位移应≈0");
        }

        @Test
        @DisplayName("正常场景：5m/s微风时晃动位移应很小")
        void calculateSwayDisplacement_5msWind_shouldBeSmall() {
            double displacement = service.calculateSwayDisplacement(15.0, 5.0);
            assertTrue(displacement < 0.1,
                    String.format("5m/s风位移%.4fm应<0.1m", displacement));
        }

        @Test
        @DisplayName("比例关系：更高处晃动位移应更大")
        void calculateSwayDisplacement_higherPosition_shouldBeLarger() {
            double disp15 = service.calculateSwayDisplacement(15.0, 10.0);
            double disp22 = service.calculateSwayDisplacement(22.0, 10.0);
            assertTrue(disp22 > disp15, "更高位置同风况晃动更大");
        }

        @Test
        @DisplayName("晃动角度：与风速平方正相关")
        void calculateSwayAngle_shouldIncreaseWithWindSpeedSquared() {
            double angle5 = service.calculateSwayAngle(5.0);
            double angle10 = service.calculateSwayAngle(10.0);
            assertTrue(angle10 > angle5, "风速越大晃动角度越大");
        }
    }

    @Nested
    @DisplayName("暗角强度测试")
    class VignetteIntensityTests {

        @Test
        @DisplayName("正常场景：低高度暗角强度应较小")
        void calculateVignetteIntensity_lowHeight_shouldBeLow() {
            double intensity = service.calculateVignetteIntensity(5.0, false);
            assertTrue(intensity < 0.5, String.format("5m暗角强度%.2f应<0.5", intensity));
        }

        @Test
        @DisplayName("正常场景：高高度暗角强度应较大")
        void calculateVignetteIntensity_highHeight_shouldBeHigher() {
            double intensity = service.calculateVignetteIntensity(50.0, false);
            assertTrue(intensity > 0.3, String.format("50m暗角强度%.2f应>0.3", intensity));
        }

        @Test
        @DisplayName("恐高保护：启用恐高保护且高度超过阈值时暗角应更强")
        void calculateVignetteIntensity_acrophobiaProtection_shouldIncreaseVignette() {
            double withoutProtection = service.calculateVignetteIntensity(30.0, false);
            double withProtection = service.calculateVignetteIntensity(30.0, true);
            assertTrue(withProtection > withoutProtection,
                    "启用恐高保护时暗角应更强");
        }

        @Test
        @DisplayName("恐高保护：高度低于阈值时保护不生效")
        void calculateVignetteIntensity_belowProtectionThreshold_shouldBeSame() {
            double withoutProtection = service.calculateVignetteIntensity(5.0, false);
            double withProtection = service.calculateVignetteIntensity(5.0, true);
            assertEquals(withoutProtection, withProtection, 0.001,
                    "低于保护阈值时暗角强度应相同");
        }

        @Test
        @DisplayName("边界条件：0米高度暗角强度应为基础值")
        void calculateVignetteIntensity_0m_shouldBeBaseValue() {
            double intensity = service.calculateVignetteIntensity(0.0, false);
            assertEquals(0.1, intensity, 0.001, "0m高度暗角应为基础值0.1");
        }
    }

    @Nested
    @DisplayName("综合分析测试")
    class AnalyzeVisionTests {

        @Test
        @DisplayName("综合分析：应返回完整的视野分析结果")
        void analyzeVisionAtHeight_shouldReturnCompleteResult() {
            VirtualVisionResult result = service.analyzeVisionAtHeight(15.0, 5.0, true);

            assertNotNull(result);
            assertEquals(15.0, result.getObserverHeightMeters(), 0.001);
            assertNotNull(result.getHorizonDistanceKm());
            assertNotNull(result.getVisibleAreaSqKm());
            assertNotNull(result.getResolutionAtDistanceMeters());
            assertNotNull(result.getEarthCurvatureDropMeters());
            assertNotNull(result.getSwayDisplacementMeters());
            assertNotNull(result.getSwayAngleRadians());
            assertNotNull(result.getVignetteIntensity());
            assertTrue(result.getAcrophobiaProtectionEnabled());
            assertEquals(5.0, result.getWindSpeedMs(), 0.001);
            assertNotNull(result.getComputeTimeMs());
        }

        @Test
        @DisplayName("综合分析：地平线距离应与单独计算一致")
        void analyzeVisionAtHeight_horizonDistanceShouldMatch() {
            double height = 20.0;
            VirtualVisionResult result = service.analyzeVisionAtHeight(height, 0.0, false);
            double expected = service.calculateHorizonDistance(height) / 1000.0;
            assertEquals(expected, result.getHorizonDistanceKm(), 0.001);
        }

        @Test
        @DisplayName("综合分析：可见面积应与单独计算一致")
        void analyzeVisionAtHeight_visibleAreaShouldMatch() {
            double height = 20.0;
            VirtualVisionResult result = service.analyzeVisionAtHeight(height, 0.0, false);
            double expected = service.calculateVisibleArea(height);
            assertEquals(expected, result.getVisibleAreaSqKm(), 0.001);
        }

        @Test
        @DisplayName("综合分析：计算时间应为非负值")
        void analyzeVisionAtHeight_computeTimeShouldBeNonNegative() {
            VirtualVisionResult result = service.analyzeVisionAtHeight(10.0, 3.0, false);
            assertTrue(result.getComputeTimeMs() >= 0, "计算时间应>=0");
        }

        @Test
        @DisplayName("综合分析：边界条件0高度")
        void analyzeVisionAtHeight_0height_shouldHandleGracefully() {
            VirtualVisionResult result = service.analyzeVisionAtHeight(0.0, 0.0, false);
            assertNotNull(result);
            assertEquals(0.0, result.getHorizonDistanceKm(), 0.001);
            assertEquals(0.0, result.getVisibleAreaSqKm(), 0.001);
        }
    }
}
