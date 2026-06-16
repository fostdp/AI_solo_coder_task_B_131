package com.nestcart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @deprecated 请使用 {@link com.nestcart.modules.era_comparator.dto.CrossEraComparisonResult} 替代
 */
@Deprecated
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossEraComparisonResult {

    private EraSummary ancientSummary;

    private EraSummary modernSummary;

    private List<ComparisonDimension> comparisonDimensions;

    private List<String> insights;

    /**
     * @deprecated 请使用 {@link com.nestcart.modules.era_comparator.dto.CrossEraComparisonResult.EraSummary} 替代
     */
    @Deprecated
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EraSummary {
        private String era;
        private String platformName;
        private Double maxAltitudeMeters;
        private Double maxRangeKm;
        private Double enduranceHours;
        private Double crewSize;
        private Double costUsd;
        private Double setupTimeMinutes;
        private Map<String, Double> capabilityScores;
    }

    /**
     * @deprecated 请使用 {@link com.nestcart.modules.era_comparator.dto.CrossEraComparisonResult.ComparisonDimension} 替代
     */
    @Deprecated
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonDimension {
        private String dimension;
        private String unit;
        private String category;
        private Double ancientValue;
        private Double modernValue;
        private Double ancientScore;
        private Double modernScore;
        private String advantage;
        private String commentary;
    }
}
