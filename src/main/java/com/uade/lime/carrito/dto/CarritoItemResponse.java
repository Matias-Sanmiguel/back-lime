package com.uade.lime.carrito.dto;

import java.time.Instant;

import com.uade.lime.carrito.model.CarritoItem;
import com.uade.lime.property.dto.ImageResponse;
import com.uade.lime.property.dto.OwnerResponse;
import com.uade.lime.property.dto.PropertyResponse;

public record CarritoItemResponse(
        Long id,
        PropertyResponse property,
        Instant addedAt) {

    public static CarritoItemResponse from(
            CarritoItem item,
            OwnerResponse owner,
            ImageResponse coverImage) {
        return new CarritoItemResponse(
                item.getId(),
                PropertyResponse.from(item.getPropiedad(), owner, coverImage),
                item.getAddedAt());
    }
}
