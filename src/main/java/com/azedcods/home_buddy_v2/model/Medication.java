package com.azedcods.home_buddy_v2.model;

import com.azedcods.home_buddy_v2.model.enums.MedicationForm;
import com.azedcods.home_buddy_v2.model.enums.MedicationSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(name = "medication",
        indexes = {
                @Index(name = "idx_medication_name", columnList = "name"),
                @Index(name = "idx_medication_source_external", columnList = "source, external_id", unique = true)
        })
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "medication_form", nullable = false, length = 50)
    private MedicationForm medicationForm;

    @Column(name = "medication_strength", length = 100)
    private String medicationStrength;

    @Column(name = "medication_description", length = 2000)
    private String medicationDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedicationSource source = MedicationSource.MANUAL;

    @Column(name = "external_id", length = 50)
    private String externalId; // RxCUI when source = RXNORM

    @Column(nullable = false)
    private boolean active = true;

    // getters/setters
}
