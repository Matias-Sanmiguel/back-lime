package com.uade.lime.carrito.service;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.uade.lime.auth.model.User;
import com.uade.lime.auth.repository.UserRepository;
import com.uade.lime.carrito.dto.AddCarritoRequest;
import com.uade.lime.carrito.dto.CarritoResponse;
import com.uade.lime.carrito.model.Carrito;
import com.uade.lime.carrito.repository.CarritoRepository;
import com.uade.lime.property.model.Property;
import com.uade.lime.property.model.PropertyStatus;
import com.uade.lime.property.repository.PropertyRepository;

@Service
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;

    public CarritoService(
            CarritoRepository carritoRepository,
            UserRepository userRepository,
            PropertyRepository propertyRepository) {
        this.carritoRepository = carritoRepository;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
    }

    @Transactional(readOnly = true)
    public List<CarritoResponse> getAll(Long usuarioId) {
        return carritoRepository.findAllByUsuario_IdOrderByFechaCreacionDesc(usuarioId).stream()
                .map(CarritoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CarritoResponse getById(Long id, Long usuarioId) {
        return CarritoResponse.from(requireCarrito(id, usuarioId));
    }

    @Transactional
    public CarritoResponse add(Long usuarioId, AddCarritoRequest request) {
        User usuario = userRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Carrito carrito = Carrito.create(usuario, Instant.now());

        for (var itemRequest : request.items()) {
            Property propiedad = propertyRepository.findByIdAndDeletedAtIsNull(itemRequest.propiedadId())
                    .filter(item -> item.getStatus() == PropertyStatus.PUBLISHED)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
            carrito.addItem(propiedad, itemRequest.cantidad());
        }

        return CarritoResponse.from(carritoRepository.save(carrito));
    }

    @Transactional
    public void delete(Long id, Long usuarioId) {
        carritoRepository.delete(requireCarrito(id, usuarioId));
    }

    private Carrito requireCarrito(Long id, Long usuarioId) {
        return carritoRepository.findByIdAndUsuario_Id(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrito no encontrado"));
    }
}
