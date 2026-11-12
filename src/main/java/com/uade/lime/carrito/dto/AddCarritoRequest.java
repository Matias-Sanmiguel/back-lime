package com.uade.lime.carrito.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record AddCarritoRequest(
        @NotEmpty List<@Valid CarritoItemRequest> items) {
}
