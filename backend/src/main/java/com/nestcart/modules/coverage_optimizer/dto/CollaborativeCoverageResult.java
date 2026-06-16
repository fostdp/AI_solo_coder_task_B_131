package com.nestcart.modules.coverage_optimizer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaborativeCoverageResult {

    private String region;

    private Double totalAreaSqKm;

    private Double coveredAreaSqKm;

    private Double coverageRatio;

    private Double overlapAreaSqKm;

    private Double overlapRatio;

    private Integer cartCount;

    private List<CartPlacement> placements;

    private List<String> blindZones;

    private OptimizationMetrics optimizationMetrics;

    private List<double[]> coverageHeatmap;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartPlacement {
        private UUID cartId;
        private String cartName;
        private Double x;
        private Double y;
        private Double height;
        private Double visionRadius;
        private Double individualCoverage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptimizationMetrics {
        private Integer iterations;
        private Double initialCoverage;
        private Double finalCoverage;
        private Double coverageImprovement;
        private String strategy;
        private Long computeTimeMs;
        private Boolean fastMode;
        private Integer gridResolution;
    }
}
