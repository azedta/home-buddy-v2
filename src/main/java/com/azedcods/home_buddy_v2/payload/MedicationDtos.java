package com.azedcods.home_buddy_v2.payload;

import com.azedcods.home_buddy_v2.model.enums.MedicationForm;
import com.azedcods.home_buddy_v2.model.enums.MedicationSource;


public class MedicationDtos {

    public record CatalogItem(
            String key,              // stable id for UI selection: "RXNORM:12345" or "LOCAL:77"
            MedicationSource source,
            String externalId,        // RxCUI if RXNORM
            Long localId,             // if LOCAL
            String name,
            String medicationForm,
            String medicationStrength,
            String description
    ) {}

    public record CreateRequest(
            String name,
            MedicationForm medicationForm,
            String medicationStrength,
            String medicationDescription
    ) {}

    public record UpdateRequest(
            String name,
            MedicationForm medicationForm,
            String medicationStrength,
            String medicationDescription,
            Boolean active
    ) {}
}

