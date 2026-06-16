package com.nestcart.modules.coverage_optimizer.algorithm;

import com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest;

import java.util.List;

public class SimulatedAnnealingStrategy extends AbstractCoverageOptimizationStrategy {

    @Override
    public List<CollaborativeCoverageRequest.CartSpec> optimize(
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
}
