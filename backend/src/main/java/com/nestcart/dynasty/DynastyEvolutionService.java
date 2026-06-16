package com.nestcart.dynasty;

import com.nestcart.dto.DynastyEvolutionResult;
import com.nestcart.entity.DynastyCart;
import com.nestcart.repository.DynastyCartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @deprecated 请使用 {@link com.nestcart.modules.evolution_analyzer.service.DynastyEvolutionService} 替代
 */
@Deprecated
@Service
@RequiredArgsConstructor
@Slf4j
public class DynastyEvolutionService {

    private final DynastyCartRepository dynastyCartRepository;

    public List<DynastyCart> getAllDynastyCarts() {
        return dynastyCartRepository.findAllByOrderBySortOrderAsc();
    }

    public DynastyEvolutionResult analyzeEvolution() {
        List<DynastyCart> carts = getAllDynastyCarts();

        List<DynastyEvolutionResult.EvolutionPoint> points = buildEvolutionPoints(carts);
        List<String> trends = analyzeTrends(carts);
        Map<String, Double> summary = computePerformanceSummary(carts);
        List<DynastyEvolutionResult.DynastyComparisonRow> comparison = buildComparisonTable(carts);

        return DynastyEvolutionResult.builder()
                .evolutionPoints(points)
                .evolutionTrend(trends)
                .performanceSummary(summary)
                .comparisonTable(comparison)
                .build();
    }

    private List<DynastyEvolutionResult.EvolutionPoint> buildEvolutionPoints(List<DynastyCart> carts) {
        List<DynastyEvolutionResult.EvolutionPoint> points = new ArrayList<>();
        for (DynastyCart cart : carts) {
            double obsDist = cart.getMaxHeight() != null ? Math.sqrt(2 * 6371000 * cart.getMaxHeight()) : 0;
            points.add(DynastyEvolutionResult.EvolutionPoint.builder()
                    .dynastyCartId(cart.getId())
                    .dynastyName(cart.getDynastyName())
                    .period(cart.getPeriod())
                    .eraYear(cart.getEraYear())
                    .maxHeight(cart.getMaxHeight())
                    .observationDistance(obsDist)
                    .boomLength(cart.getBoomLength())
                    .basketWeight(cart.getBasketWeight())
                    .crewSize(cart.getCrewSize())
                    .evolutionScore(cart.getEvolutionScore())
                    .innovationFeatures(cart.getInnovationFeatures())
                    .build());
        }
        return points;
    }

    private List<String> analyzeTrends(List<DynastyCart> carts) {
        List<String> trends = new ArrayList<>();

        if (carts.size() < 2) {
            return trends;
        }

        DynastyCart first = carts.get(0);
        DynastyCart last = carts.get(carts.size() - 1);

        double h1 = first.getMaxHeight() != null ? first.getMaxHeight() : 0;
        double h2 = last.getMaxHeight() != null ? last.getMaxHeight() : 0;
        double b1 = first.getBoomLength() != null ? first.getBoomLength() : 0;
        double b2 = last.getBoomLength() != null ? last.getBoomLength() : 0;
        double s1 = first.getEvolutionScore() != null ? first.getEvolutionScore() : 0;
        double s2 = last.getEvolutionScore() != null ? last.getEvolutionScore() : 0;

        if (h1 > 0) {
            double heightGrowth = (h2 - h1) / h1 * 100;
            trends.add(String.format("观察高度提升 %.0f%%，从 %.1f米 → %.1f米", heightGrowth, h1, h2));
        }
        if (b1 > 0) {
            double boomGrowth = (b2 - b1) / b1 * 100;
            trends.add(String.format("悬臂长度增长 %.0f%%，从 %.1f米 → %.1f米", boomGrowth, b1, b2));
        }
        if (s1 > 0) {
            double scoreGrowth = (s2 - s1) / s1 * 100;
            trends.add(String.format("综合技术评分提升 %.0f%%", scoreGrowth));
        }

        trends.add("结构材料：从原木捆绑 → 榫卯结构 → 金属加固 → 复合结构的演变趋势明显");
        trends.add("军事功能：从单纯瞭望 → 侦察指挥 → 火攻指挥一体化");

        return trends;
    }

    private Map<String, Double> computePerformanceSummary(List<DynastyCart> carts) {
        Map<String, Double> summary = new LinkedHashMap<>();

        double avgHeight = carts.stream()
                .mapToDouble(c -> c.getMaxHeight() != null ? c.getMaxHeight() : 0)
                .average().orElse(0);
        double avgBoom = carts.stream()
                .mapToDouble(c -> c.getBoomLength() != null ? c.getBoomLength() : 0)
                .average().orElse(0);
        double avgScore = carts.stream()
                .mapToDouble(c -> c.getEvolutionScore() != null ? c.getEvolutionScore() : 0)
                .average().orElse(0);
        double avgCrew = carts.stream()
                .mapToDouble(c -> c.getCrewSize() != null ? c.getCrewSize() : 0)
                .average().orElse(0);

        summary.put("avgHeight", avgHeight);
        summary.put("avgBoomLength", avgBoom);
        summary.put("avgEvolutionScore", avgScore);
        summary.put("avgCrewSize", avgCrew);
        summary.put("dynastyCount", (double) carts.size());

        return summary;
    }

    private List<DynastyEvolutionResult.DynastyComparisonRow> buildComparisonTable(List<DynastyCart> carts) {
        List<DynastyEvolutionResult.DynastyComparisonRow> rows = new ArrayList<>();

        rows.add(buildRow("最大高度", "米", carts, DynastyCart::getMaxHeight));
        rows.add(buildRow("悬臂长度", "米", carts, DynastyCart::getBoomLength));
        rows.add(buildRow("吊篮承重", "公斤", carts, DynastyCart::getBasketWeight));
        rows.add(buildRow("乘员人数", "人", carts, DynastyCart::getCrewSize));
        rows.add(buildRow("综合评分", "分", carts, DynastyCart::getEvolutionScore));

        return rows;
    }

    private DynastyEvolutionResult.DynastyComparisonRow buildRow(String metric, String unit,
                                                               List<DynastyCart> carts,
                                                               java.util.function.Function<DynastyCart, Double> extractor) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (DynastyCart cart : carts) {
            values.put(cart.getDynastyName(), extractor.apply(cart));
        }
        return DynastyEvolutionResult.DynastyComparisonRow.builder()
                .metric(metric)
                .unit(unit)
                .valuesByDynasty(values)
                .build();
    }
}
