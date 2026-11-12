package com.uade.lime.carrito.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.lime.auth.model.User;
import com.uade.lime.auth.repository.UserRepository;
import com.uade.lime.carrito.dto.AddCartItemRequest;
import com.uade.lime.carrito.dto.CarritoItemResponse;
import com.uade.lime.carrito.model.Carrito;
import com.uade.lime.carrito.model.CarritoItem;
import com.uade.lime.carrito.repository.CarritoItemRepository;
import com.uade.lime.carrito.repository.CarritoRepository;
import com.uade.lime.common.exception.ConflictoException;
import com.uade.lime.common.exception.RecursoNoEncontradoException;
import com.uade.lime.property.dto.ImageResponse;
import com.uade.lime.property.dto.OwnerResponse;
import com.uade.lime.property.model.Property;
import com.uade.lime.property.model.PropertyStatus;
import com.uade.lime.property.repository.PropertyImageRepository;
import com.uade.lime.property.repository.PropertyRepository;

@Service
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final CarritoItemRepository carritoItemRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;

    public CarritoService(
            CarritoRepository carritoRepository,
            CarritoItemRepository carritoItemRepository,
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            PropertyImageRepository propertyImageRepository) {
        this.carritoRepository = carritoRepository;
        this.carritoItemRepository = carritoItemRepository;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.propertyImageRepository = propertyImageRepository;
    }

    @Transactional(readOnly = true)
    public List<CarritoItemResponse> getAll(Long usuarioId) {
        return toResponses(carritoItemRepository.findAllByCarrito_Usuario_IdOrderByAddedAtDesc(usuarioId));
    }

    @Transactional
    public CarritoItemResponse add(Long usuarioId, AddCartItemRequest request) {
        Property propiedad = propertyRepository.findByIdAndDeletedAtIsNull(request.propertyId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Property not found"));
        if (propiedad.getStatus() != PropertyStatus.PUBLISHED) {
            throw new ConflictoException("Property is not published");
        }

        Carrito carrito = getOrCreateCarrito(usuarioId);
        if (carritoItemRepository.existsByCarrito_IdAndPropiedad_Id(carrito.getId(), propiedad.getId())) {
            throw new ConflictoException("Property is already in the cart");
        }

        CarritoItem item = carrito.addItem(propiedad, Instant.now());
        CarritoItem savedItem;
        try {
            savedItem = carritoItemRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictoException("Property is already in the cart");
        }
        return toResponses(List.of(savedItem)).get(0);
    }

    @Transactional
    public void delete(Long itemId, Long usuarioId) {
        CarritoItem item = carritoItemRepository.findByIdAndCarrito_Usuario_Id(itemId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cart item not found"));
        carritoItemRepository.delete(item);
    }

    private Carrito getOrCreateCarrito(Long usuarioId) {
        return carritoRepository.findByUsuario_Id(usuarioId)
                .orElseGet(() -> {
                    User usuario = userRepository.findById(usuarioId)
                            .orElseThrow(() -> new RecursoNoEncontradoException("User not found"));
                    return carritoRepository.saveAndFlush(Carrito.create(usuario, Instant.now()));
                });
    }

    private List<CarritoItemResponse> toResponses(List<CarritoItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        List<Property> properties = items.stream().map(CarritoItem::getPropiedad).toList();
        Map<Long, OwnerResponse> ownersById = userRepository.findAllById(
                        properties.stream().map(Property::getOwnerId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, OwnerResponse::from));
        Map<Long, ImageResponse> coversByPropertyId = propertyImageRepository.findCoverImages(
                        properties.stream().map(Property::getId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(image -> image.getProperty().getId(), ImageResponse::from));

        return items.stream()
                .map(item -> {
                    Property property = item.getPropiedad();
                    return CarritoItemResponse.from(
                            item,
                            ownersById.get(property.getOwnerId()),
                            coversByPropertyId.get(property.getId()));
                })
                .toList();
    }
}
