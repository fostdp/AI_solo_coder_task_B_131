package com.nestcart.modules.vr_nest_chariot.service;

import com.nestcart.modules.vr_nest_chariot.dto.VirtualVisionResult;
import org.springframework.stereotype.Service;

@Service
public class VirtualVisionService {

    private static final double EARTH_RADIUS_M = 6371000.0;
    private static final double HUMAN_EYE_ANGULAR_RESOLUTION_RAD = 0.0003;
    private static final double ACROPHOBIA_PROTECTION_MAX_HEIGHT_M = 10.0;
    private static final double BASKET_SWAY_NATURAL_FREQUENCY = 0.0005;

    private static final double AIR_DENSITY = 1.225;
    private static final double BASKET_DRAG_AREA = 2.0;
    private static final double BASKET_MASS_KG = 150.0;
    private static final double GRAVITY = 9.81;

    public double calculateHorizonDistance(double heightMeters) {
        if (heightMeters <= 0) {
            return 0.0;
        }
        return Math.sqrt(2 * EARTH_RADIUS_M * heightMeters);
    }

    public double calculateVisibleArea(double heightMeters) {
        double distKm = calculateHorizonDistance(heightMeters) / 1000.0;
        return Math.PI * distKm * distKm;
    }

    public double calculateResolutionAtDistance(double distanceKm, double observerHeightMeters) {
        double horizonDistKm = calculateHorizonDistance(observerHeightMeters) / 1000.0;
        if (distanceKm > horizonDistKm) {
            return Double.POSITIVE_INFINITY;
        }
        double distanceM = distanceKm * 1000.0;
        return 2 * distanceM * Math.tan(HUMAN_EYE_ANGULAR_RESOLUTION_RAD / 2);
    }

    public double calculateEarthCurvatureDrop(double distanceKm, double observerHeightMeters) {
        double distanceM = distanceKm * 1000.0;
        return distanceM * distanceM / (2 * EARTH_RADIUS_M);
    }

    public double calculateSwayDisplacement(double heightMeters, double windSpeedMs) {
        double swayAngle = calculateSwayAngle(windSpeedMs);
        return heightMeters * Math.sin(swayAngle);
    }

    public double calculateSwayAngle(double windSpeedMs) {
        double dragForce = 0.5 * AIR_DENSITY * windSpeedMs * windSpeedMs * BASKET_DRAG_AREA;
        double gravityForce = BASKET_MASS_KG * GRAVITY;
        return Math.atan2(dragForce, gravityForce);
    }

    public double calculateVignetteIntensity(double heightMeters, boolean acrophobiaProtection) {
        double baseVignette = 0.1;
        double heightFactor = Math.min(heightMeters / 50.0, 1.0);

        if (acrophobiaProtection && heightMeters > ACROPHOBIA_PROTECTION_MAX_HEIGHT_M) {
            double excessHeight = heightMeters - ACROPHOBIA_PROTECTION_MAX_HEIGHT_M;
            double protectionFactor = Math.min(excessHeight / 20.0, 1.0);
            return baseVignette + heightFactor * 0.3 + protectionFactor * 0.4;
        }

        return baseVignette + heightFactor * 0.3;
    }

    public VirtualVisionResult analyzeVisionAtHeight(double heightMeters, double windSpeedMs, boolean acrophobiaProtection) {
        long startTime = System.currentTimeMillis();

        double horizonDistanceM = calculateHorizonDistance(heightMeters);
        double horizonDistanceKm = horizonDistanceM / 1000.0;
        double visibleAreaSqKm = calculateVisibleArea(heightMeters);
        double resolutionAt1Km = calculateResolutionAtDistance(1.0, heightMeters);
        double curvatureDropAt1Km = calculateEarthCurvatureDrop(1.0, heightMeters);
        double swayDisplacement = calculateSwayDisplacement(heightMeters, windSpeedMs);
        double swayAngle = calculateSwayAngle(windSpeedMs);
        double vignetteIntensity = calculateVignetteIntensity(heightMeters, acrophobiaProtection);

        long computeTime = System.currentTimeMillis() - startTime;

        return VirtualVisionResult.builder()
                .observerHeightMeters(heightMeters)
                .horizonDistanceKm(horizonDistanceKm)
                .visibleAreaSqKm(visibleAreaSqKm)
                .resolutionAtDistanceMeters(resolutionAt1Km)
                .earthCurvatureDropMeters(curvatureDropAt1Km)
                .swayDisplacementMeters(swayDisplacement)
                .swayAngleRadians(swayAngle)
                .vignetteIntensity(vignetteIntensity)
                .acrophobiaProtectionEnabled(acrophobiaProtection)
                .windSpeedMs(windSpeedMs)
                .computeTimeMs(computeTime)
                .build();
    }
}
