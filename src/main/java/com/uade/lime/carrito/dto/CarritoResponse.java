package com.uade.lime.carrito.dto;

import java.time.Instant;
import java.util.List;

import com.uade.lime.carrito.model.Carrito;

public record CarritoResponse(
        Long id,
        Long usuarioId,
        Instant fechaCreacion,
        List<CarritoItemResponse> items) {

    public static CarritoResponse from(Carrito carrito) {
        return new CarritoResponse(
                carrito.getId(),
                carrito.getUsuario().getId(),
                carrito.getFechaCreacion(),
                carrito.getItems().stream().map(CarritoItemResponse::from).toList());
    }
}
