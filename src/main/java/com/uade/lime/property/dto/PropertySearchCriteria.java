package com.uade.lime.property.dto;

import java.math.BigDecimal;

import com.uade.lime.property.model.OperationType;
import com.uade.lime.property.model.PropertyStatus;
import com.uade.lime.property.model.PropertyType;

public record PropertySearchCriteria(
        int page,
        int size,
        String city,
        PropertyType type,
        OperationType operation,
        PropertyStatus status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String province,
        Integer minBedrooms,
        Integer minBathrooms) {

    public boolean hasInvalidPriceRange() {
        return minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0;
    }
}