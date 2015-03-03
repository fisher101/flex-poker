package com.flexpoker.web.model.outgoing;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PocketCardsDTO {

    @JsonProperty
    private final int cardId1;

    @JsonProperty
    private final int cardId2;

    @JsonProperty
    private final UUID tableId;

    public PocketCardsDTO(int cardId1, int cardId2, UUID tableId) {
        this.cardId1 = cardId1;
        this.cardId2 = cardId2;
        this.tableId = tableId;
    }

}