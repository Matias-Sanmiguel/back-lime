package com.uade.lime.property.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.lime.common.exception.ArgumentInvalidException;
import com.uade.lime.common.exception.RecursoNoEncontradoException;
import com.uade.lime.property.dto.InquiryInboxResponse;
import com.uade.lime.property.dto.InquiryResponse;
import com.uade.lime.property.dto.InquirySearchCriteria;
import com.uade.lime.property.model.Inquiry;
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
    public InquiryInboxResponse listMine(Long ownerId, InquirySearchCriteria criteria) {
        if (criteria.propertyId() != null) {
            requireOwnedProperty(criteria.propertyId(), ownerId);
        }

        var pageable = PageRequest.of(criteria.page(), criteria.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Inquiry> inquiries = findInboxPage(ownerId, criteria.propertyId(), criteria.unreadOnly(), pageable);
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
            throw new ArgumentInvalidException("read: false is not supported");
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
        if (!propertyRepository.existsByIdAndOwner_IdAndDeletedAtIsNull(propertyId, ownerId)) {
            throw new RecursoNoEncontradoException("Property not found");
        }
    }

    private Inquiry requireOwnedInquiry(Long inquiryId, Long ownerId) {
        Inquiry inquiry = inquiryRepository.findByIdWithProperty(inquiryId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Consulta no encontrada"));
        if (!ownerId.equals(inquiry.getProperty().getOwnerId())) {
            throw new RecursoNoEncontradoException("Consulta no encontrada");
        }
        return inquiry;
    }
}
