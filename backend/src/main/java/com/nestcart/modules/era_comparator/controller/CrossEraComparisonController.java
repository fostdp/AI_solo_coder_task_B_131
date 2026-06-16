package com.nestcart.modules.era_comparator.controller;

import com.nestcart.modules.era_comparator.dto.CrossEraComparisonResult;
import com.nestcart.modules.era_comparator.entity.ModernDroneSpec;
import com.nestcart.modules.era_comparator.service.CrossEraComparisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/era-comparator")
@RequiredArgsConstructor
public class CrossEraComparisonController {

    private final CrossEraComparisonService crossEraComparisonService;

    @GetMapping("/drones")
    public ResponseEntity<List<ModernDroneSpec>> getAllDrones() {
        return ResponseEntity.ok(crossEraComparisonService.getAllDrones());
    }

    @GetMapping("/cross-era")
    public ResponseEntity<CrossEraComparisonResult> compareCrossEra() {
        return ResponseEntity.ok(crossEraComparisonService.compareCrossEra());
    }

    @GetMapping("/vs-drone")
    public ResponseEntity<CrossEraComparisonResult> compareCartVsDrone(
            @RequestParam UUID cartId,
            @RequestParam UUID droneId) {
        return ResponseEntity.ok(crossEraComparisonService.compareCartVsDrone(cartId, droneId));
    }
}
