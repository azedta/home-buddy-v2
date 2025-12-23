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
                .uri(uriBuilder -> uriBuilder.path("/drugs.json")
                        .queryParam("name", name)
                        .build())
                .retrieve()
                .body(String.class);
    }
}
