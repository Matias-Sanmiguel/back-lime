package com.uade.lime.property.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.uade.lime.property.model.Inquiry;
import com.uade.lime.property.model.OperationType;
import com.uade.lime.property.model.Property;
import com.uade.lime.property.model.PropertyType;
import com.uade.lime.property.repository.InquiryRepository;
import com.uade.lime.property.repository.PropertyImageRepository;
import com.uade.lime.property.repository.PropertyRepository;

@SpringBootTest
@ActiveProfiles("test")
class InquiryEndpointsMockMvcTest {

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
    private Property property;
    private Inquiry inquiry;

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
        property = propertyRepository.save(publishedProperty(owner, "Departamento consultado"));
        inquiry = inquiryRepository.save(Inquiry.create(
                property,
                "Ana Perez",
                "ana@example.com",
                "+54 11 5555-5555",
                "Sigue disponible?",
                Instant.now()));
    }

    @Test
    void getInquiry_asAuthorizedUser_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/me/inquiries/{id}", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(inquiry.getId().intValue())))
                .andExpect(jsonPath("$.propertyId", is(property.getId().intValue())))
                .andExpect(jsonPath("$.propertyTitle", is("Departamento consultado")))
                .andExpect(jsonPath("$.name", is("Ana Perez")))
                .andExpect(jsonPath("$.email", is("ana@example.com")))
                .andExpect(jsonPath("$.phone", is("+54 11 5555-5555")))
                .andExpect(jsonPath("$.message", is("Sigue disponible?")))
                .andExpect(jsonPath("$.readAt", nullValue()));
    }

    @Test
    void getInquiry_asDifferentUser_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/me/inquiries/{id}", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInquiry_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me/inquiries/{id}", inquiry.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getInquiry_nonexistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/me/inquiries/{id}", 99_999L)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchInquiry_asAuthorizedUser_returns200AndPersistsReadAt() throws Exception {
        mockMvc.perform(patch("/api/v1/me/inquiries/{id}", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "read": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(inquiry.getId().intValue())))
                .andExpect(jsonPath("$.readAt", notNullValue()));

        Inquiry persisted = inquiryRepository.findById(inquiry.getId()).orElseThrow();
        assertThat(persisted.getReadAt()).isNotNull();
    }

    @Test
    void patchInquiry_asDifferentUser_returns404AndDoesNotModifyInquiry() throws Exception {
        mockMvc.perform(patch("/api/v1/me/inquiries/{id}", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "read": true
                                }
                                """))
                .andExpect(status().isNotFound());

        Inquiry persisted = inquiryRepository.findById(inquiry.getId()).orElseThrow();
        assertThat(persisted.getReadAt()).isNull();
    }

    @Test
    void patchInquiry_withoutToken_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/me/inquiries/{id}", inquiry.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "read": true
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchInquiry_nonexistentId_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/me/inquiries/{id}", 99_999L)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "read": true
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchInquiry_withReadFalse_returns400AndDoesNotModifyInquiry() throws Exception {
        mockMvc.perform(patch("/api/v1/me/inquiries/{id}", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "read": false
                                }
                                """))
                .andExpect(status().isBadRequest());

        Inquiry persisted = inquiryRepository.findById(inquiry.getId()).orElseThrow();
        assertThat(persisted.getReadAt()).isNull();
    }

    @Test
    void patchInquiry_withoutReadField_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/me/inquiries/{id}", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtService.issue(user, UUID.randomUUID().toString());
    }

    private User user(String email, String name, Sex sex) {
        return User.register(
                email,
                "encoded-password",
                name,
                UserRole.USER,
                null,
                LocalDate.of(1995, 1, 1),
                sex,
                Instant.now());
    }

    private Property publishedProperty(User owner, String title) {
        Property property = Property.draft(
                title,
                "Descripcion de prueba",
                PropertyType.APARTMENT,
                OperationType.RENT,
                BigDecimal.valueOf(450000),
                "ARS",
                "Av. Siempre Viva 742",
                "Buenos Aires",
                "Buenos Aires",
                2,
                1,
                BigDecimal.valueOf(55),
                BigDecimal.valueOf(65),
                owner,
                Instant.now());
        property.publish(Instant.now());
        return property;
    }
}
