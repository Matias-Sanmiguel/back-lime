package com.uade.lime.property.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 150, nullable = false)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 1000, nullable = false)
    private String message;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant readAt;

    public static Inquiry create(Property property, String name, String email, String phone, String message, Instant now) {
        Inquiry inquiry = new Inquiry();
        inquiry.property = property;
        inquiry.name = name;
        inquiry.email = email;
        inquiry.phone = phone;
        inquiry.message = message;
        inquiry.createdAt = now;
        return inquiry;
    }

    public void markRead(Instant now) {
        if (this.readAt == null) {
            this.readAt = now;
        }
    }
}