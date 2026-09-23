package com.uade.lime.property.dto;

import java.time.Instant;

import com.uade.lime.property.model.Inquiry;

public record InquiryResponse(
        Long id,
        Long propertyId,
        String propertyTitle,
        String name,
        String email,
        String phone,
        String message,
        Instant createdAt,
        Instant readAt) {

    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getProperty().getId(),
                inquiry.getProperty().getTitle(),
                inquiry.getName(),
                inquiry.getEmail(),
                inquiry.getPhone(),
                inquiry.getMessage(),
                inquiry.getCreatedAt(),
                inquiry.getReadAt());
    }
}