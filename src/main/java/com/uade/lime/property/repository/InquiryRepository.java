package com.uade.lime.property.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uade.lime.property.model.Inquiry;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @Query("SELECT i FROM Inquiry i JOIN FETCH i.property p WHERE p.owner.id = :ownerId ORDER BY i.createdAt DESC")
    List<Inquiry> findByPropertyOwnerId(@Param("ownerId") Long ownerId);

    @Query(
            value = "SELECT i FROM Inquiry i JOIN FETCH i.property p WHERE p.owner.id = :ownerId",
            countQuery = "SELECT count(i) FROM Inquiry i JOIN i.property p WHERE p.owner.id = :ownerId")
    Page<Inquiry> findByPropertyOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query(
            value = "SELECT i FROM Inquiry i JOIN FETCH i.property p WHERE p.owner.id = :ownerId AND i.readAt IS NULL",
            countQuery = "SELECT count(i) FROM Inquiry i JOIN i.property p WHERE p.owner.id = :ownerId AND i.readAt IS NULL")
    Page<Inquiry> findUnreadByPropertyOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query(
            value = "SELECT i FROM Inquiry i JOIN FETCH i.property p WHERE p.owner.id = :ownerId AND p.id = :propertyId",
            countQuery = "SELECT count(i) FROM Inquiry i JOIN i.property p WHERE p.owner.id = :ownerId AND p.id = :propertyId")
    Page<Inquiry> findByPropertyOwnerIdAndPropertyId(
            @Param("ownerId") Long ownerId, @Param("propertyId") Long propertyId, Pageable pageable);

    @Query(
            value = "SELECT i FROM Inquiry i JOIN FETCH i.property p WHERE p.owner.id = :ownerId AND p.id = :propertyId AND i.readAt IS NULL",
            countQuery = "SELECT count(i) FROM Inquiry i JOIN i.property p WHERE p.owner.id = :ownerId AND p.id = :propertyId AND i.readAt IS NULL")
    Page<Inquiry> findUnreadByPropertyOwnerIdAndPropertyId(
            @Param("ownerId") Long ownerId, @Param("propertyId") Long propertyId, Pageable pageable);

    @Query("SELECT i FROM Inquiry i JOIN FETCH i.property WHERE i.id = :id")
    Optional<Inquiry> findByIdWithProperty(@Param("id") Long id);

    @Query("SELECT count(i) FROM Inquiry i JOIN i.property p WHERE p.owner.id = :ownerId AND i.readAt IS NULL")
    long countUnreadByPropertyOwnerId(@Param("ownerId") Long ownerId);
}
