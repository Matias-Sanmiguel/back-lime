package com.uade.lime.property.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.lime.auth.security.UserPrincipal;
import com.uade.lime.property.dto.PageResponse;
import com.uade.lime.property.model.OperationType;
import com.uade.lime.property.model.PropertyStatus;
import com.uade.lime.property.model.PropertyType;
import com.uade.lime.property.dto.PropertyResponse;
import com.uade.lime.property.service.PropertyService;
import com.uade.lime.user.dto.UpdateMeRequest;
import com.uade.lime.user.dto.UserResponse;
import com.uade.lime.user.service.UserService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/me")
@Validated
public class MeController {

    private final PropertyService propertyService;
    private final UserService userService;

    public MeController(PropertyService propertyService, UserService userService) {
        this.propertyService = propertyService;
        this.userService = userService;
    }

    @GetMapping("/properties")
    public PageResponse<PropertyResponse> listMine(
            @AuthenticationPrincipal UserPrincipal user,
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
        return propertyService.listMine(
                user, page, size, city, type, operation, status, minPrice, maxPrice, province, minBedrooms, minBathrooms);
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal UserPrincipal user) {
        return userService.getMe(user.id());
    }

    @PatchMapping
    public UserResponse updateMe(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody UpdateMeRequest request) {
        return userService.updateMe(user.id(), request);
    }
}
