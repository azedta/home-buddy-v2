package com.azedcods.home_buddy_v2.service.medication;

import com.azedcods.home_buddy_v2.model.Medication;
import com.azedcods.home_buddy_v2.model.enums.MedicationSource;
import com.azedcods.home_buddy_v2.payload.MedicationDtos;
import com.azedcods.home_buddy_v2.repository.MedicationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.util.ArrayList;
import java.util.List;

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

    @Cacheable(cacheNames = "medCatalogSearch", key = "#query.toLowerCase()")
    public List<MedicationDtos.CatalogItem> search(String query, int limit) {
        List<MedicationDtos.CatalogItem> local = medicationRepo
                .findTop20ByNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(query)
                .stream()
                .map(this::toCatalogItem)
                .toList();

        List<MedicationDtos.CatalogItem> rx = searchRxNorm(query, limit);

        // simple merge: local first, then rxnorm results (dedupe by name+strength if you want later)
        List<MedicationDtos.CatalogItem> out = new ArrayList<>();
        out.addAll(local);
        out.addAll(rx);
        return out;
    }

    private MedicationDtos.CatalogItem toCatalogItem(Medication m) {
        return new MedicationDtos.CatalogItem(
                "LOCAL:" + m.getId(),
                m.getSource(),
                m.getExternalId(),
                m.getId(),
                m.getName(),
                m.getMedicationForm().name(),
                m.getMedicationStrength(),
                m.getMedicationDescription()
        );
    }

    private List<MedicationDtos.CatalogItem> searchRxNorm(String query, int limit) {
        try {
            String raw = rxNormClient.getDrugsRaw(query);
            JsonNode root = om.readTree(raw);

            // RxNorm response shapes vary; this is a pragmatic minimal parse:
            // pull out "drugGroup.conceptGroup[*].conceptProperties[*].{rxcui,name,tty}"
            JsonNode drugGroup = root.path("drugGroup");
            JsonNode conceptGroups = drugGroup.path("conceptGroup");

            List<MedicationDtos.CatalogItem> items = new ArrayList<>();
            if (conceptGroups.isArray()) {
                for (JsonNode cg : conceptGroups) {
                    JsonNode cps = cg.path("conceptProperties");
                    if (!cps.isArray()) continue;

                    for (JsonNode cp : cps) {
                        String rxcui = cp.path("rxcui").asText(null);
                        String name = cp.path("name").asText(null);
                        if (rxcui == null || name == null) continue;

                        items.add(new MedicationDtos.CatalogItem(
                                "RXNORM:" + rxcui,
                                MedicationSource.RXNORM,
                                rxcui,
                                null,
                                name,
                                null,
                                null,
                                null
                        ));

                        if (items.size() >= limit) return items;
                    }
                }
            }
            return items;
        } catch (Exception e) {
            // fail soft: return empty if RxNorm is down
            return List.of();
        }
    }
}

