package com.uade.lime.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.uade.lime.auth.model.User;
import com.uade.lime.auth.model.UserRole;
import com.uade.lime.auth.repository.DenylistedTokenRepository;
import com.uade.lime.auth.repository.UserRepository;
import com.uade.lime.property.repository.InquiryRepository;
import com.uade.lime.property.repository.PropertyImageRepository;
import com.uade.lime.property.repository.PropertyRepository;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerMockMvcTest {

    private static final String EMAIL = "augusto@example.com";
    private static final String PLAIN_PASSWORD = "password123";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

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
    }

    @Test
    void register_withValidRequest_returns201AndPersistsUserWithEncodedPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "augusto@example.com",
                                  "password": "password123",
                                  "name": "Augusto",
                                  "birthDate": "1995-06-15",
                                  "sex": "MALE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.email", is(EMAIL)))
                .andExpect(jsonPath("$.user.role", is("USER")));

        User persisted = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(persisted.getName()).isEqualTo("Augusto");
        assertThat(persisted.getRole()).isEqualTo(UserRole.USER);
        assertThat(persisted.getAgencyName()).isNull();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isEqualTo(persisted.getCreatedAt());
        assertThat(persisted.getPasswordHash()).isNotEqualTo(PLAIN_PASSWORD);
        assertThat(passwordEncoder.matches(PLAIN_PASSWORD, persisted.getPasswordHash())).isTrue();
    }

    @Test
    void register_withInvalidDto_returns400FromControllerAdviceAndDoesNotPersistUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "short",
                                  "name": "",
                                  "birthDate": "2999-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void register_withDuplicateEmail_returns409FromConflictoException() throws Exception {
        registerAugusto();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));

        assertThat(userRepository.count()).isOne();
    }

    @Test
    void login_withWrongPassword_returns401FromNoAutorizadoException() throws Exception {
        registerAugusto();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "augusto@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void getProperty_nonexistentId_returns404FromRecursoNoEncontradoException() throws Exception {
        mockMvc.perform(get("/api/v1/properties/{id}", 99_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    private void registerAugusto() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterBody()))
                .andExpect(status().isCreated());
    }

    private static String validRegisterBody() {
        return """
                {
                  "email": "augusto@example.com",
                  "password": "password123",
                  "name": "Augusto",
                  "birthDate": "1995-06-15",
                  "sex": "MALE"
                }
                """;
    }
}
