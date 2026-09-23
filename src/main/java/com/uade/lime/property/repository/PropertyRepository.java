package com.uade.lime.property.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.uade.lime.property.model.Property;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    Optional<Property> findByIdAndDeletedAtIsNull(Long id);
    List<Property> findByOwner_IdAndDeletedAtIsNull(Long ownerId);
}
