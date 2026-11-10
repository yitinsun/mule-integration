package com.yitinsun.muleintegration.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "PostalResponse", description = "Zippopotam postal lookup")
public record PostalResponse(
        String postalCode,
        String country,
        String countryAbbreviation,
        List<Place> places) {
}
