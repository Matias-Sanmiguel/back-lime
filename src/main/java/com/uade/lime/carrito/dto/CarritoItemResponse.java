package com.uade.lime.carrito.dto;

import com.uade.lime.carrito.model.CarritoItem;

public record CarritoItemResponse(
        Long propiedadId,
        String propiedadTitulo,
        int cantidad) {

    public static CarritoItemResponse from(CarritoItem item) {
        return new CarritoItemResponse(
                item.getPropiedad().getId(),
                item.getPropiedad().getTitle(),
                item.getCantidad());
    }
}
