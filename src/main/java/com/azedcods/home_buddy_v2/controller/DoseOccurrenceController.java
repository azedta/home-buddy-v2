package com.azedcods.home_buddy_v2.controller;

import com.azedcods.home_buddy_v2.model.dose.DoseOccurrence;
import com.azedcods.home_buddy_v2.payload.DoseOccurrenceDtos;
import com.azedcods.home_buddy_v2.security.services.UserDetailsImpl;
import com.azedcods.home_buddy_v2.service.dose.DoseOccurrenceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

@RestController
@RequestMapping("/api/dose-occurrences")
public class DoseOccurrenceController {

    private final DoseOccurrenceService service;

    public DoseOccurrenceController(DoseOccurrenceService service) {
        this.service = service;
    }

    @GetMapping
    public DoseOccurrenceDtos.WindowResponse listWindow(
            Authentication auth,
            @RequestParam(required = false) Long userId,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime to
    ) {
        Long effectiveUserId = resolveUserIdOrThrow(auth, userId);

        List<DoseOccurrenceDtos.Response> items = service.listWindowForUser(effectiveUserId, from, to).stream()
                .map(this::toDto)
                .toList();

        return new DoseOccurrenceDtos.WindowResponse(from, to, items);
    }

    @PostMapping("/generate")
    public DoseOccurrenceDtos.WindowResponse generate(
            Authentication auth,
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody DoseOccurrenceDtos.GenerateRequest req
    ) {
        Long effectiveUserId = resolveUserIdOrThrow(auth, userId);

        List<DoseOccurrenceDtos.Response> items = service.generateWindowForUser(effectiveUserId, req.from(), req.to()).stream()
                .map(this::toDto)
                .toList();

        return new DoseOccurrenceDtos.WindowResponse(req.from(), req.to(), items);
    }

    @PostMapping("/{id}/taken")
    public DoseOccurrenceDtos.Response markTaken(
            @PathVariable Long id,
            @Valid @RequestBody DoseOccurrenceDtos.MarkTakenRequest req
    ) {
        // Optional: enforce ownership by checking occurrence -> dose -> userId
        return toDto(service.markTaken(id, req.takenAt(), req.note()));
    }

    @PostMapping("/{id}/status")
    public DoseOccurrenceDtos.Response setStatus(
            @PathVariable Long id,
            @Valid @RequestBody DoseOccurrenceDtos.MarkStatusRequest req
    ) {
        // Optional: enforce ownership by checking occurrence -> dose -> userId
        return toDto(service.setStatus(id, req.status(), req.note()));
    }

    private DoseOccurrenceDtos.Response toDto(DoseOccurrence o) {
        return new DoseOccurrenceDtos.Response(
                o.getId(),
                o.getDose().getId(),
                o.getScheduledAt(),
                o.getStatus(),
                o.getTakenAt(),
                o.getNote()
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
