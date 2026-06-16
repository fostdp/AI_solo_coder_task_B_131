package com.nestcart.collaborative;

import com.nestcart.dto.CollaborativeCoverageRequest;
import com.nestcart.dto.CollaborativeCoverageResult;
import com.nestcart.modules.coverage_optimizer.service.CoverageOptimizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Deprecated
@Service
@RequiredArgsConstructor
public class CollaborativeCoverageService {

    private final CoverageOptimizerService coverageOptimizerService;

    public CollaborativeCoverageResult optimizeCoverage(CollaborativeCoverageRequest request) {
        com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest newRequest = convertToNewRequest(request);
        com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageResult newResult = coverageOptimizerService.optimizeCoverage(newRequest);
        return convertToOldResult(newResult);
    }

    private com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest convertToNewRequest(CollaborativeCoverageRequest oldRequest) {
        if (oldRequest == null) return null;

        List<com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest.CartSpec> newCarts = null;
        if (oldRequest.getCarts() != null) {
            newCarts = oldRequest.getCarts().stream()
                    .map(this::convertToNewCartSpec)
                    .collect(Collectors.toList());
        }

        return com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest.builder()
                .region(oldRequest.getRegion())
                .carts(newCarts)
                .regionWidthKm(oldRequest.getRegionWidthKm())
                .regionHeightKm(oldRequest.getRegionHeightKm())
                .strategy(oldRequest.getStrategy())
                .maxIterations(oldRequest.getMaxIterations())
                .fastMode(oldRequest.getFastMode())
                .build();
    }

    private com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest.CartSpec convertToNewCartSpec(CollaborativeCoverageRequest.CartSpec oldCart) {
        if (oldCart == null) return null;
        return com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest.CartSpec.builder()
                .cartId(oldCart.getCartId())
                .cartName(oldCart.getCartName())
                .x(oldCart.getX())
                .y(oldCart.getY())
                .height(oldCart.getHeight())
                .visionRadiusKm(oldCart.getVisionRadiusKm())
                .movable(oldCart.getMovable())
                .build();
    }

    private CollaborativeCoverageResult convertToOldResult(com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageResult newResult) {
        if (newResult == null) return null;

        List<CollaborativeCoverageResult.CartPlacement> oldPlacements = null;
        if (newResult.getPlacements() != null) {
            oldPlacements = newResult.getPlacements().stream()
                    .map(this::convertToOldCartPlacement)
                    .collect(Collectors.toList());
        }

        CollaborativeCoverageResult.OptimizationMetrics oldMetrics = null;
        if (newResult.getOptimizationMetrics() != null) {
            oldMetrics = convertToOldOptimizationMetrics(newResult.getOptimizationMetrics());
        }

        List<double[]> heatmap = null;
        if (newResult.getCoverageHeatmap() != null) {
            heatmap = new ArrayList<>(newResult.getCoverageHeatmap());
        }

        return CollaborativeCoverageResult.builder()
                .region(newResult.getRegion())
                .totalAreaSqKm(newResult.getTotalAreaSqKm())
                .coveredAreaSqKm(newResult.getCoveredAreaSqKm())
                .coverageRatio(newResult.getCoverageRatio())
                .overlapAreaSqKm(newResult.getOverlapAreaSqKm())
                .overlapRatio(newResult.getOverlapRatio())
                .cartCount(newResult.getCartCount())
                .placements(oldPlacements)
                .blindZones(newResult.getBlindZones() != null ? new ArrayList<>(newResult.getBlindZones()) : null)
                .optimizationMetrics(oldMetrics)
                .coverageHeatmap(heatmap)
                .build();
    }

    private CollaborativeCoverageResult.CartPlacement convertToOldCartPlacement(com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageResult.CartPlacement newPlacement) {
        if (newPlacement == null) return null;
        return CollaborativeCoverageResult.CartPlacement.builder()
                .cartId(newPlacement.getCartId())
                .cartName(newPlacement.getCartName())
                .x(newPlacement.getX())
                .y(newPlacement.getY())
                .height(newPlacement.getHeight())
                .visionRadius(newPlacement.getVisionRadius())
                .individualCoverage(newPlacement.getIndividualCoverage())
                .build();
    }

    private CollaborativeCoverageResult.OptimizationMetrics convertToOldOptimizationMetrics(com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageResult.OptimizationMetrics newMetrics) {
        if (newMetrics == null) return null;
        return CollaborativeCoverageResult.OptimizationMetrics.builder()
                .iterations(newMetrics.getIterations())
                .initialCoverage(newMetrics.getInitialCoverage())
                .finalCoverage(newMetrics.getFinalCoverage())
                .coverageImprovement(newMetrics.getCoverageImprovement())
                .strategy(newMetrics.getStrategy())
                .computeTimeMs(newMetrics.getComputeTimeMs())
                .fastMode(newMetrics.getFastMode())
                .gridResolution(newMetrics.getGridResolution())
                .build();
    }
}
