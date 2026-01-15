package com.azedcods.home_buddy_v2.service.medication;

import com.azedcods.home_buddy_v2.model.medication.Medication;
import com.azedcods.home_buddy_v2.enums.MedicationSource;
import com.azedcods.home_buddy_v2.payload.MedicationDtos;
import com.azedcods.home_buddy_v2.repository.medication.MedicationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MedicationCatalogService {

    private final RxNormClient rxNormClient;
    private final MedicationRepository medicationRepo;
    private final ObjectMapper om;

    public MedicationCatalogService(RxNormClient rxNormClient, MedicationRepository medicationRepo, ObjectMapper om) {
        this.rxNormClient = rxNormClient;
        this.medicationRepo = medicationRepo;
        this.om = om;
    }

    // ✅ include limit in cache key (otherwise cached "limit=5" will poison "limit=20")
    @Cacheable(cacheNames = "medCatalogSearch", key = "(#query == null ? '' : #query.toLowerCase()) + ':' + #limit")
    public List<MedicationDtos.CatalogItem> search(String query, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));

        List<MedicationDtos.CatalogItem> local = medicationRepo
                .findTop20ByNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(query)
                .stream()
                .map(this::toCatalogItem)
                .toList();

        List<MedicationDtos.CatalogItem> rx = searchRxNorm(query, safeLimit);

        // ✅ Merge + dedupe by "key" while keeping order (local first)
        LinkedHashMap<String, MedicationDtos.CatalogItem> merged = new LinkedHashMap<>();
        for (MedicationDtos.CatalogItem it : local) merged.put(it.key(), it);
        for (MedicationDtos.CatalogItem it : rx) merged.putIfAbsent(it.key(), it);

        // optional: cap total results to safeLimit if you want
        return merged.values().stream().limit(safeLimit).toList();
    }

    private MedicationDtos.CatalogItem toCatalogItem(Medication m) {
        return new MedicationDtos.CatalogItem(
                "LOCAL:" + m.getId(),
                m.getSource(),
                m.getExternalId(),
                m.getId(),
                m.getName(),
                m.getMedicationForm() == null ? null : m.getMedicationForm().name(),
                m.getMedicationStrength(),
                m.getMedicationDescription()
        );
    }

    private List<MedicationDtos.CatalogItem> searchRxNorm(String query, int limit) {
        if (query == null || query.trim().length() < 2) return List.of();
        String q = query.trim();

        try {
            // 1) Try strict-ish endpoint first: /drugs.json?name=
            List<MedicationDtos.CatalogItem> items = parseDrugsByName(q, limit);
            if (!items.isEmpty()) return items;

            // 2) ✅ Fallback to approximateTerm for partial inputs ("Advi")
            return parseApproximateTerm(q, limit);

        } catch (Exception e) {
            return List.of();
        }
    }

    private List<MedicationDtos.CatalogItem> parseDrugsByName(String query, int limit) throws Exception {
        String raw = rxNormClient.getDrugsRaw(query);
        JsonNode root = om.readTree(raw);

        JsonNode conceptGroups = root.path("drugGroup").path("conceptGroup");
        List<MedicationDtos.CatalogItem> items = new ArrayList<>();

        if (conceptGroups.isArray()) {
            for (JsonNode cg : conceptGroups) {
                JsonNode cps = cg.path("conceptProperties");
                if (!cps.isArray()) continue;

                for (JsonNode cp : cps) {
                    String rxcui = cp.path("rxcui").asText(null);
                    String name = cp.path("name").asText(null);
                    if (rxcui == null || name == null) continue;

                    items.add(rxItem(rxcui, name));
                    if (items.size() >= limit) return items;
                }
            }
        }
        return items;
    }

    private List<MedicationDtos.CatalogItem> parseApproximateTerm(String query, int limit) throws Exception {
        String raw = rxNormClient.approximateTermRaw(query, limit);
        JsonNode root = om.readTree(raw);

        JsonNode candidates = root.path("approximateGroup").path("candidate");
        if (!candidates.isArray()) return List.of();

        List<MedicationDtos.CatalogItem> items = new ArrayList<>();
        for (JsonNode c : candidates) {
            String rxcui = c.path("rxcui").asText(null);
            String name = c.path("name").asText(null);
            String source = c.path("source").asText("");

            // Keep RXNORM candidates (some sources may omit name unless licensed)
            if (rxcui == null || name == null) continue;
            if (!"RXNORM".equalsIgnoreCase(source)) continue;

            items.add(rxItem(rxcui, name));
            if (items.size() >= limit) return items;
        }
        return items;
    }

    private MedicationDtos.CatalogItem rxItem(String rxcui, String name) {
        return new MedicationDtos.CatalogItem(
                "RXNORM:" + rxcui,
                MedicationSource.RXNORM,
                rxcui,
                null,
                name,
                null,
                null,
                null
        );
    }
}
