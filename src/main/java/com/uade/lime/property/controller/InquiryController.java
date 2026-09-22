package com.uade.lime.property.controller;

import com.uade.lime.property.dto.InquiryInboxResponse;
import com.uade.lime.property.model.Inquiry;
import com.uade.lime.property.service.InquiryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/inquiries")
public class InquiryController {

    @Autowired
    private InquiryService inquiryService;

    @GetMapping
    public ResponseEntity<InquiryInboxResponse> getMyInquiries(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            Pageable pageable,
            Authentication authentication) {
        
        Long ownerId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(inquiryService.getInquiries(ownerId, unreadOnly, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Inquiry> getInquiryById(
            @PathVariable Long id,
            Authentication authentication) {
        
        Long ownerId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(inquiryService.getInquiryById(id, ownerId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Inquiry> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        
        Long ownerId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(inquiryService.markAsRead(id, ownerId));
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        // Reemplazá este casteo por la forma exacta en que obtienen el usuario en tu proyecto
        return 1L; 
    }
}