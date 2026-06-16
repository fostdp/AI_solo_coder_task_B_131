package com.nestcart.modules.coverage_optimizer.algorithm;

import com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest;

import java.util.List;

public interface CoverageOptimizationStrategy {

    List<CollaborativeCoverageRequest.CartSpec> optimize(
            List<CollaborativeCoverageRequest.CartSpec> carts,
            double widthKm,
            double heightKm,
            int maxIterations,
            int gridResolution);
}
