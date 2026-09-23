package com.uade.lime.property.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class PropertyAuthorizationMockMvcTest {

    private static final String PATCH_BODY = """
            {
              "title": "Departamento editado"
            }
            """;

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

        denylistedTokenRepository.deleteAll();
        inquiryRepository.deleteAll();
        inquiryRepository.flush();
        propertyImageRepository.deleteAll();
        propertyImageRepository.flush();
        propertyRepository.deleteAll();
        propertyRepository.flush();
        denylistedTokenRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();

        Instant now = Instant.now();
        LocalDate birthDate = LocalDate.of(1995, 1, 1);
        owner = userRepository.save(User.register(
                "owner@example.com",
                "encoded-password",
                "Owner User",
                UserRole.AGENCY,
                "Lime Test Agency",
                birthDate,
                Sex.MALE,
                now));
        differentUser = userRepository.save(User.register(
                "other@example.com",
                "encoded-password",
                "Other User",
                UserRole.AGENCY,
                "Otra Agencia",
                birthDate,
                Sex.FEMALE,
                now));
    }

    @Test
    void create_asUserRole_returns403() throws Exception {
        User buyer = userRepository.save(User.register(
                "buyer@example.com",
                "encoded-password",
                "Buyer User",
                UserRole.USER,
                null,
                LocalDate.of(1995, 1, 1),
                Sex.OTHER,
                Instant.now()));

        mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(buyer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "No deberia crearse"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void patch_asOwner_returns200() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(patch("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PATCH_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Departamento editado")));
    }

    @Test
    void patch_asDifferentUser_returns403() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(patch("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PATCH_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void patch_asDifferentUser_doesNotModifyProperty() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        String originalTitle = propertyRepository.findById(property.getId()).orElseThrow().getTitle();

        mockMvc.perform(patch("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PATCH_BODY))
                .andExpect(status().isForbidden());

        String titleAfterForbiddenPatch = propertyRepository.findById(property.getId()).orElseThrow().getTitle();
        org.assertj.core.api.Assertions.assertThat(titleAfterForbiddenPatch).isEqualTo(originalTitle);
    }

    @Test
    void patch_withoutToken_returns401() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(patch("/api/v1/properties/{id}", property.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PATCH_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_asOwner_returns204() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(delete("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_asDifferentUser_returns403() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(delete("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutToken_returns401() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(delete("/api/v1/properties/{id}", property.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publish_asOwner_returns200() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(post("/api/v1/properties/{id}/publish", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PUBLISHED")));
    }

    @Test
    void publish_asDifferentUser_returns403() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(post("/api/v1/properties/{id}/publish", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void publish_withoutToken_returns401() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(post("/api/v1/properties/{id}/publish", property.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pause_asOwner_returns200() throws Exception {
        Property property = propertyRepository.save(publishedProperty(owner, "Departamento original"));

        mockMvc.perform(post("/api/v1/properties/{id}/pause", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAUSED")));
    }

    @Test
    void pause_asDifferentUser_returns403() throws Exception {
        Property property = propertyRepository.save(publishedProperty(owner, "Departamento original"));

        mockMvc.perform(post("/api/v1/properties/{id}/pause", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void pause_withoutToken_returns401() throws Exception {
        Property property = propertyRepository.save(publishedProperty(owner, "Departamento original"));

        mockMvc.perform(post("/api/v1/properties/{id}/pause", property.getId()))
                .andExpect(status().isUnauthorized());
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtService.issue(user, UUID.randomUUID().toString());
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
                PropertyType.APARTMENT,
                OperationType.SALE,
                BigDecimal.valueOf(100000),
                "USD",
                "Av. Siempre Viva 742",
                "Buenos Aires",
                "Buenos Aires",
                2,
                1,
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(60),
                owner,
                Instant.now());
    }
}
