package com.uade.lime.carrito.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.lime.carrito.model.Carrito;

@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    @EntityGraph(attributePaths = {"items", "items.propiedad"})
    List<Carrito> findAllByUsuario_IdOrderByFechaCreacionDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"items", "items.propiedad"})
    Optional<Carrito> findByIdAndUsuario_Id(Long id, Long usuarioId);
}
