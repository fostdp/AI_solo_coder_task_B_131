package com.nestcart.modules.visibility_compute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineOfSightResult {

    private boolean visible;

    private int blockerX;

    private int blockerY;

    private double blockerElevation;

    private double distance;
}
