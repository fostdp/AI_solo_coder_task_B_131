package com.nestcart.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dynasty_cart")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynastyCart {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "dynasty_name", nullable = false, length = 50)
    private String dynastyName;

    @Column(name = "period", nullable = false, length = 100)
    private String period;

    @Column(name = "era_year", length = 100)
    private String eraYear;

    @Column(name = "historical_context", columnDefinition = "TEXT")
    private String historicalContext;

    @Column(name = "boom_length")
    private Double boomLength;

    @Column(name = "boom_cross_section_area")
    private Double boomCrossSectionArea;

    @Column(name = "boom_moment_of_inertia")
    private Double boomMomentOfInertia;

    @Column(name = "boom_elastic_modulus")
    private Double boomElasticModulus;

    @Column(name = "basket_weight")
    private Double basketWeight;

    @Column(name = "base_height")
    private Double baseHeight;

    @Column(name = "max_height")
    private Double maxHeight;

    @Column(name = "stress_limit")
    private Double stressLimit;

    @Column(name = "sway_limit")
    private Double swayLimit;

    @Column(name = "crew_size")
    private Integer crewSize;

    @Column(name = "observation_distance_estimate")
    private Double observationDistanceEstimate;

    @Column(name = "construction_material", length = 100)
    private String constructionMaterial;

    @Column(name = "military_role", length = 200)
    private String militaryRole;

    @Column(name = "historical_record", columnDefinition = "TEXT")
    private String historicalRecord;

    @Column(name = "innovation_features", columnDefinition = "TEXT")
    private String innovationFeatures;

    @Column(name = "evolution_score")
    private Double evolutionScore;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "literature_sources", columnDefinition = "TEXT")
    private String literatureSources;

    @Column(name = "structural_cross_section", length = 100)
    private String structuralCrossSection;

    @Column(name = "parameter_confidence_level", length = 50)
    private String parameterConfidenceLevel;

    @Column(name = "archaeological_evidence", columnDefinition = "TEXT")
    private String archaeologicalEvidence;

    @Column(name = "data_citation", length = 200)
    private String dataCitation;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
