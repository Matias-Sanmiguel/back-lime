package com.uade.lime.property.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.uade.lime.auth.model.Sex;
import com.uade.lime.auth.model.User;
import com.uade.lime.auth.model.UserRole;
import com.uade.lime.auth.repository.DenylistedTokenRepository;
import com.uade.lime.auth.repository.UserRepository;
import com.uade.lime.property.repository.InquiryRepository;
import com.uade.lime.property.repository.PropertyImageRepository;
import com.uade.lime.property.repository.PropertyRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PropertyJpaRelationshipsTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyImageRepository propertyImageRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private DenylistedTokenRepository denylistedTokenRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;

    @BeforeEach
    void setUp() {
        inquiryRepository.deleteAll();
        inquiryRepository.flush();
        propertyImageRepository.deleteAll();
        propertyImageRepository.flush();
        propertyRepository.deleteAll();
        propertyRepository.flush();
        denylistedTokenRepository.deleteAll();
        denylistedTokenRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();

        owner = userRepository.save(User.register(
                "owner-%s@example.com".formatted(UUID.randomUUID()),
                "encoded-password",
                "Owner User",
                UserRole.USER,
                null,
                LocalDate.of(1995, 1, 1),
                Sex.MALE,
                Instant.now()));
    }

    @Test
    void saveUserWithProperty_retrievesOwnerRelationship() {
        Property property = propertyRepository.save(draftProperty(owner, "Casa relacionada"));

        flushAndClear();
        Property persisted = propertyRepository.findById(property.getId()).orElseThrow();

        assertThat(persisted.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(persisted.getOwnerId()).isEqualTo(owner.getId());
        assertThat(persisted.getOwner().getEmail()).startsWith("owner-");
    }

    @Test
    void propertyRetrievesImagesFromInverseRelationship() {
        Property property = propertyRepository.save(draftProperty(owner, "Casa con imagenes"));
        propertyImageRepository.save(PropertyImage.of(property, "/uploads/properties/1/front.webp", Instant.now()));
        propertyImageRepository.save(PropertyImage.of(property, "/uploads/properties/1/kitchen.webp", Instant.now()));

        flushAndClear();
        Property persisted = propertyRepository.findById(property.getId()).orElseThrow();

        assertThat(persisted.getImages())
                .extracting(PropertyImage::getUrl)
                .containsExactlyInAnyOrder(
                        "/uploads/properties/1/front.webp",
                        "/uploads/properties/1/kitchen.webp");
    }

    @Test
    void propertyRetrievesInquiriesFromInverseRelationship() {
        Property property = propertyRepository.save(publishedProperty(owner, "Casa consultada"));
        inquiryRepository.save(Inquiry.create(
                property,
                "Ana Perez",
                "ana@example.com",
                null,
                "Sigue disponible?",
                Instant.now()));

        flushAndClear();
        Property persisted = propertyRepository.findById(property.getId()).orElseThrow();

        assertThat(persisted.getInquiries())
                .extracting(Inquiry::getEmail)
                .containsExactly("ana@example.com");
    }

    @Test
    void deletingImageDoesNotDeletePropertyOrInquiry() {
        Property property = propertyRepository.save(publishedProperty(owner, "Casa estable"));
        PropertyImage image = propertyImageRepository.save(PropertyImage.of(property, "/uploads/front.webp", Instant.now()));
        Inquiry inquiry = inquiryRepository.save(Inquiry.create(
                property,
                "Ana Perez",
                "ana@example.com",
                null,
                "Sigue disponible?",
                Instant.now()));

        propertyImageRepository.delete(image);
        propertyImageRepository.flush();

        assertThat(propertyRepository.findById(property.getId())).isPresent();
        assertThat(inquiryRepository.findById(inquiry.getId())).isPresent();
    }

    @Test
    void updatingPropertyDoesNotRemoveImagesOrInquiries() {
        Property property = propertyRepository.save(publishedProperty(owner, "Casa original"));
        PropertyImage image = propertyImageRepository.save(PropertyImage.of(property, "/uploads/front.webp", Instant.now()));
        Inquiry inquiry = inquiryRepository.save(Inquiry.create(
                property,
                "Ana Perez",
                "ana@example.com",
                null,
                "Sigue disponible?",
                Instant.now()));

        property.update(
                "Casa actualizada",
                property.getDescription(),
                property.getType(),
                property.getOperation(),
                property.getPrice(),
                property.getCurrency(),
                property.getAddress(),
                property.getCity(),
                property.getProvince(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getCoveredArea(),
                property.getTotalArea(),
                Instant.now());
        propertyRepository.saveAndFlush(property);

        assertThat(propertyImageRepository.findById(image.getId())).isPresent();
        assertThat(inquiryRepository.findById(inquiry.getId())).isPresent();
    }

    private Property publishedProperty(User owner, String title) {
        Property property = draftProperty(owner, title);
        property.publish(Instant.now());
        return property;
    }

    private Property draftProperty(User owner, String title) {
        return Property.draft(
                title,
                "Descripcion de prueba",
                PropertyType.HOUSE,
                OperationType.SALE,
                BigDecimal.valueOf(150000),
                "USD",
                "Av. Siempre Viva 742",
                "Rosario",
                "Santa Fe",
                2,
                1,
                BigDecimal.valueOf(70),
                BigDecimal.valueOf(80),
                owner,
                Instant.now());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
