package com.nestcart.modules.era_comparator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "modern_drone_spec")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModernDroneSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "year_introduced")
    private Integer yearIntroduced;

    @Column(name = "max_flight_altitude_m")
    private Double maxFlightAltitudeMeters;

    @Column(name = "max_ceiling_m")
    private Double maxCeilingMeters;

    @Column(name = "max_flight_range_km")
    private Double maxFlightRangeKm;

    @Column(name = "flight_endurance_minutes")
    private Double flightEnduranceMinutes;

    @Column(name = "cruise_speed_kmh")
    private Double cruiseSpeedKmh;

    @Column(name = "max_speed_kmh")
    private Double maxSpeedKmh;

    @Column(name = "camera_resolution_mp")
    private Double cameraResolutionMp;

    @Column(name = "optical_zoom")
    private Double opticalZoom;

    @Column(name = "thermal_camera", nullable = false)
    private Boolean thermalCamera;

    @Column(name = "thermal_resolution_mp")
    private Double thermalResolutionMp;

    @Column(name = "thermal_sensitivity_mk")
    private Double thermalSensitivityMilliKelvin;

    @Column(name = "surveillance_radius_km")
    private Double surveillanceRadiusKm;

    @Column(name = "data_link_range_km")
    private Double dataLinkRangeKm;

    @Column(name = "payload_capacity_kg")
    private Double payloadCapacityKg;

    @Column(name = "takeoff_weight_kg")
    private Double takeoffWeightKg;

    @Column(name = "unit_cost_usd")
    private Double unitCostUsd;

    @Column(name = "operating_cost_per_hour_usd")
    private Double operatingCostPerHourUsd;

    @Column(name = "crew_required")
    private Integer crewRequired;

    @Column(name = "setup_time_minutes")
    private Double setupTimeMinutes;

    @Column(name = "noise_level_db")
    private Double noiseLevelDb;

    @Column(name = "stealth_rating")
    private Double stealthRating;

    @Column(name = "weather_resistance", length = 50)
    private String weatherResistance;

    @Column(name = "max_wind_resistance_ms")
    private Double maxWindResistanceMetersPerSec;

    @Column(name = "surveillance_capability", columnDefinition = "TEXT")
    private String surveillanceCapability;

    @Column(name = "applications", columnDefinition = "TEXT")
    private String applications;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
