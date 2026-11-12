package com.uade.lime.carrito.model;

import com.uade.lime.property.model.Property;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "carrito_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CarritoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "propiedad_id", nullable = false)
    private Property propiedad;

    @Column(nullable = false)
    private int cantidad;

    static CarritoItem create(Carrito carrito, Property propiedad, int cantidad) {
        CarritoItem item = new CarritoItem();
        item.carrito = carrito;
        item.propiedad = propiedad;
        item.cantidad = cantidad;
        return item;
    }
}
