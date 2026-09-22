package com.uade.lime.property.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uade.lime.auth.security.UserPrincipal;
import com.uade.lime.property.dto.InquiryInboxResponse;
import com.uade.lime.property.dto.InquiryResponse;
import com.uade.lime.property.dto.MarkInquiryReadRequest;
import com.uade.lime.property.service.InquiryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/me/inquiries")
@Validated
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping
    public ResponseEntity<InquiryInboxResponse> listMine(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        InquiryInboxResponse result = inquiryService.listMine(user.id(), page, size, propertyId, unreadOnly);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryResponse> getMine(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long inquiryId) {
        InquiryResponse result = inquiryService.getMine(user.id(), inquiryId);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{inquiryId}")
    public ResponseEntity<InquiryResponse> markRead(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long inquiryId,
            @Valid @RequestBody MarkInquiryReadRequest request) {
        InquiryResponse result = inquiryService.markRead(user.id(), inquiryId, Boolean.TRUE.equals(request.read()));
        return ResponseEntity.ok(result);
    }
}