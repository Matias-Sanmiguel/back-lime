package com.uade.lime.property.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.uade.lime.auth.model.Sex;
import com.uade.lime.auth.model.User;
import com.uade.lime.auth.model.UserRole;
import com.uade.lime.auth.repository.DenylistedTokenRepository;
import com.uade.lime.auth.repository.UserRepository;
import com.uade.lime.auth.security.JwtService;
import com.uade.lime.property.model.OperationType;
import com.uade.lime.property.model.Property;
import com.uade.lime.property.model.PropertyType;
import com.uade.lime.property.repository.InquiryRepository;
import com.uade.lime.property.repository.PropertyImageRepository;
import com.uade.lime.property.repository.PropertyRepository;

@SpringBootTest
@ActiveProfiles("test")
class PropertyEndpointsMockMvcTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtService jwtService;

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

    private User owner;
    private User differentUser;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

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

        owner = userRepository.save(user("owner@example.com", "Owner User", Sex.MALE));
        differentUser = userRepository.save(user("other@example.com", "Other User", Sex.FEMALE));
    }

    @Test
    void getProperties_withoutToken_returns200AndOnlyPublishedProperties() throws Exception {
        Property published = propertyRepository.save(publishedProperty(owner, "Loft publicado"));
        propertyRepository.save(draftProperty(owner, "Borrador oculto"));

        mockMvc.perform(get("/api/v1/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(published.getId().intValue())))
                .andExpect(jsonPath("$.content[0].title", is("Loft publicado")))
                .andExpect(jsonPath("$.content[0].status", is("PUBLISHED")))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void getProperties_withFilters_returnsMatchingPublishedProperties() throws Exception {
        Property matching = propertyRepository.save(publishedProperty(owner, "Casa en Rosario"));
        propertyRepository.save(publishedProperty(owner, "Casa en Palermo", "Buenos Aires", 3, 2));

        mockMvc.perform(get("/api/v1/properties")
                        .param("city", "Rosario")
                        .param("province", "Santa Fe")
                        .param("type", "HOUSE")
                        .param("operation", "SALE")
                        .param("minPrice", "100000")
                        .param("maxPrice", "200000")
                        .param("minBedrooms", "2")
                        .param("minBathrooms", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(matching.getId().intValue())));
    }

    @Test
    void createProperty_withJwt_returns201AndPersistsProperty() throws Exception {
        mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "PH con patio",
                                  "description": "Unidad luminosa",
                                  "type": "HOUSE",
                                  "operation": "SALE",
                                  "price": 175000,
                                  "currency": "usd",
                                  "address": "Calle 123",
                                  "city": "La Plata",
                                  "province": "Buenos Aires",
                                  "bedrooms": 3,
                                  "bathrooms": 2,
                                  "coveredArea": 90,
                                  "totalArea": 120
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, notNullValue()))
                .andExpect(jsonPath("$.title", is("PH con patio")))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.owner.id", is(owner.getId().intValue())));

        Property persisted = propertyRepository.findAll().stream()
                .filter(property -> "PH con patio".equals(property.getTitle()))
                .findFirst()
                .orElseThrow();
        assertThat(persisted.getOwnerId()).isEqualTo(owner.getId());
        assertThat(persisted.getCurrency()).isEqualTo("USD");
    }

    @Test
    void createProperty_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Sin auth"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProperty_withNegativePrice_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Precio invalido",
                                  "price": -1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProperty_publishedProperty_withoutToken_returns200() throws Exception {
        Property property = propertyRepository.save(publishedProperty(owner, "Publicado visible"));

        mockMvc.perform(get("/api/v1/properties/{id}", property.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(property.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Publicado visible")))
                .andExpect(jsonPath("$.owner.id", is(owner.getId().intValue())));
    }

    @Test
    void getProperty_draftProperty_asOwner_returns200() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Borrador propio"));

        mockMvc.perform(get("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Borrador propio")))
                .andExpect(jsonPath("$.status", is("DRAFT")));
    }

    @Test
    void getProperty_draftProperty_withoutToken_returns404() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Borrador oculto"));

        mockMvc.perform(get("/api/v1/properties/{id}", property.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProperty_nonexistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/properties/{id}", 99_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchProperty_asOwner_returns200AndOnlyChangesSentFields() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Titulo original"));

        mockMvc.perform(patch("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Titulo editado",
                                  "price": 125000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Titulo editado")))
                .andExpect(jsonPath("$.price", is(125000)))
                .andExpect(jsonPath("$.city", is("Rosario")));

        Property persisted = propertyRepository.findById(property.getId()).orElseThrow();
        assertThat(persisted.getTitle()).isEqualTo("Titulo editado");
        assertThat(persisted.getPrice()).isEqualByComparingTo("125000");
        assertThat(persisted.getDescription()).isEqualTo("Descripcion de prueba");
        assertThat(persisted.getCity()).isEqualTo("Rosario");
    }

    @Test
    void patchProperty_asDifferentUser_returns403AndDoesNotModifyProperty() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Titulo original"));

        mockMvc.perform(patch("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Cambio no permitido"
                                }
                                """))
                .andExpect(status().isForbidden());

        Property persisted = propertyRepository.findById(property.getId()).orElseThrow();
        assertThat(persisted.getTitle()).isEqualTo("Titulo original");
    }

    @Test
    void patchProperty_withoutToken_returns401() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Titulo original"));

        mockMvc.perform(patch("/api/v1/properties/{id}", property.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Sin token"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchProperty_nonexistentId_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/properties/{id}", 99_999L)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "No existe"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchProperty_withNegativePrice_returns400() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Titulo original"));

        mockMvc.perform(patch("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "price": -20
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteProperty_asOwner_returns204AndSoftDeletesProperty() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Para borrar"));

        mockMvc.perform(delete("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
                .andExpect(status().isNoContent());

        Property persisted = propertyRepository.findById(property.getId()).orElseThrow();
        assertThat(persisted.getDeletedAt()).isNotNull();
        assertThat(propertyRepository.findByIdAndDeletedAtIsNull(property.getId())).isEmpty();
    }

    @Test
    void deleteProperty_asDifferentUser_returns403AndDoesNotDeleteProperty() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "No borrar"));

        mockMvc.perform(delete("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser)))
                .andExpect(status().isForbidden());

        Property persisted = propertyRepository.findById(property.getId()).orElseThrow();
        assertThat(persisted.getDeletedAt()).isNull();
    }

    @Test
    void deleteProperty_withoutToken_returns401() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Sin token"));

        mockMvc.perform(delete("/api/v1/properties/{id}", property.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteProperty_nonexistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/properties/{id}", 99_999L)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
                .andExpect(status().isNotFound());
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtService.issue(user, UUID.randomUUID().toString());
    }

    private User user(String email, String name, Sex sex) {
        return User.register(
                email,
                "encoded-password",
                name,
                UserRole.AGENCY,
                "Lime Test Agency",
                LocalDate.of(1995, 1, 1),
                sex,
                Instant.now());
    }

    private Property publishedProperty(User owner, String title) {
        Property property = draftProperty(owner, title);
        property.publish(Instant.now());
        return property;
    }

    private Property publishedProperty(User owner, String title, String city, int bedrooms, int bathrooms) {
        Property property = Property.draft(
                title,
                "Descripcion de prueba",
                PropertyType.HOUSE,
                OperationType.SALE,
                BigDecimal.valueOf(230000),
                "USD",
                "Av. Siempre Viva 742",
                city,
                "Buenos Aires",
                bedrooms,
                bathrooms,
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(110),
                owner,
                Instant.now());
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
}
