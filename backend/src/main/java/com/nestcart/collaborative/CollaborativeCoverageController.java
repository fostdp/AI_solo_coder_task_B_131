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
            @RequestParam(defaultValue = "greedy_spread") String strategy) {

        CollaborativeCoverageRequest request = CollaborativeCoverageRequest.builder()
                .region(region)
                .regionWidthKm(widthKm)
                .regionHeightKm(heightKm)
                .strategy(strategy)
                .maxIterations(300)
                .build();
        return ResponseEntity.ok(collaborativeCoverageService.optimizeCoverage(request));
    }
}
