package com.uade.lime.carrito.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.uade.lime.auth.model.User;
import com.uade.lime.property.model.Property;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "carritos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CarritoItem> items = new ArrayList<>();

    public static Carrito create(User usuario, Instant fechaCreacion) {
        Carrito carrito = new Carrito();
        carrito.usuario = usuario;
        carrito.fechaCreacion = fechaCreacion;
        return carrito;
    }

    public void addItem(Property propiedad, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        items.add(CarritoItem.create(this, propiedad, cantidad));
    }
}
