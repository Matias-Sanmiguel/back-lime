package com.uade.lime.property.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uade.lime.property.model.PropertyImage;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

    Optional<PropertyImage> findByIdAndPropertyId(Long id, Long propertyId);
    List<PropertyImage> findByPropertyIdOrderByCreatedAtAsc(Long propertyId);

    @Query("""
            select image from PropertyImage image
            where image.property.id in :propertyIds
              and not exists (
                  select earlier.id from PropertyImage earlier
                  where earlier.property.id = image.property.id
                    and (earlier.createdAt < image.createdAt
                         or (earlier.createdAt = image.createdAt and earlier.id < image.id))
              )
            """)
    List<PropertyImage> findCoverImages(@Param("propertyIds") List<Long> propertyIds);
}
