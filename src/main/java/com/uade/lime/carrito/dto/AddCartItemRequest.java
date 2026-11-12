package com.uade.lime.carrito.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotNull @Positive Long propertyId) {
}
