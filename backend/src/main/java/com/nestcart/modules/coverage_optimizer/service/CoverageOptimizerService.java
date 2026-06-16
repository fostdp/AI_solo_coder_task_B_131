package com.nestcart.modules.coverage_optimizer.service;

import com.nestcart.modules.coverage_optimizer.algorithm.CoverageOptimizationStrategy;
import com.nestcart.modules.coverage_optimizer.algorithm.ForceDirectedStrategy;
import com.nestcart.modules.coverage_optimizer.algorithm.GreedySpreadStrategy;
import com.nestcart.modules.coverage_optimizer.algorithm.SimulatedAnnealingStrategy;
import com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest;
import com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageResult;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CoverageOptimizerService {

    private static final int STANDARD_GRID = 50;
    private static final int FAST_GRID = 25;
    private static final int HEATMAP_RESOLUTION = 50;
    private static final int BLINDZONE_RESOLUTION = 20;

    private final Map<String, CoverageOptimizationStrategy> strategies = new HashMap<>();

    public CoverageOptimizerService() {
        strategies.put("greedy_spread", new GreedySpreadStrategy());
        strategies.put("simulated_annealing", new SimulatedAnnealingStrategy());
        strategies.put("force_directed", new ForceDirectedStrategy());
    }

    public CollaborativeCoverageResult optimizeCoverage(CollaborativeCoverageRequest request) {
        long startTime = System.currentTimeMillis();

        String region = request.getRegion() != null ? request.getRegion() : "default_battlefield";
        double widthKm = request.getRegionWidthKm() != null ? request.getRegionWidthKm() : 10.0;
        double heightKm = request.getRegionHeightKm() != null ? request.getRegionHeightKm() : 10.0;
        int maxIter = request.getMaxIterations() != null ? request.getMaxIterations() : 100;
        String strategy = request.getStrategy() != null ? request.getStrategy() : "greedy_spread";

        boolean fastMode = "fast".equalsIgnoreCase(request.getFastMode())
                || request.getFastMode() == null && widthKm * heightKm > 400;

        int gridRes = fastMode ? FAST_GRID : STANDARD_GRID;
        maxIter = Math.min(maxIter, fastMode ? 80 : 200);

        List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();
        if (request.getCarts() != null) {
            for (CollaborativeCoverageRequest.CartSpec cs : request.getCarts()) {
                carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                        .cartId(cs.getCartId() != null ? cs.getCartId() : UUID.randomUUID())
                        .cartName(cs.getCartName())
                        .x(clamp(cs.getX() != null ? cs.getX() : widthKm / 2, 0, widthKm))
                        .y(clamp(cs.getY() != null ? cs.getY() : heightKm / 2, 0, heightKm))
                        .height(cs.getHeight() != null ? cs.getHeight() : 12.0)
                        .visionRadiusKm(cs.getVisionRadiusKm() != null ? cs.getVisionRadiusKm() : computeVisionRadius(cs.getHeight()))
                        .movable(cs.getMovable() != null ? cs.getMovable() : true)
                        .build());
            }
        }

        if (carts.isEmpty()) {
            carts.add(defaultCart(0, widthKm, heightKm, "A"));
            carts.add(defaultCart(1, widthKm, heightKm, "B"));
        }

        double initialCoverage = computeCoverage(carts, widthKm, heightKm, gridRes);

        CoverageOptimizationStrategy optimizationStrategy = strategies.getOrDefault(strategy, strategies.get("greedy_spread"));
        List<CollaborativeCoverageRequest.CartSpec> optimized = optimizationStrategy.optimize(carts, widthKm, heightKm, maxIter, gridRes);

        double finalCoverage = computeCoverage(optimized, widthKm, heightKm, gridRes);
        double overlap = computeOverlap(optimized, widthKm, heightKm, gridRes);

        List<CollaborativeCoverageResult.CartPlacement> placements = new ArrayList<>();
        for (CollaborativeCoverageRequest.CartSpec c : optimized) {
            double r = c.getVisionRadiusKm();
            placements.add(CollaborativeCoverageResult.CartPlacement.builder()
                    .cartId(c.getCartId())
                    .cartName(c.getCartName())
                    .x(c.getX())
                    .y(c.getY())
                    .height(c.getHeight())
                    .visionRadius(r)
                    .individualCoverage(Math.PI * r * r)
                    .build());
        }

        List<double[]> heatmap = buildCoverageHeatmap(optimized, widthKm, heightKm);
        List<String> blindZones = identifyBlindZones(optimized, widthKm, heightKm);

        long computeTime = System.currentTimeMillis() - startTime;

        return CollaborativeCoverageResult.builder()
                .region(region)
                .totalAreaSqKm(widthKm * heightKm)
                .coveredAreaSqKm(finalCoverage * widthKm * heightKm)
                .coverageRatio(finalCoverage)
                .overlapAreaSqKm(overlap)
                .overlapRatio(overlap / (widthKm * heightKm))
                .cartCount(optimized.size())
                .placements(placements)
                .blindZones(blindZones)
                .optimizationMetrics(CollaborativeCoverageResult.OptimizationMetrics.builder()
                        .iterations(maxIter)
                        .initialCoverage(initialCoverage)
                        .finalCoverage(finalCoverage)
                        .coverageImprovement(finalCoverage - initialCoverage)
                        .strategy(strategy)
                        .computeTimeMs(computeTime)
                        .fastMode(fastMode)
                        .gridResolution(gridRes)
                        .build())
                .coverageHeatmap(heatmap)
                .build();
    }

    private double computeVisionRadius(Double heightMeters) {
        double h = heightMeters != null ? heightMeters : 12.0;
        return Math.sqrt(2 * 6371000 * h) / 1000.0;
    }

    private CollaborativeCoverageRequest.CartSpec defaultCart(int idx, double w, double h, String name) {
        return CollaborativeCoverageRequest.CartSpec.builder()
                .cartId(UUID.randomUUID())
                .cartName("巢车-" + name)
                .x(w * (0.3 + 0.4 * idx))
                .y(h * (0.3 + 0.4 * idx))
                .height(12.0)
                .visionRadiusKm(computeVisionRadius(12.0))
                .movable(true)
                .build();
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private double computeCoverage(List<CollaborativeCoverageRequest.CartSpec> carts, double w, double h, int res) {
        int covered = 0;
        double cellW = w / res;
        double cellH = h / res;
        for (int i = 0; i < res; i++) {
            for (int j = 0; j < res; j++) {
                double px = (i + 0.5) * cellW;
                double py = (j + 0.5) * cellH;
                for (CollaborativeCoverageRequest.CartSpec c : carts) {
                    double dx = px - c.getX();
                    double dy = py - c.getY();
                    double r = c.getVisionRadiusKm();
                    if (dx * dx + dy * dy <= r * r) {
                        covered++;
                        break;
                    }
                }
            }
        }
        return (double) covered / (res * res);
    }

    private double computeOverlap(List<CollaborativeCoverageRequest.CartSpec> carts, double w, double h, int res) {
        int overlapCount = 0;
        double cellW = w / res;
        double cellH = h / res;
        for (int i = 0; i < res; i++) {
            for (int j = 0; j < res; j++) {
                double px = (i + 0.5) * cellW;
                double py = (j + 0.5) * cellH;
                int count = 0;
                for (CollaborativeCoverageRequest.CartSpec c : carts) {
                    double dx = px - c.getX();
                    double dy = py - c.getY();
                    double r = c.getVisionRadiusKm();
                    if (dx * dx + dy * dy <= r * r) {
                        count++;
                    }
                }
                if (count >= 2) {
                    overlapCount += (count - 1);
                }
            }
        }
        return (double) overlapCount * cellW * cellH;
    }

    private List<double[]> buildCoverageHeatmap(List<CollaborativeCoverageRequest.CartSpec> carts,
                                                 double w, double h) {
        List<double[]> heatmap = new ArrayList<>(HEATMAP_RESOLUTION * HEATMAP_RESOLUTION);
        double cellW = w / HEATMAP_RESOLUTION;
        double cellH = h / HEATMAP_RESOLUTION;
        for (int i = 0; i < HEATMAP_RESOLUTION; i++) {
            for (int j = 0; j < HEATMAP_RESOLUTION; j++) {
                double px = (i + 0.5) * cellW;
                double py = (j + 0.5) * cellH;
                int count = 0;
                for (CollaborativeCoverageRequest.CartSpec c : carts) {
                    double dx = px - c.getX();
                    double dy = py - c.getY();
                    double r = c.getVisionRadiusKm();
                    if (dx * dx + dy * dy <= r * r) {
                        count++;
                    }
                }
                heatmap.add(new double[]{px, py, count});
            }
        }
        return heatmap;
    }

    private List<String> identifyBlindZones(List<CollaborativeCoverageRequest.CartSpec> carts,
                                             double w, double h) {
        List<String> zones = new ArrayList<>();
        double cellW = w / BLINDZONE_RESOLUTION;
        double cellH = h / BLINDZONE_RESOLUTION;
        for (int i = 0; i < BLINDZONE_RESOLUTION; i++) {
            for (int j = 0; j < BLINDZONE_RESOLUTION; j++) {
                double px = (i + 0.5) * cellW;
                double py = (j + 0.5) * cellH;
                boolean covered = false;
                for (CollaborativeCoverageRequest.CartSpec c : carts) {
                    double dx = px - c.getX();
                    double dy = py - c.getY();
                    double r = c.getVisionRadiusKm() * 0.9;
                    if (dx * dx + dy * dy <= r * r) {
                        covered = true;
                        break;
                    }
                }
                if (!covered) {
                    zones.add(String.format(Locale.US, "盲区 [%.1f, %.1f] km 尺寸%.1f×%.1fkm", px, py, cellW, cellH));
                }
            }
        }
        return zones;
    }
}
