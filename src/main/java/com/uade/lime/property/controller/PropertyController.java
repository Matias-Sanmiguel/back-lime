package com.uade.lime.property.controller;

import java.math.BigDecimal;
import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import com.uade.lime.auth.security.UserPrincipal;
import com.uade.lime.property.dto.CreateInquiryRequest;
import com.uade.lime.property.dto.CreatePropertyRequest;
import com.uade.lime.property.dto.ImageResponse;
import com.uade.lime.property.dto.InquiryResponse;
import com.uade.lime.property.dto.PageResponse;
import com.uade.lime.property.dto.PropertyResponse;
import com.uade.lime.property.dto.UpdatePropertyRequest;
import com.uade.lime.property.model.OperationType;
import com.uade.lime.property.model.PropertyStatus;
import com.uade.lime.property.model.PropertyType;
import com.uade.lime.property.service.PropertyService;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

@RestController
@RequestMapping("/api/v1/properties")
@Validated
public class PropertyController {

    private final PropertyService service;

    public PropertyController(PropertyService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<PropertyResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) PropertyType type,
            @RequestParam(required = false) OperationType operation,
            @RequestParam(required = false) PropertyStatus status,
            @RequestParam(required = false) @PositiveOrZero BigDecimal minPrice,
            @RequestParam(required = false) @PositiveOrZero BigDecimal maxPrice,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) @PositiveOrZero Integer minBedrooms,
            @RequestParam(required = false) @PositiveOrZero Integer minBathrooms) {
        return service.list(
                page, size, city, type, operation, status, minPrice, maxPrice, province, minBedrooms, minBathrooms);
    }

    @PostMapping
    public ResponseEntity<PropertyResponse> create(
            @Valid @RequestBody CreatePropertyRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        PropertyResponse created = service.create(request, user);
        return ResponseEntity.created(URI.create("/api/v1/properties/" + created.id())).body(created);
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> addImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal user) {
        ImageResponse created = service.addImage(id, file, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping(value = "/{id}/images/{imageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> replaceImage(
            @PathVariable Long id,
            @PathVariable Long imageId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal user) {
        ImageResponse updated = service.replaceImage(id, imageId, file, user);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId,
            @AuthenticationPrincipal UserPrincipal user) {
        service.deleteImage(id, imageId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public PropertyResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return service.get(id, user);
    }

    @PatchMapping("/{id}")
    public PropertyResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePropertyRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return service.update(id, request, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        service.delete(id, user.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public PropertyResponse publish(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return service.publish(id, user);
    }

    @PostMapping("/{id}/pause")
    public PropertyResponse pause(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return service.pause(id, user);
    }

    @PostMapping("/{id}/inquiries")
    public ResponseEntity<InquiryResponse> createInquiry(
            @PathVariable Long id,
            @Valid @RequestBody CreateInquiryRequest request) {
        InquiryResponse created = service.createInquiry(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
