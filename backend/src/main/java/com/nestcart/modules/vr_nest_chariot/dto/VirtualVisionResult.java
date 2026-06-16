package com.nestcart.modules.vr_nest_chariot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualVisionResult {

    private Double observerHeightMeters;

    private Double horizonDistanceKm;

    private Double visibleAreaSqKm;

    private Double resolutionAtDistanceMeters;

    private Double earthCurvatureDropMeters;

    private Double swayDisplacementMeters;

    private Double swayAngleRadians;

    private Double vignetteIntensity;

    private Boolean acrophobiaProtectionEnabled;

    private Double windSpeedMs;

    private Long computeTimeMs;
}
