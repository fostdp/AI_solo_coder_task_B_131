package com.nestcart.collaborative;

import com.nestcart.dto.CollaborativeCoverageRequest;
import com.nestcart.dto.CollaborativeCoverageResult;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CollaborativeCoverageService {

    private static final int STANDARD_GRID = 50;
    private static final int FAST_GRID = 25;
    private static final int HEATMAP_RESOLUTION = 50;
    private static final int BLINDZONE_RESOLUTION = 20;

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

        List<CollaborativeCoverageRequest.CartSpec> optimized;
        if ("simulated_annealing".equals(strategy)) {
            optimized = simulatedAnnealingOptimization(carts, widthKm, heightKm, maxIter, gridRes);
        } else if ("force_directed".equals(strategy)) {
            optimized = forceDirectedOptimization(carts, widthKm, heightKm, maxIter);
        } else {
            optimized = greedySpreadOptimization(carts, widthKm, heightKm, maxIter, gridRes);
        }

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

    private List<CollaborativeCoverageRequest.CartSpec> greedySpreadOptimization(
            List<CollaborativeCoverageRequest.CartSpec> carts, double w, double h, int iterations, int gridRes) {

        List<CollaborativeCoverageRequest.CartSpec> result = deepCopy(carts);
        double bestCoverage = computeCoverage(result, w, h, gridRes);

        double step = Math.max(w / 20.0, 0.2);
        final double minStep = Math.max(w / 200.0, 0.02);

        for (int iter = 0; iter < iterations; iter++) {
            boolean improved = false;
            for (int ci = 0; ci < result.size(); ci++) {
                if (!result.get(ci).getMovable()) continue;

                CollaborativeCoverageRequest.CartSpec c = result.get(ci);
                double origX = c.getX();
                double origY = c.getY();

                double[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
                for (double[] d : dirs) {
                    double newX = clamp(origX + d[0] * step, 0, w);
                    double newY = clamp(origY + d[1] * step, 0, h);
                    if (newX == origX && newY == origY) continue;
                    c.setX(newX);
                    c.setY(newY);
                    double cov = computeCoverage(result, w, h, gridRes);
                    if (cov > bestCoverage + 1e-6) {
                        bestCoverage = cov;
                        improved = true;
                        origX = newX;
                        origY = newY;
                    } else {
                        c.setX(origX);
                        c.setY(origY);
                    }
                }
            }
            if (!improved) {
                step *= 0.5;
                if (step < minStep) break;
            }
        }
        return result;
    }

    private List<CollaborativeCoverageRequest.CartSpec> simulatedAnnealingOptimization(
            List<CollaborativeCoverageRequest.CartSpec> carts, double w, double h, int iterations, int gridRes) {

        List<CollaborativeCoverageRequest.CartSpec> result = deepCopy(carts);
        List<CollaborativeCoverageRequest.CartSpec> best = deepCopy(carts);
        double bestCoverage = computeCoverage(best, w, h, gridRes);
        double currentCoverage = bestCoverage;

        double T0 = 0.05;
        for (int iter = 0; iter < iterations; iter++) {
            double T = T0 * Math.pow(0.995, iter);
            for (int ci = 0; ci < result.size(); ci++) {
                if (!result.get(ci).getMovable()) continue;

                CollaborativeCoverageRequest.CartSpec c = result.get(ci);
                double oldX = c.getX();
                double oldY = c.getY();
                double step = Math.max(w / 20.0, 0.1) * (0.3 + 0.7 * (1 - iter / (double) iterations));
                double newX = clamp(oldX + (Math.random() - 0.5) * 2 * step, 0, w);
                double newY = clamp(oldY + (Math.random() - 0.5) * 2 * step, 0, h);
                if (newX == oldX && newY == oldY) continue;
                c.setX(newX);
                c.setY(newY);
                double newCov = computeCoverage(result, w, h, gridRes);
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
            if (iter % 10 == 0 && bestCoverage > 0.995) break;
        }
        return best;
    }

    private List<CollaborativeCoverageRequest.CartSpec> forceDirectedOptimization(
            List<CollaborativeCoverageRequest.CartSpec> carts, double w, double h, int iterations) {

        List<CollaborativeCoverageRequest.CartSpec> result = deepCopy(carts);
        double k = Math.sqrt(w * h / Math.max(1, result.size())) * 0.8;
        double cx = w / 2;
        double cy = h / 2;

        for (int iter = 0; iter < iterations; iter++) {
            double[] fx = new double[result.size()];
            double[] fy = new double[result.size()];

            for (int i = 0; i < result.size(); i++) {
                CollaborativeCoverageRequest.CartSpec ci = result.get(i);
                for (int j = i + 1; j < result.size(); j++) {
                    CollaborativeCoverageRequest.CartSpec cj = result.get(j);
                    double dx = ci.getX() - cj.getX();
                    double dy = ci.getY() - cj.getY();
                    double dist2 = dx * dx + dy * dy;
                    if (dist2 < 1e-4) dist2 = 1e-4;
                    double dist = Math.sqrt(dist2);
                    double repForce = (k * k) / dist;
                    double fxComp = (dx / dist) * repForce;
                    double fyComp = (dy / dist) * repForce;
                    fx[i] += fxComp;
                    fy[i] += fyComp;
                    fx[j] -= fxComp;
                    fy[j] -= fyComp;
                }
            }

            for (int i = 0; i < result.size(); i++) {
                CollaborativeCoverageRequest.CartSpec ci = result.get(i);
                double dx = cx - ci.getX();
                double dy = cy - ci.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 1e-4) {
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
                double maxDisp = Math.max(0.1 * cooling, 0.02);
                double clamped = Math.min(disp, maxDisp);
                CollaborativeCoverageRequest.CartSpec ci = result.get(i);
                ci.setX(clamp(ci.getX() + fx[i] / disp * clamped, 0, w));
                ci.setY(clamp(ci.getY() + fy[i] / disp * clamped, 0, h));
            }

            if (iter % 5 == 0) {
                double totalDisp = 0;
                for (int i = 0; i < fx.length; i++) totalDisp += Math.sqrt(fx[i] * fx[i] + fy[i] * fy[i]);
                if (totalDisp < 0.001 * fx.length) break;
            }
        }
        return result;
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

    private List<CollaborativeCoverageRequest.CartSpec> deepCopy(List<CollaborativeCoverageRequest.CartSpec> original) {
        List<CollaborativeCoverageRequest.CartSpec> copy = new ArrayList<>(original.size());
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
