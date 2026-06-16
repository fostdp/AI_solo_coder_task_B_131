package com.nestcart.modules.visibility_compute.service;

import com.nestcart.dto.VisionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisibilityComputeService {

    private final LineOfSightService lineOfSightService;

    @Async("visibilityComputeExecutor")
    public CompletableFuture<VisionResult> analyzeVisionAsync(
            UUID cartId, String regionName,
            int observerGridX, int observerGridY, double observerHeight,
            int gridSizeX, int gridSizeY, double analysisRadius,
            double resolution, double earthRadius, int sectorCount,
            boolean useQuadtree, LineOfSightService.QuadtreeNode quadtreeRoot,
            BiFunction<Integer, Integer, Double> elevationGetter) {

        log.debug("开始异步视野分析: cartId={}, region={}", cartId, regionName);

        double horizonDistance = Math.sqrt(2 * earthRadius * observerHeight + observerHeight * observerHeight);
        double effectiveRadius = Math.min(analysisRadius, horizonDistance);

        int radiusInCells = (int) Math.ceil(effectiveRadius / resolution);
        radiusInCells = Math.min(radiusInCells, Math.min(gridSizeX / 2, gridSizeY / 2));

        int visiblePoints = 0;
        int totalPoints = 0;
        double maxVisibleDistance = 0;
        List<VisionResult.GridPoint> visiblePointList = new ArrayList<>();
        List<VisionResult.GridPoint> blockedPointList = new ArrayList<>();

        List<VisionResult.SectorAnalysis> sectors = new ArrayList<>();

        for (int s = 0; s < sectorCount; s++) {
            double azimuth = s * (360.0 / sectorCount);
            double minElev = Double.MAX_VALUE;
            double maxElev = Double.MIN_VALUE;
            double sectorVisibleDist = 0;

            int steps = radiusInCells;
            for (int step = 1; step <= steps; step++) {
                double dist = step * resolution;
                double angleRad = Math.toRadians(azimuth);
                int targetX = observerGridX + (int) Math.round(step * Math.cos(angleRad));
                int targetY = observerGridY + (int) Math.round(step * Math.sin(angleRad));

                if (targetX < 0 || targetX >= gridSizeX || targetY < 0 || targetY >= gridSizeY) {
                    break;
                }

                totalPoints++;
                boolean isVisible;

                if (useQuadtree && quadtreeRoot != null) {
                    isVisible = lineOfSightService.isLineOfSightQuadtree(
                            quadtreeRoot, observerGridX, observerGridY, observerHeight,
                            targetX, targetY, earthRadius, resolution);
                } else {
                    isVisible = lineOfSightService.isLineOfSightBresenham(
                            observerGridX, observerGridY, observerHeight,
                            targetX, targetY, earthRadius, resolution,
                            elevationGetter);
                }

                double elev = elevationGetter.apply(targetX, targetY);
                minElev = Math.min(minElev, elev);
                maxElev = Math.max(maxElev, elev);

                if (isVisible) {
                    visiblePoints++;
                    sectorVisibleDist = dist;
                    maxVisibleDistance = Math.max(maxVisibleDistance, dist);
                    if (visiblePointList.size() < 5000) {
                        visiblePointList.add(new VisionResult.GridPoint(targetX, targetY, elev));
                    }
                } else {
                    if (blockedPointList.size() < 5000) {
                        blockedPointList.add(new VisionResult.GridPoint(targetX, targetY, elev));
                    }
                }
            }

            sectors.add(VisionResult.SectorAnalysis.builder()
                    .azimuth(azimuth)
                    .visibleDistance(sectorVisibleDist)
                    .isBlocked(sectorVisibleDist < effectiveRadius * 0.9)
                    .minElevation(minElev == Double.MAX_VALUE ? 0 : minElev)
                    .maxElevation(maxElev == Double.MIN_VALUE ? 0 : maxElev)
                    .build());
        }

        double coverageRatio = totalPoints > 0 ? (double) visiblePoints / totalPoints : 0;

        VisionResult result = VisionResult.builder()
                .cartId(cartId)
                .observerHeight(observerHeight)
                .analysisRadius(effectiveRadius)
                .regionName(regionName)
                .theoreticalHorizon(horizonDistance)
                .maxVisibleDistance(maxVisibleDistance)
                .visiblePoints(visiblePoints)
                .totalPoints(totalPoints)
                .coverageRatio(coverageRatio)
                .visiblePointList(visiblePointList)
                .blockedPointList(blockedPointList)
                .sectorAnalysis(sectors)
                .build();

        log.debug("异步视野分析完成: cartId={}, 可见点={}, 覆盖率={}", cartId, visiblePoints, coverageRatio);

        return CompletableFuture.completedFuture(result);
    }

    @Async("visibilityComputeExecutor")
    public CompletableFuture<VisionResult> analyzeVisionSimpleAsync(
            UUID cartId, String regionName,
            int observerGridX, int observerGridY, double observerHeight,
            int gridSizeX, int gridSizeY, double analysisRadius,
            double resolution, double earthRadius, int sectorCount,
            BiFunction<Integer, Integer, Double> elevationGetter) {

        return analyzeVisionAsync(
                cartId, regionName,
                observerGridX, observerGridY, observerHeight,
                gridSizeX, gridSizeY, analysisRadius,
                resolution, earthRadius, sectorCount,
                false, null, elevationGetter);
    }
}
