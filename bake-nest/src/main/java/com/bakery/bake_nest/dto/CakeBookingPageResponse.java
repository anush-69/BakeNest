package com.bakery.bake_nest.dto;

import java.util.List;

public record CakeBookingPageResponse(List<CakeBookingResponse> items, int page, int size,
        long totalElements, int totalPages) {
    public CakeBookingPageResponse {
        items = List.copyOf(items);
    }
}
