package com.nestcart.comparison;

import com.nestcart.dto.CrossEraComparisonResult;
import com.nestcart.entity.ModernDroneSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comparison")
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
