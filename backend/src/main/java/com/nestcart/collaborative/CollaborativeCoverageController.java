package com.nestcart.collaborative;

import com.nestcart.dto.CollaborativeCoverageRequest;
import com.nestcart.dto.CollaborativeCoverageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/collaborative")
@RequiredArgsConstructor
public class CollaborativeCoverageController {

    private final CollaborativeCoverageService collaborativeCoverageService;

    @PostMapping("/optimize")
    public ResponseEntity<CollaborativeCoverageResult> optimizeCoverage(
            @RequestBody CollaborativeCoverageRequest request) {
        return ResponseEntity.ok(collaborativeCoverageService.optimizeCoverage(request));
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

        java.util.List<CollaborativeCoverageRequest.CartSpec> carts = new java.util.ArrayList<>();
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
        return ResponseEntity.ok(collaborativeCoverageService.optimizeCoverage(request));
    }
}
