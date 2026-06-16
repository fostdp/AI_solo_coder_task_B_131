package com.nestcart.modules.vr_nest_chariot.controller;

import com.nestcart.modules.vr_nest_chariot.dto.VirtualVisionResult;
import com.nestcart.modules.vr_nest_chariot.service.VirtualVisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vr-nest-chariot")
@RequiredArgsConstructor
public class VirtualRideController {

    private final VirtualVisionService virtualVisionService;

    @GetMapping("/horizon-distance")
    public ResponseEntity<Map<String, Double>> getHorizonDistance(
            @RequestParam(defaultValue = "15.0") Double heightMeters) {
        double distanceM = virtualVisionService.calculateHorizonDistance(heightMeters);
        Map<String, Double> result = new HashMap<>();
        result.put("heightMeters", heightMeters);
        result.put("horizonDistanceMeters", distanceM);
        result.put("horizonDistanceKm", distanceM / 1000.0);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/visible-area")
    public ResponseEntity<Map<String, Double>> getVisibleArea(
            @RequestParam(defaultValue = "15.0") Double heightMeters) {
        double areaSqKm = virtualVisionService.calculateVisibleArea(heightMeters);
        Map<String, Double> result = new HashMap<>();
        result.put("heightMeters", heightMeters);
        result.put("visibleAreaSqKm", areaSqKm);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/resolution")
    public ResponseEntity<Map<String, Double>> getResolutionAtDistance(
            @RequestParam(defaultValue = "1.0") Double distanceKm,
            @RequestParam(defaultValue = "15.0") Double observerHeightMeters) {
        double resolutionM = virtualVisionService.calculateResolutionAtDistance(distanceKm, observerHeightMeters);
        Map<String, Double> result = new HashMap<>();
        result.put("distanceKm", distanceKm);
        result.put("observerHeightMeters", observerHeightMeters);
        result.put("resolutionMeters", resolutionM);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/curvature-drop")
    public ResponseEntity<Map<String, Double>> getEarthCurvatureDrop(
            @RequestParam(defaultValue = "5.0") Double distanceKm,
            @RequestParam(defaultValue = "15.0") Double observerHeightMeters) {
        double dropM = virtualVisionService.calculateEarthCurvatureDrop(distanceKm, observerHeightMeters);
        Map<String, Double> result = new HashMap<>();
        result.put("distanceKm", distanceKm);
        result.put("observerHeightMeters", observerHeightMeters);
        result.put("curvatureDropMeters", dropM);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sway-displacement")
    public ResponseEntity<Map<String, Double>> getSwayDisplacement(
            @RequestParam(defaultValue = "15.0") Double heightMeters,
            @RequestParam(defaultValue = "5.0") Double windSpeedMs) {
        double displacementM = virtualVisionService.calculateSwayDisplacement(heightMeters, windSpeedMs);
        double swayAngle = virtualVisionService.calculateSwayAngle(windSpeedMs);
        Map<String, Double> result = new HashMap<>();
        result.put("heightMeters", heightMeters);
        result.put("windSpeedMs", windSpeedMs);
        result.put("swayDisplacementMeters", displacementM);
        result.put("swayAngleRadians", swayAngle);
        result.put("swayAngleDegrees", Math.toDegrees(swayAngle));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/vignette-intensity")
    public ResponseEntity<Map<String, Object>> getVignetteIntensity(
            @RequestParam(defaultValue = "15.0") Double heightMeters,
            @RequestParam(defaultValue = "false") Boolean acrophobiaProtection) {
        double intensity = virtualVisionService.calculateVignetteIntensity(heightMeters, acrophobiaProtection);
        Map<String, Object> result = new HashMap<>();
        result.put("heightMeters", heightMeters);
        result.put("acrophobiaProtection", acrophobiaProtection);
        result.put("vignetteIntensity", intensity);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/analyze")
    public ResponseEntity<VirtualVisionResult> analyzeVision(
            @RequestParam(defaultValue = "15.0") Double heightMeters,
            @RequestParam(defaultValue = "5.0") Double windSpeedMs,
            @RequestParam(defaultValue = "false") Boolean acrophobiaProtection) {
        VirtualVisionResult result = virtualVisionService.analyzeVisionAtHeight(
                heightMeters, windSpeedMs, acrophobiaProtection);
        return ResponseEntity.ok(result);
    }
}
