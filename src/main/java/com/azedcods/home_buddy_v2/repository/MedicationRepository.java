package com.azedcods.home_buddy_v2.repository;

import com.azedcods.home_buddy_v2.model.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationRepository extends JpaRepository<Medication, Long> {

    List<Medication> findTop20ByNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(String name);
}

