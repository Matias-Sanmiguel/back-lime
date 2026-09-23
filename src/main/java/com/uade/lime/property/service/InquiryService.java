<<<<<<< HEAD
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
=======
package com.uade.lime.property.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.uade.lime.property.dto.InquiryInboxResponse;
import com.uade.lime.property.dto.InquiryResponse;
import com.uade.lime.property.model.Inquiry;
import com.uade.lime.property.model.Property;
import com.uade.lime.property.repository.InquiryRepository;
import com.uade.lime.property.repository.PropertyRepository;

@Service
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final PropertyRepository propertyRepository;

    public InquiryService(InquiryRepository inquiryRepository, PropertyRepository propertyRepository) {
        this.inquiryRepository = inquiryRepository;
        this.propertyRepository = propertyRepository;
    }

    @Transactional(readOnly = true)
    public InquiryInboxResponse listMine(Long ownerId, int page, int size, Long propertyId, boolean unreadOnly) {
        if (propertyId != null) {
            requireOwnedProperty(propertyId, ownerId);
        }

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Inquiry> inquiries = findInboxPage(ownerId, propertyId, unreadOnly, pageable);
        long unreadCount = inquiryRepository.countUnreadByPropertyOwnerId(ownerId);

        return InquiryInboxResponse.from(inquiries.map(InquiryResponse::from), unreadCount);
    }

    @Transactional(readOnly = true)
    public InquiryResponse getMine(Long ownerId, Long inquiryId) {
        return InquiryResponse.from(requireOwnedInquiry(inquiryId, ownerId));
    }

    @Transactional
    public InquiryResponse markRead(Long ownerId, Long inquiryId, boolean read) {
        if (!read) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "read: false is not supported");
        }

        Inquiry inquiry = requireOwnedInquiry(inquiryId, ownerId);
        inquiry.markRead(Instant.now());
        inquiryRepository.save(inquiry);
        return InquiryResponse.from(inquiry);
    }

    private Page<Inquiry> findInboxPage(Long ownerId, Long propertyId, boolean unreadOnly, PageRequest pageable) {
        if (propertyId != null && unreadOnly) {
            return inquiryRepository.findUnreadByPropertyOwnerIdAndPropertyId(ownerId, propertyId, pageable);
        }
        if (propertyId != null) {
            return inquiryRepository.findByPropertyOwnerIdAndPropertyId(ownerId, propertyId, pageable);
        }
        if (unreadOnly) {
            return inquiryRepository.findUnreadByPropertyOwnerId(ownerId, pageable);
        }
        return inquiryRepository.findByPropertyOwnerId(ownerId, pageable);
    }

    private void requireOwnedProperty(Long propertyId, Long ownerId) {
        Property property = propertyRepository.findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        if (!ownerId.equals(property.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        }
    }

    private Inquiry requireOwnedInquiry(Long inquiryId, Long ownerId) {
        Inquiry inquiry = inquiryRepository.findByIdWithProperty(inquiryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consulta no encontrada"));
        if (!ownerId.equals(inquiry.getProperty().getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consulta no encontrada");
        }
        return inquiry;
    }
}
>>>>>>> origin/main
