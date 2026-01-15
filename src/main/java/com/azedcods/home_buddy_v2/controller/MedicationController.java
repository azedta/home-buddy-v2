package com.azedcods.home_buddy_v2.controller;

import com.azedcods.home_buddy_v2.model.medication.Medication;
import com.azedcods.home_buddy_v2.payload.MedicationDtos;
import com.azedcods.home_buddy_v2.service.medication.MedicationAdminService;
import com.azedcods.home_buddy_v2.service.medication.MedicationCatalogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.CacheEvict;


import java.util.List;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private final MedicationCatalogService catalogService;
    private final MedicationAdminService adminService;

    public MedicationController(MedicationCatalogService catalogService, MedicationAdminService adminService) {
        this.catalogService = catalogService;
        this.adminService = adminService;
    }

    // MAIN endpoint: list/search meds (RxNorm + local)
    @GetMapping("/catalog")
    public List<MedicationDtos.CatalogItem> catalog(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit
    ) {
        if (q == null || q.trim().length() < 2) return List.of();
        return catalogService.search(q.trim(), Math.min(limit, 50));
    }

    // caregiver/admin: create manual medication
    @PostMapping
    // @PreAuthorize("hasAnyRole('CAREGIVER','ADMIN')")
    @CacheEvict(cacheNames = "medCatalogSearch", allEntries = true)
    public MedicationDtos.CatalogItem create(@RequestBody @Valid MedicationDtos.CreateRequest req) {
        Medication saved = adminService.createManual(req);
        return new MedicationDtos.CatalogItem(
                "LOCAL:" + saved.getId(),
                saved.getSource(),
                saved.getExternalId(),
                saved.getId(),
                saved.getName(),
                saved.getMedicationForm().name(),
                saved.getMedicationStrength(),
                saved.getMedicationDescription()
        );
    }

    @PutMapping("/{id}")
    // @PreAuthorize("hasAnyRole('CAREGIVER','ADMIN')")
    @CacheEvict(cacheNames = "medCatalogSearch", allEntries = true)
    public MedicationDtos.CatalogItem update(@PathVariable Long id, @RequestBody MedicationDtos.UpdateRequest req) {
        Medication saved = adminService.update(id, req);
        return new MedicationDtos.CatalogItem(
                "LOCAL:" + saved.getId(),
                saved.getSource(),
                saved.getExternalId(),
                saved.getId(),
                saved.getName(),
                saved.getMedicationForm().name(),
                saved.getMedicationStrength(),
                saved.getMedicationDescription()
        );
    }

    @DeleteMapping("/{id}")
    // @PreAuthorize("hasAnyRole('CAREGIVER','ADMIN')")
    @CacheEvict(cacheNames = "medCatalogSearch", allEntries = true)
    public void delete(@PathVariable Long id) {
        adminService.delete(id);
    }
}
