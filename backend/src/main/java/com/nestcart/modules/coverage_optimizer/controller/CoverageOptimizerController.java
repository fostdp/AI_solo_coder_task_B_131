package com.nestcart.modules.coverage_optimizer.controller;

import com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageRequest;
import com.nestcart.modules.coverage_optimizer.dto.CollaborativeCoverageResult;
import com.nestcart.modules.coverage_optimizer.service.CoverageOptimizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/coverage-optimizer")
@RequiredArgsConstructor
public class CoverageOptimizerController {

    private final CoverageOptimizerService coverageOptimizerService;

    @PostMapping("/optimize")
    public ResponseEntity<CollaborativeCoverageResult> optimizeCoverage(
            @RequestBody CollaborativeCoverageRequest request) {
        return ResponseEntity.ok(coverageOptimizerService.optimizeCoverage(request));
    }

    @GetMapping("/coverage")
    public ResponseEntity<CollaborativeCoverageResult> getCoverage(
            @RequestParam(defaultValue = "default_battlefield") String region,
            @RequestParam(defaultValue = "2") Integer cartCount,
            @RequestParam(defaultValue = "10") Double widthKm,
            @RequestParam(defaultValue = "10") Double heightKm,
            @RequestParam(defaultValue = "greedy_spread") String strategy,
            @RequestParam(required = false) String fastMode,
            @RequestParam(defaultValue = "100") Integer maxIterations) {

        List<CollaborativeCoverageRequest.CartSpec> carts = new ArrayList<>();
        for (int i = 0; i < Math.max(1, cartCount); i++) {
            carts.add(CollaborativeCoverageRequest.CartSpec.builder()
                    .cartName("巢车-" + (char) ('A' + i))
                    .x(widthKm * (0.2 + 0.6 * (i / (double) cartCount)))
                    .y(heightKm * (0.2 + 0.6 * (i / (double) cartCount)))
                    .height(12.0)
                    .movable(true)
                    .build());
        }

        CollaborativeCoverageRequest request = CollaborativeCoverageRequest.builder()
                .region(region)
                .regionWidthKm(widthKm)
                .regionHeightKm(heightKm)
                .strategy(strategy)
                .maxIterations(maxIterations)
                .fastMode(fastMode)
                .carts(carts)
                .build();
        return ResponseEntity.ok(coverageOptimizerService.optimizeCoverage(request));
    }
}
