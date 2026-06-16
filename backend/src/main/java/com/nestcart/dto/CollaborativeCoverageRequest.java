package com.nestcart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaborativeCoverageRequest {

    private String region;

    private List<CartSpec> carts;

    private Double regionWidthKm;

    private Double regionHeightKm;

    private String strategy;

    private Integer maxIterations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartSpec {
        private UUID cartId;
        private String cartName;
        private Double x;
        private Double y;
        private Double height;
        private Double visionRadiusKm;
        private Boolean movable;
    }
}
