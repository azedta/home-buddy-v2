package com.azedcods.home_buddy_v2.service.dose;

import com.azedcods.home_buddy_v2.enums.MedicationSource;
import com.azedcods.home_buddy_v2.model.auth.User;
import com.azedcods.home_buddy_v2.model.dose.Dose;
import com.azedcods.home_buddy_v2.model.medication.Medication;
import com.azedcods.home_buddy_v2.payload.DoseDtos;
import com.azedcods.home_buddy_v2.repository.auth.UserRepository;
import com.azedcods.home_buddy_v2.repository.dose.DoseRepository;
import com.azedcods.home_buddy_v2.repository.medication.MedicationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class DoseService {

    private final DoseRepository doseRepo;
    private final UserRepository userRepo;
    private final MedicationRepository medicationRepo;

    public DoseService(DoseRepository doseRepo, UserRepository userRepo, MedicationRepository medicationRepo) {
        this.doseRepo = doseRepo;
        this.userRepo = userRepo;
        this.medicationRepo = medicationRepo;
    }

    public Dose create(DoseDtos.CreateRequest req) {
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + req.userId()));

        Medication med = medicationRepo.findById(req.localMedicationId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Medication not found: " + req.localMedicationId()
                ));

        // 🔒 HARD RULE: dose creation only with MANUAL medications
        if (med.getSource() != MedicationSource.MANUAL) {
            throw new IllegalArgumentException(
                    "Dose creation requires a LOCAL (MANUAL) medication."
            );
        }

        if (!med.isActive()) {
            throw new IllegalStateException("Medication is inactive.");
        }

        Set<DayOfWeek> days =
                (req.daysOfWeek() == null) ? new LinkedHashSet<>() : new LinkedHashSet<>(req.daysOfWeek());

        Set<LocalTime> times = normalizeTimes(req.times());

        validateTimesIfProvided(req.timeFrequency(), times);

        Dose dose = new Dose(
                req.timeFrequency(),
                days,
                times,
                req.quantityAmount(),
                req.quantityUnit(),
                req.startDate(),
                req.endDate(),
                req.instructions(),
                med,
                user
        );

        return doseRepo.save(dose);
    }



    @Transactional(readOnly = true)
    public List<Dose> getAll() {
        return doseRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Dose getById(Long id) {
        return doseRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dose not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Dose> getUserDoses(Long userId) {
        return doseRepo.findByUser_UserId(userId);
    }

    public Dose update(Long id, DoseDtos.UpdateRequest req) {
        Dose dose = getById(id);

        if (req.timeFrequency() != null) {
            dose.setTimeFrequency(req.timeFrequency());
        }

        // If provided (even empty), apply it.
        if (req.daysOfWeek() != null) {
            dose.setDaysOfWeek(new LinkedHashSet<>(req.daysOfWeek()));
        }

        if (req.times() != null) {
            dose.setTimes(normalizeTimes(req.times()));
        }

        if (req.quantityAmount() != null) dose.setQuantityAmount(req.quantityAmount());
        if (req.quantityUnit() != null) dose.setQuantityUnit(req.quantityUnit());
        if (req.startDate() != null) dose.setStartDate(req.startDate());
        if (req.endDate() != null) dose.setEndDate(req.endDate());
        if (req.instructions() != null) dose.setInstructions(req.instructions());

        // Re-validate only if times are explicitly provided (non-empty)
        validateTimesIfProvided(dose.getTimeFrequency(), dose.getTimes());

        return dose;
    }

    public void delete(Long id) {
        if (!doseRepo.existsById(id)) throw new EntityNotFoundException("Dose not found: " + id);
        doseRepo.deleteById(id);
    }

    private Set<LocalTime> normalizeTimes(Set<LocalTime> times) {
        if (times == null || times.isEmpty()) return new LinkedHashSet<>();
        return new LinkedHashSet<>(times.stream()
                .filter(t -> t != null)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList());
    }

    private void validateTimesIfProvided(Integer timeFrequency, Set<LocalTime> times) {
        if (timeFrequency == null || timeFrequency < 1) {
            throw new IllegalArgumentException("timeFrequency must be >= 1");
        }

        if (times != null && !times.isEmpty()) {
            int timesCount = times.size();
            if (timesCount != timeFrequency) {
                throw new IllegalArgumentException(
                        "times count (" + timesCount + ") must equal timeFrequency (" + timeFrequency + ")"
                );
            }
        }
        // If times are empty => allowed, schedule engine derives defaults
    }
}
