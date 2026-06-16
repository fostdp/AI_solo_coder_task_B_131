package com.nestcart.collaborative;

import com.nestcart.dto.CollaborativeCoverageRequest;
import com.nestcart.dto.CollaborativeCoverageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborativeCoverageService {

    private static final int GRID_RESOLUTION = 100;

    public CollaborativeCoverageResult optimizeCoverage(CollaborativeCoverageRequest request) {
        long startTime = System.currentTimeMillis();

        String region = request.getRegion() != null ? request.getRegion() : "default_battlefield";
        double widthKm = request.getRegionWidthKm() != null ? request.getRegionWidthKm() : 10.0;
        double heightKm = request.getRegionHeightKm() != null ? request.getRegionHeightKm() : 10.0;
        int maxIter = request.getMaxIterations() != null ? request.getMaxIterations() : 500;
        String strategy = request.getStrategy() != null ? request.getStrategy() : "greedy_spread";

        List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();
        if (request.getCarts() != null) {
            for (CollaborativeCoverageRequest.CartSpec cs : request.getCarts()) {
                carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                        .cartId(cs.getCartId() != null ? cs.getCartId() : UUID.randomUUID())
                        .cartName(cs.getCartName())
                        .x(cs.getX() != null ? cs.getX() : widthKm / 2)
                        .y(cs.getY() != null ? cs.getY() : heightKm / 2)
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

        double initialCoverage = computeCoverage(carts, widthKm, heightKm);

        List<CollaborativeCoverageRequest.CartSpec> optimized;
        if ("greedy_spread".equals(strategy)) {
            optimized = greedySpreadOptimization(carts, widthKm, heightKm, maxIter);
        } else if ("simulated_annealing".equals(strategy)) {
            optimized = simulatedAnnealingOptimization(carts, widthKm, heightKm, maxIter);
        } else if ("force_directed".equals(strategy)) {
            optimized = forceDirectedOptimization(carts, widthKm, heightKm, maxIter);
        } else {
            optimized = greedySpreadOptimization(carts, widthKm, heightKm, maxIter);
        }

        double finalCoverage = computeCoverage(optimized, widthKm, heightKm);
        double overlap = computeOverlap(optimized, widthKm, heightKm);

        List<CollaborativeCoverageResult.CartPlacement> placements = new ArrayList<>();
        for (CollaborativeCoverageRequest.CartSpec c : optimized) {
            double indCov = Math.PI * c.getVisionRadiusKm() * c.getVisionRadiusKm();
            placements.add(CollaborativeCoverageResult.CartPlacement.builder()
                    .cartId(c.getCartId())
                    .cartName(c.getCartName())
                    .x(c.getX())
                    .y(c.getY())
                    .height(c.getHeight())
                    .visionRadius(c.getVisionRadiusKm())
                    .individualCoverage(indCov)
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

    private double computeCoverage(List<CollaborativeCoverageRequest.CartSpec> carts, double w, double h) {
        int covered = 0;
        double cellW = w / GRID_RESOLUTION;
        double cellH = h / GRID_RESOLUTION;
        for (int i = 0; i < GRID_RESOLUTION; i++) {
            for (int j = 0; j < GRID_RESOLUTION; j++) {
                double px = (i + 0.5) * cellW;
                double py = (j + 0.5) * cellH;
                for (CollaborativeCoverageRequest.CartSpec c : carts) {
                    double dx = px - c.getX();
                    double dy = py - c.getY();
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist <= c.getVisionRadiusKm()) {
                        covered++;
                        break;
                    }
                }
            }
        }
        return (double) covered / (GRID_RESOLUTION * GRID_RESOLUTION);
    }

    private double computeOverlap(List<CollaborativeCoverageRequest.CartSpec> carts, double w, double h) {
        int overlapCount = 0;
        double cellW = w / GRID_RESOLUTION;
        double cellH = h / GRID_RESOLUTION;
        for (int i = 0; i < GRID_RESOLUTION; i++) {
            for (int j = 0; j < GRID_RESOLUTION; j++) {
                double px = (i + 0.5) * cellW;
                double py = (j + 0.5) * cellH;
                int count = 0;
                for (CollaborativeCoverageRequest.CartSpec c : carts) {
                    double dx = px - c.getX();
                    double dy = py - c.getY();
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist <= c.getVisionRadiusKm()) {
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

    private List<CollaborativeCoverageRequest.CartSpec> greedySpreadOptimization(
            List<CollaborativeCoverageRequest.CartSpec> carts, double w, double h, int iterations) {

        List<CollaborativeCoverageRequest.CartSpec> result = deepCopy(carts);
        double bestCoverage = computeCoverage(result, w, h);

        for (int iter = 0; iter < iterations; iter++) {
            boolean improved = false;
            for (int ci = 0; ci < result.size(); ci++) {
                if (!result.get(ci).getMovable()) continue;

                CollaborativeCoverageRequest.CartSpec original = result.get(ci);
                double origX = original.getX();
                double origY = original.getY();
                double step = Math.max(0.01, (w / GRID_RESOLUTION) * (1 - iter / (double) iterations));

                double[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
                for (double[] dir : directions) {
                    double newX = Math.max(0, Math.min(w, origX + dir[0] * step));
                    double newY = Math.max(0, Math.min(h, origY + dir[1] * step));
                    original.setX(newX);
                    original.setY(newY);
                    double cov = computeCoverage(result, w, h);
                    if (cov > bestCoverage) {
                        bestCoverage = cov;
                        improved = true;
                    } else {
                        original.setX(origX);
                        original.setY(origY);
                    }
                }
            }
            if (!improved) break;
        }
        return result;
    }

    private List<CollaborativeCoverageRequest.CartSpec> simulatedAnnealingOptimization(
            List<CollaborativeCoverageRequest.CartSpec> carts, double w, double h, int iterations) {

        List<CollaborativeCoverageRequest.CartSpec> result = deepCopy(carts);
        List<CollaborativeCoverageRequest.CartSpec> best = deepCopy(carts);
        double bestCoverage = computeCoverage(best, w, h);
        double currentCoverage = bestCoverage;

        double T0 = 0.1;
        for (int iter = 0; iter < iterations; iter++) {
            double T = T0 * (1 - iter / (double) iterations);
            for (int ci = 0; ci < result.size(); ci++) {
                if (!result.get(ci).getMovable()) continue;

                CollaborativeCoverageRequest.CartSpec c = result.get(ci);
                double oldX = c.getX();
                double oldY = c.getY();
                double step = Math.max(0.01, (w / 20) * (1 - iter / (double) iterations));
                double newX = Math.max(0, Math.min(w, oldX + (Math.random() - 0.5) * 2 * step));
                double newY = Math.max(0, Math.min(h, oldY + (Math.random() - 0.5) * 2 * step));
                c.setX(newX);
                c.setY(newY);
                double newCov = computeCoverage(result, w, h);
                double delta = newCov - currentCoverage;

                if (delta > 0 || Math.random() < Math.exp(delta / Math.max(T, 1e-6))) {
                    currentCoverage = newCov;
                    if (currentCoverage > bestCoverage) {
                        bestCoverage = currentCoverage;
                        best = deepCopy(result);
                    }
                } else {
                    c.setX(oldX);
                    c.setY(oldY);
                }
            }
        }
        return best;
    }

    private List<CollaborativeCoverageRequest.CartSpec> forceDirectedOptimization(
            List<CollaborativeCoverageRequest.CartSpec> carts, double w, double h, int iterations) {

        List<CollaborativeCoverageRequest.CartSpec> result = deepCopy(carts);

        for (int iter = 0; iter < iterations; iter++) {
            double[] fx = new double[result.size()];
            double[] fy = new double[result.size()];
            double k = Math.sqrt(w * h / Math.max(1, result.size())) * 0.8;

            for (int i = 0; i < result.size(); i++) {
                for (int j = 0; j < result.size(); j++) {
                    if (i == j) continue;
                    CollaborativeCoverageRequest.CartSpec ci = result.get(i);
                    CollaborativeCoverageRequest.CartSpec cj = result.get(j);
                    double dx = ci.getX() - cj.getX();
                    double dy = ci.getY() - cj.getY();
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < 0.01) dist = 0.01;
                    double repForce = k * k / dist;
                    fx[i] += (dx / dist) * repForce;
                    fy[i] += (dy / dist) * repForce;
                }
            }

            double cx = w / 2;
            double cy = h / 2;
            for (int i = 0; i < result.size(); i++) {
                CollaborativeCoverageRequest.CartSpec ci = result.get(i);
                double dx = cx - ci.getX();
                double dy = cy - ci.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 0.01) {
                    double attrForce = dist * 0.005;
                    fx[i] += (dx / dist) * attrForce;
                    fy[i] += (dy / dist) * attrForce;
                }
            }

            double cooling = 1.0 - iter / (double) iterations;
            for (int i = 0; i < result.size(); i++) {
                if (!result.get(i).getMovable()) continue;
                double disp = Math.sqrt(fx[i] * fx[i] + fy[i] * fy[i]);
                if (disp < 0.001) continue;
                double maxDisp = 0.1 * cooling;
                double clamped = Math.min(disp, maxDisp);
                CollaborativeCoverageRequest.CartSpec ci = result.get(i);
                double newX = Math.max(0, Math.min(w, ci.getX() + fx[i] / disp * clamped));
                double newY = Math.max(0, Math.min(h, ci.getY() + fy[i] / disp * clamped));
                ci.setX(newX);
                ci.setY(newY);
            }
        }
        return result;
    }

    private List<double[]> buildCoverageHeatmap(List<CollaborativeCoverageRequest.CartSpec> carts,
                                                 double w, double h) {
        List<double[]> heatmap = new ArrayList<>();
        int resolution = 50;
        double cellW = w / resolution;
        double cellH = h / resolution;
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                double px = (i + 0.5) * cellW;
                double py = (j + 0.5) * cellH;
                int count = 0;
                for (CollaborativeCoverageRequest.CartSpec c : carts) {
                    double dx = px - c.getX();
                    double dy = py - c.getY();
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist <= c.getVisionRadiusKm()) {
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
        int resolution = 20;
        double cellW = w / resolution;
        double cellH = h / resolution;
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                double px = (i + 0.5) * cellW;
                double py = (j + 0.5) * cellH;
                boolean covered = false;
                for (CollaborativeCoverageRequest.CartSpec c : carts) {
                    double dx = px - c.getX();
                    double dy = py - c.getY();
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist <= c.getVisionRadiusKm() * 0.9) {
                        covered = true;
                        break;
                    }
                }
                if (!covered) {
                    zones.add(String.format("盲区 [%.1f, %.1f] km 尺寸%.1f×%.1fkm", px, py, cellW, cellH));
                }
            }
        }
        return zones;
    }

    private List<CollaborativeCoverageRequest.CartSpec> deepCopy(List<CollaborativeCoverageRequest.CartSpec> original) {
        List<CollaborativeCoverageRequest.CartSpec> copy = new ArrayList<>();
        for (CollaborativeCoverageRequest.CartSpec c : original) {
            copy.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartId(c.getCartId())
                    .cartName(c.getCartName())
                    .x(c.getX())
                    .y(c.getY())
                    .height(c.getHeight())
                    .visionRadiusKm(c.getVisionRadiusKm())
                    .movable(c.getMovable())
                    .build());
        }
        return copy;
    }
}
