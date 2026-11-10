package com.yitinsun.muleintegration.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Place", description = "One place inside a postal code")
public record Place(
        String placeName,
        String state,
        String stateAbbreviation,
        String latitude,
        String longitude) {
}
