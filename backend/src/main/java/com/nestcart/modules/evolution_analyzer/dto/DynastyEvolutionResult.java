package com.nestcart.modules.evolution_analyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynastyEvolutionResult {

    private List<EvolutionPoint> evolutionPoints;

    private List<String> evolutionTrend;

    private Map<String, Double> performanceSummary;

    private List<DynastyComparisonRow> comparisonTable;

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
