package com.uade.lime.carrito.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.lime.auth.security.UserPrincipal;
import com.uade.lime.carrito.dto.AddCarritoRequest;
import com.uade.lime.carrito.dto.CarritoResponse;
import com.uade.lime.carrito.service.CarritoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/carritos")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @GetMapping
    public List<CarritoResponse> getAll(@AuthenticationPrincipal UserPrincipal user) {
        return carritoService.getAll(user.id());
    }

    @GetMapping("/{id}")
    public CarritoResponse getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {
        return carritoService.getById(id, user.id());
    }

    @PostMapping
    public ResponseEntity<CarritoResponse> add(
            @Valid @RequestBody AddCarritoRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        CarritoResponse created = carritoService.add(user.id(), request);
        return ResponseEntity.created(URI.create("/api/v1/carritos/" + created.id())).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {
        carritoService.delete(id, user.id());
        return ResponseEntity.noContent().build();
    }
}
