package com.uade.lime.user.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.uade.lime.auth.model.Sex;
import com.uade.lime.auth.model.User;
import com.uade.lime.auth.model.UserRole;
import com.uade.lime.auth.repository.DenylistedTokenRepository;
import com.uade.lime.auth.repository.UserRepository;
import com.uade.lime.auth.security.JwtService;
import com.uade.lime.property.repository.InquiryRepository;
import com.uade.lime.property.repository.PropertyImageRepository;
import com.uade.lime.property.repository.PropertyRepository;

@SpringBootTest
@ActiveProfiles("test")
class MeControllerMockMvcTest {

    private static final String PLAIN_PASSWORD = "password123";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DenylistedTokenRepository denylistedTokenRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private PropertyImageRepository propertyImageRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

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

        user = userRepository.save(User.register(
                "matias@example.com",
                passwordEncoder.encode(PLAIN_PASSWORD),
                "Matias Original",
                UserRole.USER,
                null,
                LocalDate.of(1998, 5, 10),
                Sex.MALE,
                Instant.now()));
    }

    @Test
    void getMe_withJwt_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.email", is("matias@example.com")))
                .andExpect(jsonPath("$.name", is("Matias Original")))
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.agencyName", nullValue()));
    }

    @Test
    void getMe_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchMe_withoutToken_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Nuevo Nombre"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchMe_partialNameOnly_returns200() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Matias Actualizado"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Matias Actualizado")))
                .andExpect(jsonPath("$.email", is("matias@example.com")))
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.agencyName", nullValue()));
    }

    @Test
    void patchMe_wrongCurrentPassword_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "not-the-password",
                                  "newPassword": "password456"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private String bearerToken(User authenticatedUser) {
        return "Bearer " + jwtService.issue(authenticatedUser, UUID.randomUUID().toString());
    }
}
