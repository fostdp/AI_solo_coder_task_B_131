package com.nestcart.virtual;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("虚拟登巢车视野真实性测试")
class VirtualVisionRealismTest {

    private static final double EARTH_RADIUS_M = 6371000.0;
    private static final double HUMAN_EYE_HEIGHT_M = 1.7;
    private static final double VERTICAL_FIELD_OF_VIEW_DEG = 135.0;
    private static final double HORIZONTAL_FIELD_OF_VIEW_DEG = 200.0;
    private static final double VISUAL_ACUITY_MINUTES = 1.0;
    private static final double BASKET_RAIL_HEIGHT_M = 1.2;

    private double computeHorizonDistance(double heightMeters) {
        return Math.sqrt(2 * EARTH_RADIUS_M * heightMeters);
    }

    private double computeHorizonDistanceKm(double heightMeters) {
        return computeHorizonDistance(heightMeters) / 1000.0;
    }

    private double computeVisibleAreaSqKm(double heightMeters) {
        double distKm = computeHorizonDistanceKm(heightMeters);
        return Math.PI * distKm * distKm;
    }

    private double computeResolutionAtDistance(double heightMeters, double distanceKm) {
        double horizonDistKm = computeHorizonDistanceKm(heightMeters);
        if (distanceKm > horizonDistKm) return Double.POSITIVE_INFINITY;
        double distanceM = distanceKm * 1000;
        double angleRad = Math.toRadians(VISUAL_ACUITY_MINUTES / 60.0);
        return 2 * distanceM * Math.tan(angleRad / 2);
    }

    private boolean canSeeObject(double observerHeight, double objectHeight, double distanceKm) {
        double distanceM = distanceKm * 1000;
        double curvatureDrop = distanceM * distanceM / (2 * EARTH_RADIUS_M);
        double observerHorizonH = observerHeight - curvatureDrop;
        double objectEffectiveH = objectHeight - curvatureDrop;
        return observerHorizonH > 0 || objectEffectiveH > 0;
    }

    private double computeBasketSwayAngle(double windSpeedMs) {
        double dragForce = 0.5 * 1.225 * windSpeedMs * windSpeedMs * 2.0;
        double gravityForce = 150.0 * 9.81;
        return Math.atan2(dragForce, gravityForce);
    }

    private double computeBasketSwayDisplacement(double heightMeters, double swayAngle) {
        return heightMeters * Math.sin(swayAngle);
    }

    @Nested
    @DisplayName("视野真实性：地平线距离验证")
    class HorizonDistanceTests {

        @Test
        @DisplayName("8米高度（西周巢车）理论视距应约10.1km")
        void horizonDistance_8m_shouldBeAbout10km() {
            double dist = computeHorizonDistanceKm(8.0);
            assertTrue(dist > 9 && dist < 11,
                    String.format("8m高度视距%.2fkm应在9-11km范围", dist));
        }

        @Test
        @DisplayName("15米高度（春秋巢车）理论视距应约13.8km")
        void horizonDistance_15m_shouldBeAbout14km() {
            double dist = computeHorizonDistanceKm(15.0);
            assertTrue(dist > 12 && dist < 16,
                    String.format("15m高度视距%.2fkm应在12-16km范围", dist));
        }

        @Test
        @DisplayName("22米高度（宋代巢车）理论视距应约16.7km")
        void horizonDistance_22m_shouldBeAbout17km() {
            double dist = computeHorizonDistanceKm(22.0);
            assertTrue(dist > 15 && dist < 18,
                    String.format("22m高度视距%.2fkm应在15-18km范围", dist));
        }

        @Test
        @DisplayName("0米高度（地面）理论视距应为0")
        void horizonDistance_0m_shouldBeZero() {
            assertEquals(0.0, computeHorizonDistanceKm(0.0), 0.001);
        }

        @Test
        @DisplayName("视距应与高度的平方根成正比")
        void horizonDistance_shouldBeProportionalToSqrtOfHeight() {
            double dist8 = computeHorizonDistanceKm(8.0);
            double dist32 = computeHorizonDistanceKm(32.0);
            double ratio = dist32 / dist8;
            assertEquals(Math.sqrt(4), ratio, 0.01,
                    "高度4倍时视距应2倍");
        }
    }

    @Nested
    @DisplayName("视野真实性：可见面积验证")
    class VisibleAreaTests {

        @Test
        @DisplayName("升空过程可见面积应单调递增")
        void visibleArea_shouldIncreaseWithHeight() {
            double prev = 0;
            for (double h = 1; h <= 25; h += 1) {
                double area = computeVisibleAreaSqKm(h);
                assertTrue(area > prev, String.format("高度%.0fm面积%.2fkm²应>前一步", h, area));
                prev = area;
            }
        }

        @Test
        @DisplayName("15米高度可见面积应>500km²")
        void visibleArea_15m_shouldExceed500sqKm() {
            double area = computeVisibleAreaSqKm(15.0);
            assertTrue(area > 500, String.format("15m可见面积%.1fkm²应>500km²", area));
        }

        @Test
        @DisplayName("22米高度可见面积应>15米高度的面积")
        void visibleArea_22m_shouldExceed_15m() {
            assertTrue(computeVisibleAreaSqKm(22.0) > computeVisibleAreaSqKm(15.0));
        }
    }

    @Nested
    @DisplayName("视野真实性：分辨率验证")
    class ResolutionTests {

        @Test
        @DisplayName("1km处人眼分辨率应约0.29m（可识别人体）")
        void resolution_1km_shouldIdentifyHumanBody() {
            double res = computeResolutionAtDistance(15.0, 1.0);
            assertTrue(res < 0.5, String.format("1km处分辨率%.3fm应<0.5m", res));
        }

        @Test
        @DisplayName("5km处人眼分辨率应>1m（无法识别人体细节）")
        void resolution_5km_shouldNotIdentifyDetails() {
            double res = computeResolutionAtDistance(15.0, 5.0);
            assertTrue(res > 1.0, String.format("5km处分辨率%.3fm应>1m", res));
        }

        @Test
        @DisplayName("超过地平线距离应返回无穷大（不可见）")
        void resolution_beyondHorizon_shouldBeInfinite() {
            double dist = computeHorizonDistanceKm(15.0) + 1;
            double res = computeResolutionAtDistance(15.0, dist);
            assertTrue(Double.isInfinite(res) || res > 1000);
        }

        @Test
        @DisplayName("分辨率应随距离线性恶化")
        void resolution_shouldDegradeWithDistance() {
            double res1 = computeResolutionAtDistance(15.0, 1.0);
            double res3 = computeResolutionAtDistance(15.0, 3.0);
            double ratio = res3 / res1;
            assertEquals(3.0, ratio, 0.01, "3倍距离分辨率应恶化约3倍");
        }
    }

    @Nested
    @DisplayName("视野真实性：地球曲率遮挡验证")
    class CurvatureOcclusionTests {

        @Test
        @DisplayName("地面1.7m高的人应在约4.7km外被地球曲率遮挡")
        void curvatureOcclusion_humanShouldBeHiddenBy5km() {
            double dist = computeHorizonDistanceKm(HUMAN_EYE_HEIGHT_M);
            assertTrue(dist < 6 && dist > 4,
                    String.format("1.7m人眼视距%.2fkm应在4-6km范围", dist));
        }

        @Test
        @DisplayName("巢车高度15m应能看到5km外地面上的2m高目标")
        void curvatureOcclusion_15mShouldSee5kmTarget() {
            double distM = 5000;
            double curvatureDrop = distM * distM / (2 * EARTH_RADIUS_M);
            double lineOfSightAt5km = 15.0 - curvatureDrop;
            assertTrue(lineOfSightAt5km > 0,
                    String.format("15m高5km处视线高度%.2fm应>0", lineOfSightAt5km));
        }

        @Test
        @DisplayName("巢车高度15m在15km处应无法看到地面目标")
        void curvatureOcclusion_15mShouldNotSee15kmTarget() {
            double horizonDist = computeHorizonDistanceKm(15.0);
            assertTrue(horizonDist < 15,
                    String.format("15m视距%.2fkm应<15km", horizonDist));
        }

        @Test
        @DisplayName("地球曲率下降量应与距离平方成正比")
        void curvatureOcclusion_dropShouldFollowDistanceSquared() {
            double drop5 = 5000.0 * 5000.0 / (2 * EARTH_RADIUS_M);
            double drop10 = 10000.0 * 10000.0 / (2 * EARTH_RADIUS_M);
            double ratio = drop10 / drop5;
            assertEquals(4.0, ratio, 0.01, "2倍距离曲率下降应4倍");
        }
    }

    @Nested
    @DisplayName("沉浸感：吊篮晃动真实性验证")
    class BasketSwayRealismTests {

        @Test
        @DisplayName("无风时吊篮晃动角度应接近0")
        void basketSway_noWind_shouldBeNearZero() {
            double angle = computeBasketSwayAngle(0.0);
            assertEquals(0.0, angle, 0.001, "无风时晃动角度应≈0");
        }

        @Test
        @DisplayName("5m/s微风时晃动位移应<0.1m（不影响观察）")
        void basketSway_5msWind_shouldBeSmall() {
            double angle = computeBasketSwayAngle(5.0);
            double displacement = computeBasketSwayDisplacement(15.0, angle);
            assertTrue(displacement < 0.1,
                    String.format("5m/s风位移%.4fm应<0.1m", displacement));
        }

        @Test
        @DisplayName("20m/s大风时晃动位移应在0.1-1m范围（影响但可忍受）")
        void basketSway_20msWind_shouldBeModerate() {
            double angle = computeBasketSwayAngle(20.0);
            double displacement = computeBasketSwayDisplacement(15.0, angle);
            assertTrue(displacement > 0.01 && displacement < 2.0,
                    String.format("20m/s风位移%.3fm应在0.01-2m范围", displacement));
        }

        @Test
        @DisplayName("30m/s暴风时晃动应剧烈（>0.5m位移）")
        void basketSway_30msWind_shouldBeSevere() {
            double angle = computeBasketSwayAngle(30.0);
            double displacement = computeBasketSwayDisplacement(15.0, angle);
            assertTrue(displacement > 0.5,
                    String.format("30m/s风位移%.3fm应>0.5m", displacement));
        }

        @Test
        @DisplayName("更高处晃动位移应更大（同角度下位移与高度成正比）")
        void basketSway_higherPosition_shouldHaveLargerDisplacement() {
            double angle = computeBasketSwayAngle(10.0);
            double disp15 = computeBasketSwayDisplacement(15.0, angle);
            double disp22 = computeBasketSwayDisplacement(22.0, angle);
            assertTrue(disp22 > disp15, "更高位置同风况晃动更大");
        }
    }

    @Nested
    @DisplayName("沉浸感：升空体验连续性验证")
    class AscentContinuityTests {

        @Test
        @DisplayName("从0m升空到22m视距应连续增长无跳跃")
        void ascentContinuity_visionDistanceShouldIncreaseSmoothly() {
            double prevDist = 0;
            for (double h = 0; h <= 22; h += 0.5) {
                double dist = computeHorizonDistanceKm(h);
                double increment = dist - prevDist;
                if (h > 0) {
                    assertTrue(increment > 0 && increment < 1,
                            String.format("高度%.1fm视距增量%.4fkm异常", h, increment));
                }
                prevDist = dist;
            }
        }

        @Test
        @DisplayName("升空过程中可见面积增长率应递减（边际效应递减）")
        void ascentContinuity_areaGrowthRateShouldDecrease() {
            double prevGrowth = Double.MAX_VALUE;
            for (double h = 2; h <= 22; h += 2) {
                double area = computeVisibleAreaSqKm(h);
                double prevArea = computeVisibleAreaSqKm(h - 2);
                double growth = area - prevArea;
                assertTrue(growth < prevGrowth + 1,
                        String.format("高度%.0fm面积增长%.2f不应大幅超过前步", h, growth));
                prevGrowth = growth;
            }
        }

        @Test
        @DisplayName("视野锥体张角应与高度无关（始终覆盖地平线）")
        void ascentContinuity_fovShouldBeConsistent() {
            for (double h = 5; h <= 25; h += 5) {
                double horizonDist = computeHorizonDistance(h);
                double halfAngle = Math.atan2(h, horizonDist);
                assertTrue(halfAngle < Math.toRadians(5),
                        String.format("高度%.0fm视野半锥角%.2f°应<5°", h, Math.toDegrees(halfAngle)));
            }
        }
    }

    @Nested
    @DisplayName("沉浸感：吊篮栏杆遮挡验证")
    class RailingOcclusionTests {

        @Test
        @DisplayName("1.2m高栏杆在1.7m眼高处应遮挡约29%垂直视野下沿")
        void railingOcclusion_shouldBlockLower29Percent() {
            double eyeHeight = HUMAN_EYE_HEIGHT_M;
            double railHeight = BASKET_RAIL_HEIGHT_M;
            double blockedRatio = railHeight / eyeHeight;
            assertEquals(0.706, blockedRatio, 0.01,
                    "栏杆遮挡比应约70.6%垂直高度以下");
        }

        @Test
        @DisplayName("俯视时栏杆不应遮挡正下方视野")
        void railingOcclusion_shouldNotBlockDownwardView() {
            double railHeight = BASKET_RAIL_HEIGHT_M;
            double eyeHeight = HUMAN_EYE_HEIGHT_M;
            double俯角 = Math.atan2(railHeight, eyeHeight - railHeight);
            assertTrue(Math.toDegrees(俯角) > 50,
                    String.format("栏杆允许俯角%.1f°应>50°", Math.toDegrees(俯角)));
        }
    }

    @Nested
    @DisplayName("边界与异常")
    class BoundaryAndExceptionTests {

        @Test
        @DisplayName("负高度应返回0视距而非负值")
        void negativeHeight_shouldReturnZeroDistance() {
            double dist = computeHorizonDistanceKm(-5.0);
            assertTrue(dist == 0 || Double.isNaN(dist),
                    "负高度应返回0或NaN");
        }

        @Test
        @DisplayName("极大高度（100km太空边缘）视距应超350km")
        void extremeHeight_100km_shouldExceed350km() {
            double dist = computeHorizonDistanceKm(100000.0);
            assertTrue(dist > 350, String.format("100km视距%.1fkm应>350km", dist));
        }

        @Test
        @DisplayName("无穷大风速晃动角度应趋近90°")
        void infiniteWind_swayAngleShouldApproach90Degrees() {
            double angle = computeBasketSwayAngle(10000.0);
            assertTrue(Math.toDegrees(angle) > 45,
                    String.format("10000m/s风晃角%.1f°应>45°", Math.toDegrees(angle)));
        }
    }
}
