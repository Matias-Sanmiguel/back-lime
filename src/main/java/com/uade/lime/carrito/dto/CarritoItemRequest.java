package com.uade.lime.carrito.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CarritoItemRequest(
        @NotNull Long propiedadId,
        @NotNull @Positive Integer cantidad) {
}
