package com.nestcart.modules.coverage_optimizer.algorithm;

import com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest;

import java.util.List;

public class GreedySpreadStrategy extends AbstractCoverageOptimizationStrategy {

    @Override
    public List<CollaborativeCoverageRequest.CartSpec> optimize(
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
}
