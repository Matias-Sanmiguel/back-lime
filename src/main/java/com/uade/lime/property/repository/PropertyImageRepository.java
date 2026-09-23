package com.uade.lime.property.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.lime.property.model.PropertyImage;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

    Optional<PropertyImage> findByIdAndPropertyId(Long id, Long propertyId);
}