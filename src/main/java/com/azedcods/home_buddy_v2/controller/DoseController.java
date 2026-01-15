package com.azedcods.home_buddy_v2.controller;

import com.azedcods.home_buddy_v2.model.dose.Dose;
import com.azedcods.home_buddy_v2.payload.DoseDtos;
import com.azedcods.home_buddy_v2.security.services.UserDetailsImpl;
import com.azedcods.home_buddy_v2.service.dose.DoseService;
import com.azedcods.home_buddy_v2.service.dose.DoseOccurrenceService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doses")
public class DoseController {

    private final DoseService service;
    private final DoseOccurrenceService occurrenceService;

    public DoseController(DoseService service, DoseOccurrenceService occurrenceService) {
        this.service = service;
        this.occurrenceService = occurrenceService;
    }

    @PostMapping
    public DoseDtos.Response createDose(Authentication auth, @Valid @RequestBody DoseDtos.CreateRequest req) {
        Long effectiveUserId = resolveUserIdOrThrow(auth, req.userId());

        DoseDtos.CreateRequest safeReq = new DoseDtos.CreateRequest(
                effectiveUserId,
                req.localMedicationId(),
                req.timeFrequency(),
                req.daysOfWeek(),
                req.times(),
                req.quantityAmount(),
                req.quantityUnit(),
                req.startDate(),
                req.endDate(),
                req.instructions()
        );

        Dose created = service.create(safeReq);

        // Auto-generate + auto-load the dispenser for the current month immediately.
        var now = java.time.LocalDateTime.now();
        var monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        var monthEnd = now.toLocalDate()
                .withDayOfMonth(now.toLocalDate().lengthOfMonth())
                .atTime(23, 59, 59);

        occurrenceService.generateWindowForUser(effectiveUserId, monthStart, monthEnd);

        return toDto(created);
    }

    @GetMapping
    public List<DoseDtos.Response> list(Authentication auth, @RequestParam(required = false) Long userId) {
        Long effectiveUserId = resolveUserIdOrThrow(auth, userId);
        return service.getUserDoses(effectiveUserId).stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public DoseDtos.Response getDoseById(@PathVariable Long id) {
        return toDto(service.getById(id));
    }

    @PutMapping("/{id}")
    public DoseDtos.Response updateDose(@PathVariable Long id, @Valid @RequestBody DoseDtos.UpdateRequest req) {
        return toDto(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public void deleteDose(@PathVariable Long id) {
        service.delete(id);
    }

    private DoseDtos.Response toDto(Dose d) {
        return new DoseDtos.Response(
                d.getId(),
                d.getUser().getUserId(),
                d.getMedication().getId(),
                d.getMedication().getName(),
                d.getTimeFrequency(),
                d.getDaysOfWeek(),
                d.getTimes(),
                d.getQuantityAmount(),
                d.getQuantityUnit(),
                d.getStartDate(),
                d.getEndDate(),
                d.getInstructions()
        );
    }

    private Long resolveUserIdOrThrow(Authentication auth, Long requestedUserId) {
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl principal)) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
        }

        boolean isUser = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));

        boolean isCaregiverOrAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CAREGIVER") || a.getAuthority().equals("ROLE_ADMIN"));

        if (isCaregiverOrAdmin) {
            if (requestedUserId == null) throw new IllegalArgumentException("userId is required");
            return requestedUserId;
        }

        if (isUser) return principal.getId();

        throw new org.springframework.security.access.AccessDeniedException("Forbidden");
    }
}
