package com.nestcart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @deprecated 请使用 {@link com.nestcart.modules.evolution_analyzer.dto.DynastyEvolutionResult} 替代
 */
@Deprecated
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynastyEvolutionResult {

    private List<EvolutionPoint> evolutionPoints;

    private List<String> evolutionTrend;

    private Map<String, Double> performanceSummary;

    private List<DynastyComparisonRow> comparisonTable;

    /**
     * @deprecated 请使用 {@link com.nestcart.modules.evolution_analyzer.dto.DynastyEvolutionResult.EvolutionPoint} 替代
     */
    @Deprecated
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvolutionPoint {
        private UUID dynastyCartId;
        private String dynastyName;
        private String period;
        private String eraYear;
        private Double maxHeight;
        private Double observationDistance;
        private Double boomLength;
        private Double basketWeight;
        private Integer crewSize;
        private Double evolutionScore;
        private String innovationFeatures;
    }

    /**
     * @deprecated 请使用 {@link com.nestcart.modules.evolution_analyzer.dto.DynastyEvolutionResult.DynastyComparisonRow} 替代
     */
    @Deprecated
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DynastyComparisonRow {
        private String metric;
        private String unit;
        private Map<String, Object> valuesByDynasty;
    }
}
