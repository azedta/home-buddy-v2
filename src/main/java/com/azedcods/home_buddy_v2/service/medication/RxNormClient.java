package com.azedcods.home_buddy_v2.service.medication;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RxNormClient {

    private final RestClient client = RestClient.builder()
            .baseUrl("https://rxnav.nlm.nih.gov/REST")
            .build();

    public String getDrugsRaw(String name) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/drugs.json")
                        .queryParam("name", name)
                        .build())
                .retrieve()
                .body(String.class);
    }

    // ✅ Fallback for partial search / typeahead (e.g., "Advi" -> "Advil")
    public String approximateTermRaw(String term, int maxEntries) {
        int safeMax = Math.max(1, Math.min(maxEntries, 100));
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/approximateTerm.json")
                        .queryParam("term", term)
                        .queryParam("maxEntries", safeMax)
                        .queryParam("option", 1) // active concepts only
                        .build())
                .retrieve()
                .body(String.class);
    }
}
