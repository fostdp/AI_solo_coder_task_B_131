package com.nestcart.modules.coverage_optimizer.algorithm;

import com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCoverageOptimizationStrategy implements CoverageOptimizationStrategy {

    protected static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    protected double computeCoverage(List<CollaborativeCoverageRequest.CartSpec> carts, double w, double h, int res) {
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

    protected List<CollaborativeCoverageRequest.CartSpec> deepCopy(List<CollaborativeCoverageRequest.CartSpec> original) {
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
