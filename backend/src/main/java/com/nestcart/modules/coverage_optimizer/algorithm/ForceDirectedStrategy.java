package com.nestcart.modules.coverage_optimizer.algorithm;

import com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest;

import java.util.List;

public class ForceDirectedStrategy extends AbstractCoverageOptimizationStrategy {

    @Override
    public List<CollaborativeCoverageRequest.CartSpec> optimize(
            List<CollaborativeCoverageRequest.CartSpec> carts, double w, double h, int iterations, int gridRes) {

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
}
