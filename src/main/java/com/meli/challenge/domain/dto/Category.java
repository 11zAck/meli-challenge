package com.meli.challenge.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Category {

    private String id;

    private String name;

    private String permalink;

    @JsonProperty("total_items_in_this_category")
    private int totalItemsInThisCategory;

    /*
    "id": "MLA6977",
    "name": "Puertas",
    "picture": null,
    "permalink": null,
    "total_items_in_this_category": 33998,
    */
}
