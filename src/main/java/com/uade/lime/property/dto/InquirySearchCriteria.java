package com.uade.lime.property.dto;

public record InquirySearchCriteria(
        int page,
        int size,
        Long propertyId,
        boolean unreadOnly) {
}