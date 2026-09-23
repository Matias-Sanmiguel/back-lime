package com.uade.lime.property.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "property_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) //puede haber mas de una imagen para una propiedad
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static PropertyImage of(Property property, String url, Instant now) {
        PropertyImage image = new PropertyImage();
        image.property = property;
        image.url = url;
        image.createdAt = now;
        image.updatedAt = now;
        return image;
    }

    public void replaceUrl(String url, Instant now) {
        this.url = url;
        this.updatedAt = now;
    }
}
