package com.uade.lime.carrito.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.lime.carrito.model.CarritoItem;

@Repository
public interface CarritoItemRepository extends JpaRepository<CarritoItem, Long> {

    @EntityGraph(attributePaths = "propiedad")
    List<CarritoItem> findAllByCarrito_Usuario_IdOrderByAddedAtDesc(Long usuarioId);

    Optional<CarritoItem> findByIdAndCarrito_Usuario_Id(Long itemId, Long usuarioId);

    boolean existsByCarrito_IdAndPropiedad_Id(Long carritoId, Long propiedadId);
}
