package com.uade.lime.property.dto;

import java.time.Instant;

import com.uade.lime.property.model.PropertyImage;

public record ImageResponse(Long id, String url, Instant createdAt, Instant updatedAt) {

    public static ImageResponse from(PropertyImage image) {
        return new ImageResponse(
                image.getId(),
                image.getUrl(),
                image.getCreatedAt(),
                image.getUpdatedAt());
    }
    
}
