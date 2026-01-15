package com.azedcods.home_buddy_v2.repository.medication;

import com.azedcods.home_buddy_v2.model.medication.Medication;
import com.azedcods.home_buddy_v2.enums.MedicationSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicationRepository extends JpaRepository<Medication, Long> {

    List<Medication> findTop20ByNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(String name);

    Optional<Medication> findBySourceAndExternalId(MedicationSource source, String externalId);
}
