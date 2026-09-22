package com.uade.lime.property.service;

import com.uade.lime.common.ResourceNotFoundException;
import com.uade.lime.property.dto.InquiryInboxResponse;
import com.uade.lime.property.model.Inquiry;
import com.uade.lime.property.repository.InquiryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class InquiryService {

    @Autowired
    private InquiryRepository inquiryRepository;

    @Transactional(readOnly = true)
    public InquiryInboxResponse getInquiries(Long ownerId, boolean unreadOnly, Pageable pageable) {
        Page<Inquiry> inquiries;
        if (unreadOnly) {
            inquiries = inquiryRepository.findByPropertyOwnerIdAndReadAtIsNull(ownerId, pageable);
        } else {
            inquiries = inquiryRepository.findByPropertyOwnerId(ownerId, pageable);
        }
        long unreadCount = inquiryRepository.countByPropertyOwnerIdAndReadAtIsNull(ownerId);
        return new InquiryInboxResponse(inquiries, unreadCount);
    }

    @Transactional(readOnly = true)
    public Inquiry getInquiryById(Long id, Long ownerId) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada con ID: " + id));
        
        // Validación de propiedad
        if (!Objects.equals(inquiry.getProperty().getOwnerId(), ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para acceder a esta consulta");
        }
        return inquiry;
    }

    @Transactional
    public Inquiry markAsRead(Long id, Long ownerId) {
        // Reutilizamos el método anterior que ya busca y valida los permisos
        Inquiry inquiry = getInquiryById(id, ownerId); 
        
        inquiry.setReadAt(LocalDateTime.now());
        return inquiryRepository.save(inquiry);
    }
}