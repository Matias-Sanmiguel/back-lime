package com.uade.lime.carrito.controller;

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
import com.uade.lime.carrito.dto.AddCartItemRequest;
import com.uade.lime.carrito.dto.CarritoItemResponse;
import com.uade.lime.carrito.service.CarritoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/me/cart/items")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @GetMapping
    public List<CarritoItemResponse> getAll(@AuthenticationPrincipal UserPrincipal user) {
        return carritoService.getAll(user.id());
    }

    @PostMapping
    public ResponseEntity<CarritoItemResponse> add(
            @Valid @RequestBody AddCartItemRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        CarritoItemResponse created = carritoService.add(user.id(), request);
        return ResponseEntity.status(201).body(created);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal user) {
        carritoService.delete(itemId, user.id());
        return ResponseEntity.noContent().build();
    }
}
