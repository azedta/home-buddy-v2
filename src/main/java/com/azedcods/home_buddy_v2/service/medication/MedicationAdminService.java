package com.azedcods.home_buddy_v2.service.medication;

import com.azedcods.home_buddy_v2.model.Medication;
import com.azedcods.home_buddy_v2.model.enums.*;
import com.azedcods.home_buddy_v2.payload.MedicationDtos;
import com.azedcods.home_buddy_v2.repository.MedicationRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class MedicationAdminService {

    private final MedicationRepository repo;

    public MedicationAdminService(MedicationRepository repo) {
        this.repo = repo;
    }

    public Medication createManual(MedicationDtos.CreateRequest req) {
        Medication m = new Medication();
        m.setName(req.name().trim());
        m.setMedicationForm(req.medicationForm());
        m.setMedicationStrength(req.medicationStrength());
        m.setMedicationDescription(req.medicationDescription());
        m.setSource(MedicationSource.MANUAL);
        m.setActive(true);
        return repo.save(m);
    }

    public Medication update(Long id, MedicationDtos.UpdateRequest req) {
        Medication m = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Medication not found"));
        if (req.name() != null) m.setName(req.name().trim());
        if (req.medicationForm() != null) m.setMedicationForm(req.medicationForm());
        if (req.medicationStrength() != null) m.setMedicationStrength(req.medicationStrength());
        if (req.medicationDescription() != null) m.setMedicationDescription(req.medicationDescription());
        if (req.active() != null) m.setActive(req.active());
        return repo.save(m);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}

