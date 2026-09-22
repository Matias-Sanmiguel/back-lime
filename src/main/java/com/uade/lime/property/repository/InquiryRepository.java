package com.uade.lime.property.repository;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.uade.lime.property.model.Inquiry;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // Cuenta no leídas navegando: Inquiry -> Property -> Owner -> Id
    long countByPropertyOwnerIdAndReadAtIsNull(Long ownerId);

    // Trae las no leídas paginadas navegando por la propiedad
    Page<Inquiry> findByPropertyOwnerIdAndReadAtIsNull(Long ownerId, Pageable pageable);

    // Trae todas paginadas navegando por la propiedad
    Page<Inquiry> findByPropertyOwnerId(Long ownerId, Pageable pageable);

    List<Inquiry> findByPropertyOwnerId(Long ownerId);
}